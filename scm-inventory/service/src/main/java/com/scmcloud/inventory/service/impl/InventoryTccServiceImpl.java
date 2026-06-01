package com.scmcloud.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.scmcloud.inventory.api.InventoryTccService;
import com.scmcloud.inventory.domain.entity.InvTccReservation;
import com.scmcloud.inventory.domain.entity.Inventory;
import com.scmcloud.inventory.mapper.InvInventoryMapper;
import com.scmcloud.inventory.mapper.InvTccReservationMapper;
import io.seata.core.context.RootContext;
import io.seata.rm.tcc.api.BusinessActionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 库存 TCC 服务实现
 *
 * <p>实现 Try-Confirm-Cancel 三阶段库存预�?
 *
 * <p>关键特性：
 * <ul>
 *   <li>幂等性：基于 business_key 防止重复执行</li>
 *   <li>防悬挂：Confirm/Cancel 检�?Try 记录是否存在</li>
 *   <li>允许空回滚：Cancel 时如�?Try 未执行，直接返回成功</li>
 * </ul>
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryTccServiceImpl implements InventoryTccService {
    private final InvInventoryMapper inventoryMapper;
    private final InvTccReservationMapper reservationMapper;

    /**
     * Try 阶段：预留库�?
     *
     * <p>业务逻辑�?
     * 1. 幂等性检查：如果预留记录已存在，直接返回
     * 2. 查询库存并检查是否充�?
     * 3. 将可用库存转为锁定库存（available_stock - X, locked_stock + X�?
     * 4. 插入预留记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reserveInventory(Long skuId, Integer quantity, String businessKey) {
        String xid = RootContext.getXID();
        log.info("🔵 [TCC-Try] 开始预留库�? skuId={}, quantity={}, businessKey={}, XID={}",
                skuId, quantity, businessKey, xid);

        // 1. 幂等性检�?
        InvTccReservation existingReservation = reservationMapper.selectOne(
                new LambdaQueryWrapper<InvTccReservation>()
                        .eq(InvTccReservation::getBusinessKey, businessKey)
        );

        if (existingReservation != null) {
            log.warn("⚠️  [TCC-Try] 预留记录已存在，幂等返回: businessKey={}, status={}",
                    businessKey, existingReservation.getStatus());
            return true;
        }

        // 2. 查询库存
        Inventory inventory = inventoryMapper.selectOne(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getSkuId, skuId)
                        .last("FOR UPDATE")  // 行锁
        );

        if (inventory == null) {
            log.error("�?[TCC-Try] SKU 不存�? skuId={}", skuId);
            throw new IllegalArgumentException("商品不存�?);
        }

        // 3. 检查库存是否充�?
        if (inventory.getAvailableStock() < quantity) {
            log.error("�?[TCC-Try] 库存不足: skuId={}, available={}, required={}",
                    skuId, inventory.getAvailableStock(), quantity);
            throw new RuntimeException(String.format("库存不足: 可用 %d, 需�?%d",
                    inventory.getAvailableStock(), quantity));
        }

        // 4. 预留库存（可用库�?-> 锁定库存�?
        int updated = inventoryMapper.update(null,
                new LambdaUpdateWrapper<Inventory>()
                        .setSql("available_stock = available_stock - " + quantity)
                        .setSql("locked_stock = locked_stock + " + quantity)
                        .eq(Inventory::getId, inventory.getId())
                        .ge(Inventory::getAvailableStock, quantity)  // 乐观�?
        );

        if (updated == 0) {
            log.error("�?[TCC-Try] 库存预留失败（并发冲突）: skuId={}", skuId);
            throw new RuntimeException("库存预留失败，请重试");
        }

        // 5. 插入预留记录
        InvTccReservation reservation = new InvTccReservation();
        reservation.setBusinessKey(businessKey);
        reservation.setSkuId(skuId);
        reservation.setQuantity(quantity);
        reservation.setXid(xid);
        reservation.setBranchId(0L);  // 分支 ID �?Seata 管理
        reservation.setStatus(InvTccReservation.Status.TRYING);
        reservation.setTryTime(LocalDateTime.now());

        reservationMapper.insert(reservation);

        log.info("�?[TCC-Try] 库存预留成功: skuId={}, quantity={}, businessKey={}, XID={}",
                skuId, quantity, businessKey, xid);
        return true;
    }

    /**
     * Confirm 阶段：确认预�?
     *
     * <p>业务逻辑�?
     * 1. 幂等性检查：如果已经 CONFIRMED，直接返�?
     * 2. 防悬挂检查：如果 Try 记录不存在，拒绝执行
     * 3. 扣减锁定库存（locked_stock - X�?
     * 4. 更新预留记录状态为 CONFIRMED
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmReserve(BusinessActionContext context) {
        String businessKey = context.getActionContext("businessKey").toString();
        Long skuId = Long.valueOf(context.getActionContext("skuId").toString());
        Integer quantity = Integer.valueOf(context.getActionContext("quantity").toString());
        String xid = context.getXid();

        log.info("🟢 [TCC-Confirm] 开始确认预�? skuId={}, quantity={}, businessKey={}, XID={}",
                skuId, quantity, businessKey, xid);

        // 1. 查询预留记录
        InvTccReservation reservation = reservationMapper.selectOne(
                new LambdaQueryWrapper<InvTccReservation>()
                        .eq(InvTccReservation::getBusinessKey, businessKey)
        );

        // 2. 防悬挂：Try 记录不存�?
        if (reservation == null) {
            log.warn("⚠️  [TCC-Confirm] Try 记录不存在，拒绝执行（防悬挂�? businessKey={}", businessKey);
            return false;
        }

        // 3. 幂等性：已经 CONFIRMED
        if (InvTccReservation.Status.CONFIRMED.equals(reservation.getStatus())) {
            log.warn("⚠️  [TCC-Confirm] 已经确认过，幂等返回: businessKey={}", businessKey);
            return true;
        }

        // 4. 扣减锁定库存
        int updated = inventoryMapper.update(null,
                new LambdaUpdateWrapper<Inventory>()
                        .setSql("locked_stock = locked_stock - " + quantity)
                        .eq(Inventory::getSkuId, skuId)
                        .ge(Inventory::getLockedStock, quantity)
        );

        if (updated == 0) {
            log.error("�?[TCC-Confirm] 锁定库存不足: skuId={}, quantity={}", skuId, quantity);
            throw new RuntimeException("锁定库存不足");
        }

        // 5. 更新预留记录状�?
        reservation.setStatus(InvTccReservation.Status.CONFIRMED);
        reservation.setConfirmTime(LocalDateTime.now());
        reservationMapper.updateById(reservation);

        log.info("�?[TCC-Confirm] 预留确认成功: skuId={}, quantity={}, businessKey={}, XID={}",
                skuId, quantity, businessKey, xid);
        return true;
    }

    /**
     * Cancel 阶段：取消预�?
     *
     * <p>业务逻辑�?
     * 1. 幂等性检查：如果已经 CANCELLED，直接返�?
     * 2. 空回滚：如果 Try 记录不存在（Try 未执行），直接返回成�?
     * 3. 释放锁定库存为可用库存（available_stock + X, locked_stock - X�?
     * 4. 更新预留记录状态为 CANCELLED
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelReserve(BusinessActionContext context) {
        String businessKey = context.getActionContext("businessKey").toString();
        Long skuId = Long.valueOf(context.getActionContext("skuId").toString());
        Integer quantity = Integer.valueOf(context.getActionContext("quantity").toString());
        String xid = context.getXid();

        log.info("🔴 [TCC-Cancel] 开始取消预�? skuId={}, quantity={}, businessKey={}, XID={}",
                skuId, quantity, businessKey, xid);

        // 1. 查询预留记录
        InvTccReservation reservation = reservationMapper.selectOne(
                new LambdaQueryWrapper<InvTccReservation>()
                        .eq(InvTccReservation::getBusinessKey, businessKey)
        );

        // 2. 空回滚：Try 记录不存在（Try 未执行或网络延迟�?
        if (reservation == null) {
            log.warn("⚠️  [TCC-Cancel] Try 记录不存在，空回�? businessKey={}", businessKey);
            // 插入一�?CANCELLED 记录，防止后�?Try 悬挂
            InvTccReservation cancelRecord = new InvTccReservation();
            cancelRecord.setBusinessKey(businessKey);
            cancelRecord.setSkuId(skuId);
            cancelRecord.setQuantity(quantity);
            cancelRecord.setXid(xid);
            cancelRecord.setBranchId(0L);
            cancelRecord.setStatus(InvTccReservation.Status.CANCELLED);
            cancelRecord.setCancelTime(LocalDateTime.now());
            reservationMapper.insert(cancelRecord);
            return true;
        }

        // 3. 幂等性：已经 CANCELLED
        if (InvTccReservation.Status.CANCELLED.equals(reservation.getStatus())) {
            log.warn("⚠️  [TCC-Cancel] 已经取消过，幂等返回: businessKey={}", businessKey);
            return true;
        }

        // 4. 释放锁定库存
        int updated = inventoryMapper.update(null,
                new LambdaUpdateWrapper<Inventory>()
                        .setSql("available_stock = available_stock + " + quantity)
                        .setSql("locked_stock = locked_stock - " + quantity)
                        .eq(Inventory::getSkuId, skuId)
                        .ge(Inventory::getLockedStock, quantity)
        );

        if (updated == 0) {
            log.warn("⚠️  [TCC-Cancel] 锁定库存不足，可能已被释�? skuId={}, quantity={}",
                    skuId, quantity);
            // 不抛异常，标记为已取�?
        }

        // 5. 更新预留记录状�?
        reservation.setStatus(InvTccReservation.Status.CANCELLED);
        reservation.setCancelTime(LocalDateTime.now());
        reservationMapper.updateById(reservation);

        log.info("�?[TCC-Cancel] 预留取消成功: skuId={}, quantity={}, businessKey={}, XID={}",
                skuId, quantity, businessKey, xid);
        return true;
    }
}
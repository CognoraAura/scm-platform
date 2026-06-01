package com.scmcloud.warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.scmcloud.warehouse.domain.entity.WmsInbound;
import com.scmcloud.warehouse.domain.entity.WmsInboundItem;
import com.scmcloud.warehouse.mapper.WmsInboundMapper;
import com.scmcloud.warehouse.service.IWmsInboundItemService;
import com.scmcloud.warehouse.service.IWmsInboundService;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class WmsInboundServiceImpl extends ServiceImpl<WmsInboundMapper, WmsInbound>
        implements IWmsInboundService {

    private final IWmsInboundItemService inboundItemService;

    @Override
    public Page<WmsInbound> pageList(int page, int size, String warehouseId, Integer inboundType, Integer status) {
        LambdaQueryWrapper<WmsInbound> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(WmsInbound::getDeleted, false);

        if (StringUtils.hasText(warehouseId)) {
            wrapper.eq(WmsInbound::getWarehouseId, warehouseId);
        }
        if (inboundType != null) {
            wrapper.eq(WmsInbound::getInboundType, inboundType);
        }
        if (status != null) {
            wrapper.eq(WmsInbound::getStatus, status);
        }
        wrapper.orderByDesc(WmsInbound::getCreateTime);

        return page(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean receive(String inboundId, String operatorId, String operatorName) {
        WmsInbound inbound = getById(inboundId);
        if (inbound == null) {
            log.warn("入库单不存在: id={}", inboundId);
            return false;
        }
        if (inbound.getStatus() != 0 && inbound.getStatus() != 1) {
            throw new IllegalStateException("入库单状态不允许收货，当前状�? " + inbound.getStatus());
        }

        List<WmsInboundItem> items = inboundItemService.lambdaQuery()
                .eq(WmsInboundItem::getInboundId, inboundId)
                .eq(WmsInboundItem::getDeleted, false)
                .list();

        int totalReceived = items.stream()
                .mapToInt(item -> item.getActualQuantity() != null ? item.getActualQuantity() : 0)
                .sum();

        if (totalReceived == 0) {
            throw new IllegalStateException("入库明细实际收货数量不能全部�?");
        }

        boolean allReceived = items.stream()
                .allMatch(item -> item.getActualQuantity() != null
                        && item.getActualQuantity().equals(item.getPlanQuantity()));

        inbound.setReceivedQuantity(totalReceived);
        inbound.setStatus(allReceived ? 3 : 2); // 3-已完�? 2-部分入库
        inbound.setOperatorId(operatorId);
        inbound.setOperatorName(operatorName);
        inbound.setUpdateTime(LocalDateTime.now());
        inbound.setUpdateBy(operatorId);

        if (allReceived) {
            inbound.setCompletedAt(LocalDateTime.now());
        }

        boolean success = updateById(inbound);
        if (success) {
            log.info("入库单收货完�? id={}, inboundNo={}, status={}, receivedQty={}",
                    inboundId, inbound.getInboundNo(), inbound.getStatus(), totalReceived);
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(String inboundId, String operatorId, String operatorName) {
        WmsInbound inbound = getById(inboundId);
        if (inbound == null) {
            log.warn("入库单不存在: id={}", inboundId);
            return false;
        }
        if (inbound.getStatus() == 3) {
            throw new IllegalStateException("已完成的入库单不能取�?);
        }

        inbound.setStatus(4); // 4-已取�?
        inbound.setOperatorId(operatorId);
        inbound.setOperatorName(operatorName);
        inbound.setUpdateTime(LocalDateTime.now());
        inbound.setUpdateBy(operatorId);

        boolean success = updateById(inbound);
        if (success) {
            log.info("入库单已取消: id={}, inboundNo={}", inboundId, inbound.getInboundNo());
        }
        return success;
    }
}

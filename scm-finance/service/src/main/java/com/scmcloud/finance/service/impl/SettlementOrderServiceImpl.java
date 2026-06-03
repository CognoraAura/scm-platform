package com.scmcloud.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.status.StatusValidator;
import com.scmcloud.common.util.UUIDv7Util;
import com.scmcloud.finance.domain.entity.SettlementOrder;
import com.scmcloud.finance.mapper.SettlementOrderMapper;
import com.scmcloud.finance.service.ISettlementOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
public class SettlementOrderServiceImpl extends ServiceImpl<SettlementOrderMapper, SettlementOrder>
        implements ISettlementOrderService {

    @Autowired
    private StatusValidator statusValidator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettlementOrder createSettlement(SettlementOrder order) {
        log.info("创建结算� partnerName={}, settlementType={}", order.getPartnerName(), order.getSettlementType());

        order.setId(UUIDv7Util.generateString());
        order.setSettlementNo(generateSettlementNo());
        order.setStatus(0);
        order.setPaidAmount(BigDecimal.ZERO);
        order.setUnpaidAmount(order.getActualAmount());
        order.setDeleted(false);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        boolean success = save(order);
        if (!success) {
            throw new RuntimeException("创建结算单失败");
        }

        log.info("结算单创建成功 id={}, settlementNo={}", order.getId(), order.getSettlementNo());
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettlementOrder confirmSettlement(String id, String approverId, String approverName) {
        log.info("确认结算� id={}, approver={}", id, approverName);

        SettlementOrder order = getById(id);
        if (order == null || Boolean.TRUE.equals(order.getDeleted())) {
            throw new IllegalArgumentException("结算单不存在: " + id);
        }
        statusValidator.validateTransition("SETTLEMENT", "DRAFT", "CONFIRMED");

        order.setStatus(1);
        order.setApproverId(approverId);
        order.setApproverName(approverName);
        order.setApprovedAt(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        updateById(order);
        log.info("结算单确认成� id={}", id);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettlementOrder recordPayment(String id, BigDecimal amount) {
        log.info("记录结算单付� id={}, amount={}", id, amount);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("付款金额必须大于0");
        }

        SettlementOrder order = getById(id);
        if (order == null || Boolean.TRUE.equals(order.getDeleted())) {
            throw new IllegalArgumentException("结算单不存在: " + id);
        }
        BigDecimal newPaidAmount = order.getPaidAmount().add(amount);
        if (newPaidAmount.compareTo(order.getActualAmount()) > 0) {
            throw new IllegalArgumentException(
                    String.format("付款金额超出应付金额: 已付=%s, 本次=%s, 应付=%s",
                            order.getPaidAmount(), amount, order.getActualAmount()));
        }

        order.setPaidAmount(newPaidAmount);
        order.setUnpaidAmount(order.getActualAmount().subtract(newPaidAmount));
        order.setUpdateTime(LocalDateTime.now());

        String targetStatus = order.getUnpaidAmount().compareTo(BigDecimal.ZERO) == 0 ? "FULLY_PAID" : "PARTIAL_PAID";
        statusValidator.validateTransition("SETTLEMENT", resolveSettlementStatusName(order.getStatus()), targetStatus);

        if (order.getUnpaidAmount().compareTo(BigDecimal.ZERO) == 0) {
            order.setStatus(4);
            log.info("结算单已全额付款: id={}", id);
        } else {
            order.setStatus(3);
            log.info("结算单部分付� id={}, paid={}, unpaid={}", id, newPaidAmount, order.getUnpaidAmount());
        }

        updateById(order);
        return order;
    }

    @Override
    public Page<SettlementOrder> listByStatus(Integer status, int page, int size) {
        log.debug("按状态查询结算单: status={}, page={}, size={}", status, page, size);
        LambdaQueryWrapper<SettlementOrder> wrapper = Wrappers.lambdaQuery();
        if (status != null) {
            wrapper.eq(SettlementOrder::getStatus, status);
        }
        wrapper.eq(SettlementOrder::getDeleted, false);
        wrapper.orderByDesc(SettlementOrder::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    private String generateSettlementNo() {
        return "STL" + System.currentTimeMillis();
    }

    private String resolveSettlementStatusName(int status) {
        return switch (status) {
            case 0 -> "DRAFT";
            case 1 -> "CONFIRMED";
            case 2 -> "PARTIAL_PAID";
            case 3 -> "FULLY_PAID";
            case 4 -> "CLOSED";
            default -> throw new IllegalStateException("未知的结算单状态: " + status);
        };
    }
}

package com.scmcloud.logistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scmcloud.common.status.StatusValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.scmcloud.logistics.domain.entity.TmsWaybill;
import com.scmcloud.logistics.mapper.TmsWaybillMapper;
import com.scmcloud.logistics.service.ITmsWaybillService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmsWaybillServiceImpl extends ServiceImpl<TmsWaybillMapper, TmsWaybill> implements ITmsWaybillService {

    private final StatusValidator statusValidator;

    @Override
    public Page<TmsWaybill> pageList(int page, int size, String waybillNo, Integer status, String carrierId) {
        log.debug("查询运单列表: page={}, size={}, waybillNo={}, status={}, carrierId={}", page, size, waybillNo, status, carrierId);

        LambdaQueryWrapper<TmsWaybill> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(waybillNo)) {
            wrapper.like(TmsWaybill::getWaybillNo, waybillNo);
        }
        if (status != null) {
            wrapper.eq(TmsWaybill::getStatus, status);
        }
        if (StringUtils.hasText(carrierId)) {
            wrapper.eq(TmsWaybill::getCarrierId, carrierId);
        }
        wrapper.eq(TmsWaybill::getDeleted, false);
        wrapper.orderByDesc(TmsWaybill::getCreateTime);

        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public TmsWaybill getByWaybillNo(String waybillNo) {
        log.debug("根据运单号查� waybillNo={}", waybillNo);
        return lambdaQuery()
                .eq(TmsWaybill::getWaybillNo, waybillNo)
                .eq(TmsWaybill::getDeleted, false)
                .one();
    }

    @Override
    public List<TmsWaybill> listByOrderId(String orderId) {
        log.debug("根据订单ID查询运单: orderId={}", orderId);
        return lambdaQuery()
                .eq(TmsWaybill::getOrderId, orderId)
                .eq(TmsWaybill::getDeleted, false)
                .orderByDesc(TmsWaybill::getCreateTime)
                .list();
    }

    @Override
    public List<TmsWaybill> listByOrderNo(String orderNo) {
        log.debug("根据订单号查询运� orderNo={}", orderNo);
        return lambdaQuery()
                .eq(TmsWaybill::getOrderNo, orderNo)
                .eq(TmsWaybill::getDeleted, false)
                .orderByDesc(TmsWaybill::getCreateTime)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TmsWaybill createWaybill(TmsWaybill waybill) {
        log.info("创建运单: orderNo={}, carrierId={}", waybill.getOrderNo(), waybill.getCarrierId());

        if (waybill.getId() == null) {
            waybill.setId(UUID.randomUUID().toString());
        }
        if (waybill.getWaybillNo() == null) {
            waybill.setWaybillNo(generateWaybillNo());
        }
        waybill.setStatus(0);
        waybill.setDeleted(false);
        waybill.setCreateTime(LocalDateTime.now());
        waybill.setUpdateTime(LocalDateTime.now());

        boolean success = save(waybill);
        if (!success) {
            throw new RuntimeException("创建运单失败");
        }

        log.info("运单创建成功: id={}, waybillNo={}", waybill.getId(), waybill.getWaybillNo());
        return waybill;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(String waybillId, Integer status, String operator) {
        log.info("更新运单状� waybillId={}, status={}, operator={}", waybillId, status, operator);

        TmsWaybill waybill = getById(waybillId);
        if (waybill == null) {
            log.warn("运单不存� waybillId={}", waybillId);
            return false;
        }

        waybill.setStatus(status);
        waybill.setUpdateBy(operator);
        waybill.setUpdateTime(LocalDateTime.now());

        if (status == 4) {
            waybill.setActualDelivery(LocalDateTime.now());
        }

        boolean success = updateById(waybill);
        if (success) {
            log.info("运单状态更新成� waybillNo={}, status={}", waybill.getWaybillNo(), status);
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelWaybill(String waybillId, String reason, String operator) {
        log.info("取消运单: waybillId={}, reason={}, operator={}", waybillId, reason, operator);

        TmsWaybill waybill = getById(waybillId);
        if (waybill == null) {
            log.warn("运单不存\u200b waybillId={}", waybillId);
            return false;
        }

        String implCancelFromStatus;
        if (waybill.getStatus() == 0) {
            implCancelFromStatus = "CREATED";
        } else if (waybill.getStatus() == 1) {
            implCancelFromStatus = "PENDING";
        } else if (waybill.getStatus() == 2) {
            implCancelFromStatus = "IN_TRANSIT";
        } else {
            implCancelFromStatus = "DELIVERED";
        }
        statusValidator.validateTransition("LOGISTICS", implCancelFromStatus, "CANCELLED");

        waybill.setStatus(6);
        waybill.setExceptionType("CANCEL");
        waybill.setExceptionReason(reason);
        waybill.setUpdateBy(operator);
        waybill.setUpdateTime(LocalDateTime.now());

        boolean success = updateById(waybill);
        if (success) {
            log.info("运单取消成功: waybillNo={}", waybill.getWaybillNo());
        }
        return success;
    }

    private String generateWaybillNo() {
        return "WB" + System.currentTimeMillis() + String.format("%04d", (int) (Math.random() * 10000));
    }
}

package com.scmcloud.logistics.service.command;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.common.status.StatusValidator;
import com.scmcloud.logistics.domain.entity.TmsWaybill;
import com.scmcloud.logistics.mapper.TmsWaybillMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmsWaybillCommandService {

    private final TmsWaybillMapper tmsWaybillMapper;
    private final StatusValidator statusValidator;

    @Master(reason = "保存运单")
    @Transactional(rollbackFor = Exception.class)
    public boolean save(TmsWaybill entity) {
        return tmsWaybillMapper.insert(entity) > 0;
    }

    @Master(reason = "更新运单")
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(TmsWaybill entity) {
        return tmsWaybillMapper.updateById(entity) > 0;
    }

    @Master(reason = "删除运单")
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(String id) {
        return tmsWaybillMapper.deleteById(id) > 0;
    }

    @Master(reason = "创建运单")
    @Transactional(rollbackFor = Exception.class)
    public TmsWaybill createWaybill(TmsWaybill waybill) {
        log.info("创建运单: orderNo={}, carrierId={}", waybill.getOrderNo(), waybill.getCarrierId());
        if (waybill.getId() == null) {
            waybill.setId(java.util.UUID.randomUUID().toString());
        }
        if (waybill.getWaybillNo() == null) {
            waybill.setWaybillNo(generateWaybillNo());
        }
        waybill.setStatus(0);
        waybill.setDeleted(false);
        waybill.setCreateTime(LocalDateTime.now());
        waybill.setUpdateTime(LocalDateTime.now());
        boolean success = tmsWaybillMapper.insert(waybill) > 0;
        if (!success) {
            throw new RuntimeException("创建运单失败");
        }
        log.info("运单创建成功: id={}, waybillNo={}", waybill.getId(), waybill.getWaybillNo());
        return waybill;
    }

    @Master(reason = "更新运单状态")
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(String waybillId, Integer status, String operator) {
        log.info("更新运单状态: waybillId={}, status={}, operator={}", waybillId, status, operator);
        TmsWaybill waybill = tmsWaybillMapper.selectById(waybillId);
        if (waybill == null) {
            log.warn("运单不存在: waybillId={}", waybillId);
            return false;
        }
        waybill.setStatus(status);
        waybill.setUpdateBy(operator);
        waybill.setUpdateTime(LocalDateTime.now());
        if (status == 4) {
            waybill.setActualDelivery(LocalDateTime.now());
        }
        boolean success = tmsWaybillMapper.updateById(waybill) > 0;
        if (success) {
            log.info("运单状态更新成功: waybillNo={}, status={}", waybill.getWaybillNo(), status);
        }
        return success;
    }

    @Master(reason = "取消运单")
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelWaybill(String waybillId, String reason, String operator) {
        log.info("取消运单: waybillId={}, reason={}, operator={}", waybillId, reason, operator);
        TmsWaybill waybill = tmsWaybillMapper.selectById(waybillId);
        if (waybill == null) {
            log.warn("运单不存在: waybillId={}", waybillId);
            return false;
        }
        String cancelFromStatus;
        if (waybill.getStatus() == 0) {
            cancelFromStatus = "CREATED";
        } else if (waybill.getStatus() == 1) {
            cancelFromStatus = "PENDING";
        } else if (waybill.getStatus() == 2) {
            cancelFromStatus = "IN_TRANSIT";
        } else {
            cancelFromStatus = "DELIVERED";
        }
        statusValidator.validateTransition("LOGISTICS", cancelFromStatus, "CANCELLED");
        waybill.setStatus(6);
        waybill.setExceptionType("CANCEL");
        waybill.setExceptionReason(reason);
        waybill.setUpdateBy(operator);
        waybill.setUpdateTime(LocalDateTime.now());
        boolean success = tmsWaybillMapper.updateById(waybill) > 0;
        if (success) {
            log.info("运单取消成功: waybillNo={}", waybill.getWaybillNo());
        }
        return success;
    }

    private String generateWaybillNo() {
        return "WB" + System.currentTimeMillis() + String.format("%04d", (int) (Math.random() * 10000));
    }
}

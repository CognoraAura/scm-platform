package com.scmcloud.purchase.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.common.status.StatusValidator;
import com.scmcloud.purchase.domain.entity.PurRequest;
import com.scmcloud.purchase.mapper.PurRequestMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurRequestCommandService {

    private final PurRequestMapper purRequestMapper;
    private final StatusValidator statusValidator;

    @Master(reason = "保存采购申请")
    @Transactional(rollbackFor = Exception.class)
    public boolean save(PurRequest entity) {
        return purRequestMapper.insert(entity) > 0;
    }

    @Master(reason = "更新采购申请")
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(PurRequest entity) {
        return purRequestMapper.updateById(entity) > 0;
    }

    @Master(reason = "删除采购申请")
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(String id) {
        return purRequestMapper.deleteById(id) > 0;
    }

    @Master(reason = "提交采购申请")
    @Transactional(rollbackFor = Exception.class)
    public boolean submit(String id) {
        PurRequest request = purRequestMapper.selectById(id);
        if (request == null || request.getDeleted()) {
            throw new IllegalArgumentException("采购申请不存在: " + id);
        }
        statusValidator.validateTransition("PURCHASE_REQUEST", "DRAFT", "PENDING_APPROVAL");
        request.setStatus(1);
        request.setSubmittedAt(LocalDateTime.now());
        request.setUpdateTime(LocalDateTime.now());
        return purRequestMapper.updateById(request) > 0;
    }

    @Master(reason = "审批采购申请")
    @Transactional(rollbackFor = Exception.class)
    public boolean approve(String id, String approverId, String approverName) {
        PurRequest request = purRequestMapper.selectById(id);
        if (request == null || request.getDeleted()) {
            throw new IllegalArgumentException("采购申请不存在: " + id);
        }
        statusValidator.validateTransition("PURCHASE_REQUEST", "PENDING_APPROVAL", "APPROVED");
        request.setStatus(2);
        request.setCurrentApproverId(approverId);
        request.setCurrentApproverName(approverName);
        request.setApprovedAt(LocalDateTime.now());
        request.setUpdateTime(LocalDateTime.now());
        return purRequestMapper.updateById(request) > 0;
    }

    @Master(reason = "驳回采购申请")
    @Transactional(rollbackFor = Exception.class)
    public boolean reject(String id, String approverId, String approverName, String reason) {
        PurRequest request = purRequestMapper.selectById(id);
        if (request == null || request.getDeleted()) {
            throw new IllegalArgumentException("采购申请不存在: " + id);
        }
        statusValidator.validateTransition("PURCHASE_REQUEST", "PENDING_APPROVAL", "REJECTED");
        request.setStatus(3);
        request.setCurrentApproverId(approverId);
        request.setCurrentApproverName(approverName);
        request.setRejectedAt(LocalDateTime.now());
        request.setRejectReason(reason);
        request.setUpdateTime(LocalDateTime.now());
        return purRequestMapper.updateById(request) > 0;
    }

    @Master(reason = "关闭采购申请")
    @Transactional(rollbackFor = Exception.class)
    public boolean close(String id) {
        PurRequest request = purRequestMapper.selectById(id);
        if (request == null || request.getDeleted()) {
            throw new IllegalArgumentException("采购申请不存在: " + id);
        }
        String currentStatus = statusCodeToName(request.getStatus());
        statusValidator.validateTransition("PURCHASE_REQUEST", currentStatus, "CLOSED");
        request.setStatus(5);
        request.setUpdateTime(LocalDateTime.now());
        return purRequestMapper.updateById(request) > 0;
    }

    @Master(reason = "采购申请转采购单")
    @Transactional(rollbackFor = Exception.class)
    public boolean convertToOrder(String id, String orderId, String orderNo) {
        PurRequest request = purRequestMapper.selectById(id);
        if (request == null || request.getDeleted()) {
            throw new IllegalArgumentException("采购申请不存在: " + id);
        }
        if (Boolean.TRUE.equals(request.getConverted())) {
            throw new IllegalStateException("该申请已转采购单");
        }
        statusValidator.validateTransition("PURCHASE_REQUEST", "APPROVED", "CONVERTED");
        request.setStatus(4);
        request.setConverted(true);
        request.setPurchaseOrderId(orderId);
        request.setPurchaseOrderNo(orderNo);
        request.setUpdateTime(LocalDateTime.now());
        return purRequestMapper.updateById(request) > 0;
    }

    private String statusCodeToName(int code) {
        return switch (code) {
            case 0 -> "DRAFT";
            case 1 -> "PENDING_APPROVAL";
            case 2 -> "APPROVED";
            case 3 -> "REJECTED";
            case 4 -> "CONVERTED";
            case 5 -> "CLOSED";
            default -> "UNKNOWN";
        };
    }
}

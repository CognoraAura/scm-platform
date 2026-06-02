package com.scmcloud.purchase.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
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
        if (request.getStatus() != 0) {
            throw new IllegalStateException("只有草稿状态的申请才能提交");
        }
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
        if (request.getStatus() != 1) {
            throw new IllegalStateException("只有待审批状态的申请才能审批");
        }
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
        if (request.getStatus() != 1) {
            throw new IllegalStateException("只有待审批状态的申请才能驳回");
        }
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
        if (request.getStatus() == 4) {
            throw new IllegalStateException("已转采购单的申请不能关闭");
        }
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
        if (request.getStatus() != 2) {
            throw new IllegalStateException("只有已审批的申请才能转采购单");
        }
        if (Boolean.TRUE.equals(request.getConverted())) {
            throw new IllegalStateException("该申请已转采购单");
        }
        request.setStatus(4);
        request.setConverted(true);
        request.setPurchaseOrderId(orderId);
        request.setPurchaseOrderNo(orderNo);
        request.setUpdateTime(LocalDateTime.now());
        return purRequestMapper.updateById(request) > 0;
    }
}

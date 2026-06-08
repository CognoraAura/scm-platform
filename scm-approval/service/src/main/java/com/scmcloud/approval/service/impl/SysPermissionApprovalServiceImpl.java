package com.scmcloud.approval.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scmcloud.common.util.UUIDv7Util;
import com.scmcloud.approval.domain.entity.SysPermissionApproval;
import com.scmcloud.approval.mapper.SysPermissionApprovalMapper;
import com.scmcloud.approval.service.ISysPermissionApprovalService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class SysPermissionApprovalServiceImpl extends ServiceImpl<SysPermissionApprovalMapper, SysPermissionApproval>
        implements ISysPermissionApprovalService {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_APPROVING = 1;
    private static final int STATUS_APPROVED = 2;
    private static final int STATUS_REJECTED = 3;
    private static final int STATUS_WITHDRAWN = 4;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysPermissionApproval submitApproval(SysPermissionApproval approval) {
        log.info("鎻愪氦瀹℃壒鐢宠: applicantId={}, type={}", approval.getApplicantId(), approval.getApprovalType());

        approval.setId(UUIDv7Util.generateString());
        approval.setApprovalStatus(STATUS_PENDING);
        approval.setCreateTime(LocalDateTime.now());
        approval.setUpdateTime(LocalDateTime.now());

        boolean success = save(approval);
        if (!success) {
            throw new RuntimeException("鎻愪氦瀹℃壒鐢宠澶辫触");
        }

        log.info("瀹℃壒鐢宠鎻愪氦鎴愬姛: id={}", approval.getId());
        return approval;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysPermissionApproval approve(String approvalId, String approverId, String approverName) {
        log.info("瀹℃壒閫氳繃: approvalId={}, approverId={}", approvalId, approverId);

        SysPermissionApproval approval = getById(approvalId);
        if (approval == null) {
            throw new IllegalArgumentException("瀹℃壒璁板綍涓嶅瓨锟?" + approvalId);
        }

        if (approval.getApprovalStatus() != STATUS_PENDING
                && approval.getApprovalStatus() != STATUS_APPROVING) {
            throw new IllegalStateException("褰撳墠鐘舵€佷笉鍏佽瀹℃壒: status=" + approval.getApprovalStatus());
        }

        approval.setApprovalStatus(STATUS_APPROVED);
        approval.setApprovedBy(approverId);
        approval.setApproverName(approverName);
        approval.setApprovedTime(LocalDateTime.now());
        approval.setUpdateTime(LocalDateTime.now());

        boolean success = updateById(approval);
        if (!success) {
            throw new RuntimeException("瀹℃壒鎿嶄綔澶辫触");
        }

        log.info("瀹℃壒閫氳繃鎴愬姛: id={}", approvalId);
        return approval;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysPermissionApproval reject(String approvalId, String approverId, String approverName, String rejectReason) {
        log.info("瀹℃壒鎷掔粷: approvalId={}, approverId={}", approvalId, approverId);

        SysPermissionApproval approval = getById(approvalId);
        if (approval == null) {
            throw new IllegalArgumentException("瀹℃壒璁板綍涓嶅瓨锟?" + approvalId);
        }

        if (approval.getApprovalStatus() != STATUS_PENDING
                && approval.getApprovalStatus() != STATUS_APPROVING) {
            throw new IllegalStateException("褰撳墠鐘舵€佷笉鍏佽瀹℃壒: status=" + approval.getApprovalStatus());
        }

        approval.setApprovalStatus(STATUS_REJECTED);
        approval.setApprovedBy(approverId);
        approval.setApproverName(approverName);
        approval.setApprovedTime(LocalDateTime.now());
        approval.setRejectReason(rejectReason);
        approval.setUpdateTime(LocalDateTime.now());

        boolean success = updateById(approval);
        if (!success) {
            throw new RuntimeException("瀹℃壒鎷掔粷鎿嶄綔澶辫触");
        }

        log.info("瀹℃壒鎷掔粷鎴愬姛: id={}", approvalId);
        return approval;
    }

    @Override
    public List<SysPermissionApproval> listByApplicant(String applicantId) {
        log.debug("鏌ヨ鐢宠浜哄鎵瑰垪锟?applicantId={}", applicantId);

        LambdaQueryWrapper<SysPermissionApproval> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysPermissionApproval::getApplicantId, applicantId)
               .orderByDesc(SysPermissionApproval::getCreateTime);

        return list(wrapper);
    }

    @Override
    public List<SysPermissionApproval> listPending(String approverId) {
        log.debug("鏌ヨ寰呭鎵瑰垪锟?approverId={}", approverId);

        LambdaQueryWrapper<SysPermissionApproval> wrapper = Wrappers.lambdaQuery();
        wrapper.and(w -> w.eq(SysPermissionApproval::getApprovalStatus, STATUS_PENDING)
                          .or()
                          .eq(SysPermissionApproval::getApprovalStatus, STATUS_APPROVING));
        wrapper.orderByDesc(SysPermissionApproval::getCreateTime);

        return list(wrapper);
    }
}

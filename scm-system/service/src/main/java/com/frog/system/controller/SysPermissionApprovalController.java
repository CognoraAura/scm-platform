package com.frog.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frog.common.domain.PageResult;
import com.frog.common.log.annotation.AuditLog;
import com.frog.common.response.ApiResponse;
import com.frog.common.dto.approval.ApprovalDTO;
import com.frog.common.dto.approval.ApprovalProcessDTO;
import com.frog.system.service.ISysPermissionApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 权限申请审批控制�?
 *
 * @author Deng
 * @since 2025-11-03
 */
@RestController
@RequestMapping("/api/system/approvals")
@RequiredArgsConstructor
public class SysPermissionApprovalController {
    private final ISysPermissionApprovalService approvalService;

    /**
     * 提交权限申请
     */
    @PostMapping("/submit")
    @AuditLog(
            operation = "提交权限申请",
            businessType = "APPROVAL",
            riskLevel = 3
    )
    public ApiResponse<UUID> submitApproval(@Validated @RequestBody ApprovalDTO approvalDTO) {
        UUID approvalId = approvalService.submitApproval(approvalDTO);

        return ApiResponse.success("申请已提交，等待审批", approvalId);
    }

    /**
     * 审批处理
     */
    @PostMapping("/{id}/process")
    @PreAuthorize("hasAnyAuthority('system:approval:process', 'system:admin')")
    @AuditLog(
            operation = "审批处理",
            businessType = "APPROVAL",
            riskLevel = 4
    )
    public ApiResponse<String> processApproval(
            @PathVariable UUID id,
            @Validated @RequestBody ApprovalProcessDTO dto) {
        approvalService.processApproval(id, dto);

        return ApiResponse.success(dto.getApproved() ? "审批通过" : "审批拒绝");
    }

    /**
     * 撤回申请
     */
    @PostMapping("/{id}/withdraw")
    @AuditLog(
            operation = "撤回申请",
            businessType = "APPROVAL",
            riskLevel = 2
    )
    public ApiResponse<String> withdrawApproval(@PathVariable UUID id) {
        approvalService.withdrawApproval(id);

        return ApiResponse.success("申请已撤�?);
    }

    /**
     * 查询待我审批的列�?
     */
    @GetMapping("/pending")
    public ApiResponse<PageResult<ApprovalDTO>> getPendingApprovals(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<ApprovalDTO> result = approvalService.getPendingApprovals(page, size);

        return ApiResponse.success(PageResult.of(result));
    }

    /**
     * 查询我的申请历史
     */
    @GetMapping("/my-applications")
    public ApiResponse<PageResult<ApprovalDTO>> getMyApplications(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<ApprovalDTO> result = approvalService.getMyApplications(page, size);

        return ApiResponse.success(PageResult.of(result));
    }

    /**
     * 查询审批详情
     */
    @GetMapping("/{id}")
    public ApiResponse<ApprovalDTO> getApprovalDetail(@PathVariable UUID id) {
        ApprovalDTO detail = approvalService.getApprovalDetail(id);

        return ApiResponse.success(detail);
    }
}
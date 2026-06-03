package com.scmcloud.tenant.controller;

import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.tenant.api.dto.tenant.TenantCreateCommand;
import com.scmcloud.tenant.service.provisioning.TenantProvisioningResult;
import com.scmcloud.tenant.service.provisioning.TenantProvisioningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for tenant provisioning (onboarding).
 * Creates tenant + admin role + subscription + quotas in a single transaction.
 */
@Tag(name = "租户开通", description = "租户开通/初始化接口")
@RestController
@RequestMapping("/tenant/provisioning")
@RequiredArgsConstructor
public class TenantProvisioningController {

    private final TenantProvisioningService provisioningService;

    @Operation(summary = "开通新租户", description = "创建租户、默认订阅、资源配额，返回所有创建的ID")
    @PostMapping
    public ApiResponse<TenantProvisioningResult> provisionTenant(@Valid @RequestBody TenantCreateCommand command) {
        TenantProvisioningResult result = provisioningService.provision(command);
        if (result.isSuccess()) {
            return ApiResponse.success(result);
        } else {
            return ApiResponse.fail(result.getErrorMessage());
        }
    }
}

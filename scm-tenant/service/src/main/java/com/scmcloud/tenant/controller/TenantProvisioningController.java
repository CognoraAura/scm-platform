package com.scmcloud.tenant.controller;

import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.tenant.api.command.TenantCreateCommand;
import com.scmcloud.tenant.service.provisioning.TenantProvisioningResult;
import com.scmcloud.tenant.service.provisioning.TenantProvisioningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/provisioning")
@RequiredArgsConstructor
public class TenantProvisioningController {

    private final TenantProvisioningService provisioningService;

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

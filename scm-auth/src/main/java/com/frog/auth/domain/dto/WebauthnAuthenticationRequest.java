package com.frog.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebAuthn 认证请求DTO
 * <p>
 * 用于验证 WebAuthn凭证
 *
 * @author system
 * @since 2025-11-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebauthnAuthenticationRequest {
    @NotBlank(message = "凭证 ID不能为空")
    private String credentialId;

    ", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "客户端数据不能为�?)
    private String clientDataJSON;

    ", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "认证器数据不能为�?)
    private String authenticatorData;

    ", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "签名不能为空")
    private String signature;

    private String userHandle;

    @NotNull(message = "签名计数器不能为�?)
    private Long signCount;
}
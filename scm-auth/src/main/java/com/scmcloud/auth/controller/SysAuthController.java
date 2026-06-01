package com.scmcloud.auth.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.scmcloud.auth.service.ISysAuthService;
import com.scmcloud.common.dto.user.LoginRequest;
import com.scmcloud.common.dto.user.LoginResponse;
import com.scmcloud.common.dto.user.RefreshTokenRequest;
import com.scmcloud.common.dto.user.UserInfo;
import com.scmcloud.common.log.annotation.AuditLog;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.common.security.util.HttpServletRequestUtils;
import com.scmcloud.common.security.util.IpUtils;
import com.scmcloud.common.sentinel.annotation.RateLimit;
import com.scmcloud.common.util.UUIDv7Util;
import com.scmcloud.common.web.util.SecurityUtils;
import com.scmcloud.system.api.UserDubboService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统认证控制�?
 * 提供用户登录、登出、Token刷新等相关接�?
 * <p>
 * WebAuthn 相关接口请参�?{@link WebAuthnCredentialController}
 *
 * @author Deng
 * @version 1.0
 * @since 2025-10-14
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class SysAuthController {
    private final ISysAuthService authService;
    private final HttpServletRequestUtils httpServletRequestUtils;
    private final UserDubboService userDubboService;

    /**
     * 用户登录接口
     *
     * @param request 登录请求参数
     * @param httpRequest HTTP 请求对象
     * @return 登录响应结果
     */
    @PostMapping("/login")
    @SentinelResource(value = "auth_login")
    @RateLimit()
    public ApiResponse<LoginResponse> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletRequest httpRequest) {
        String ipAddress = IpUtils.getClientIp(httpRequest);
        String deviceId = httpServletRequestUtils.getDeviceId(httpRequest);
        String traceId = traceId(httpRequest);

        LoginResponse response = authService.login(request, ipAddress, deviceId);
        log.info("login success traceId={} user={} ip={} device={}", traceId, request.getUsername(), ipAddress, deviceId);

        return ApiResponse.success(response);
    }

    /**
     * 用户登出接口
     *
     * @param request HTTP 请求对象
     * @return 操作结果
     */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String token = httpServletRequestUtils.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return ApiResponse.fail(400, "Missing token");
        }
        UUID userId = SecurityUtils.getCurrentUserUuid().orElse(null);
        String traceId = traceId(request);

        authService.logout(token, userId, "用户主动登出");
        log.info("logout traceId={} userId={}", traceId, userId);

        return ApiResponse.success();
    }

    /**
     * 刷新 Token 接口
     *
     * @param request 刷新 Token 请求
     * @param httpRequest HTTP 请求对象
     * @return 登录响应结果
     */
    @PostMapping("/refresh")
    @RateLimit()
    public ApiResponse<LoginResponse> refreshToken(
            @RequestBody @Valid RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        if (!StringUtils.hasText(request.getRefreshToken())) {
            return ApiResponse.fail(400, "Missing refresh token");
        }
        String ipAddress = IpUtils.getClientIp(httpRequest);

        String deviceId = httpServletRequestUtils.getDeviceId(httpRequest);
        if (StringUtils.hasText(request.getDeviceId())) {
            deviceId = request.getDeviceId();
        }

        LoginResponse response = authService.refreshToken(
                request.getRefreshToken(),
                deviceId,
                ipAddress);

        return ApiResponse.success(response);
    }

    /**
     * 获取当前用户信息接口
     *
     * @param request HTTP 请求对象
     * @return 用户信息
     */
    @GetMapping("/userinfo")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<UserInfo> getUserInfo(HttpServletRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid().orElse(null);
        if (userId == null) {
            return ApiResponse.fail(401, "Unauthorized");
        }

        // 如果 Dubbo 不可用，说明系统异常，应该快速失�?
        UserInfo userInfo;
        try {
            userInfo = userDubboService.getUserInfo(userId);
        } catch (Exception ex) {
            String traceId = traceId(request);
            log.error("getUserInfo failed via Dubbo traceId={} userId={} error={}",
                    traceId, userId, ex.getMessage());
            return ApiResponse.fail(503, "User service unavailable");
        }

        return ApiResponse.success(userInfo);
    }

    /**
     * 强制用户登出接口
     *
     * @param userId 用户 ID
     * @param reason 登出原因
     * @return 操作结果
     */
    @PostMapping("/force-logout/{userId}")
    @PreAuthorize("hasAuthority('system:user:edit')")
    @AuditLog(
            operation = "强制下线",
            businessType = "USER",
            riskLevel = 3
    )
    public ApiResponse<Void> forceLogout(
            @PathVariable UUID userId,
            @RequestParam @NotBlank(message = "登出原因不能为空") String reason) {

        authService.forceLogout(userId, reason);
        log.info("force logout userId={} reason={}", userId, reason);

        return ApiResponse.success();
    }

    private String traceId(HttpServletRequest request) {
        String id = request.getHeader("X-Request-ID");
        if (!StringUtils.hasText(id)) {
            id = request.getHeader("traceparent");
        }
        return StringUtils.hasText(id) ? id : UUIDv7Util.generate().toString();
    }
}

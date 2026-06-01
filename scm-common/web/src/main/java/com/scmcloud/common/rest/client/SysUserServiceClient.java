package com.scmcloud.common.rest.client;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.scmcloud.common.response.ApiResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.Set;
import java.util.UUID;

/**
 * 用户服务客户端（@HttpExchange 版本�?
 * <p>替代 OpenFeign �?SysUserServiceClient</p>
 *
 * <p>架构说明�?
 * <ul>
 *   <li>主要通信：Dubbo (UserDubboService) - 高性能 RPC</li>
 *   <li>降级备用：RestClient + @HttpExchange (SysUserServiceClient) - HTTP REST</li>
 * </ul>
 *
 * <p>此客户端�?system-service �?SysUserController 端点对应
 *
 * <p>注意：认证相关方法（getUserByUsername, getUserRoles, getUserPermissions�?
 * 应使�?Dubbo 而不�?HTTP Exchange，因为它们在 controller 中不公开
 *
 * @author Claude
 * @since 2025-12-29
 */
@HttpExchange("/api/system/users")
public interface SysUserServiceClient {

    /**
     * 更新最后登录信�?
     * <p>对应: SysUserController.updateLastLogin()</p>
     * <p>Dubbo: UserDubboService.updateLastLogin()</p>
     *
     * <p>降级策略：返回成功，不中断登录流�?/p>
     *
     * @param userId 用户 ID
     * @param ipAddress IP 地址
     * @return 响应结果
     */
    @GetExchange("/{userId}/update-login")
    @SentinelResource(
        value = "user-service:updateLastLogin",
        fallback = "updateLastLoginFallback"
    )
    ApiResponse<Void> updateLastLogin(
        @PathVariable UUID userId,
        @RequestParam("ipAddress") String ipAddress
    );

    /**
     * 更新最后登录信息的降级方法
     * <p>降级策略：返回成功（不影响用户登录）</p>
     * <p>Note: Sentinel Dashboard 会记录降级事件，无需应用日志</p>
     *
     * @param userId 用户 ID
     * @param ipAddress IP 地址
     * @param ex 异常
     * @return 成功响应
     */
    default ApiResponse<Void> updateLastLoginFallback(
        UUID userId,
        String ipAddress,
        Throwable ex) {
        // 降级返回成功：登录信息更新失败不应中断用户登录流�?
        return ApiResponse.success();
    }

    @GetExchange("/{userId}/roles")
    ApiResponse<Set<String>> findRolesByUserId(@PathVariable UUID userId);

    @GetExchange("/{userId}/permissions")
    ApiResponse<Set<String>> findPermissionsByUserId(@PathVariable UUID userId);
}

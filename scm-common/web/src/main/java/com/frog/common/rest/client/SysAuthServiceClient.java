package com.frog.common.rest.client;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.frog.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.UUID;

/**
 * 认证服务客户端（@HttpExchange 版本）
 * <p>替代 OpenFeign 的 SysAuthServiceClient</p>
 *
 * @author Claude
 * @since 2025-12-29
 */
@HttpExchange("/api/auth")
public interface SysAuthServiceClient {

    /**
     * Logger for security-critical operations
     * <p>Used to audit force logout failures (security events)</p>
     */
    Logger log = LoggerFactory.getLogger(SysAuthServiceClient.class);

    /**
     * 强制用户登出
     *
     * <p>使用场景：
     * <ul>
     *   <li>管理员强制用户下线</li>
     *   <li>安全策略触发强制登出</li>
     *   <li>账号异常行为检测后强制登出</li>
     * </ul>
     *
     * <p>降级策略：返回 503 服务不可用</p>
     *
     * @param userId 用户 ID
     * @param reason 登出原因
     * @return 响应结果
     */
    @PostExchange("/force-logout/{userId}")
    @SentinelResource(
        value = "auth-service:forceLogout",
        fallback = "forceLogoutFallback"
    )
    ApiResponse<Void> forceLogout(
        @PathVariable UUID userId,
        @RequestParam("reason") String reason
    );

    /**
     * 强制登出的降级方法
     * <p><strong>SECURITY: 记录安全事件</strong> - 强制登出失败可能导致安全风险</p>
     * <p>降级策略：返回 503 服务不可用</p>
     *
     * @param userId 用户 ID
     * @param reason 登出原因
     * @param ex 异常
     * @return 失败响应
     */
    default ApiResponse<Void> forceLogoutFallback(
        UUID userId,
        String reason,
        Throwable ex) {

        // SECURITY: 记录强制登出失败，这可能是安全事件
        // 例如：发现账号被盗想强制下线，但服务不可用导致无法登出
        log.error("SECURITY ALERT: Force logout failed - potential security risk. " +
                  "userId={}, reason={}, error={}",
                  userId, reason, ex.getMessage());

        // 降级返回失败：认证服务不可用时无法强制登出
        return ApiResponse.fail(503, "认证服务暂时不可用，请稍后重试");
    }
}

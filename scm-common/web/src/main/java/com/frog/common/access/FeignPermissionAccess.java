package com.frog.common.access;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.frog.common.rest.client.SysPermissionServiceClient;
import com.frog.common.response.ApiResponse;
import com.frog.common.security.PermissionService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 基于 RestClient 的 PermissionService 回退实现。
 *
 * <p>已迁移：从 Feign 更改为 RestClient + @HttpExchange（2025-12-29）
 *
 * <p>已重构：现在实现 PermissionService 接口（common/core），
 * 而不是 PermissionAccessPort。这解耦了 common/web 和 system/api。
 *
 * <p>安全性：使用 Sentinel 断路器实现故障关闭模式
 * - 使用 Sentinel SphU 进行手动资源保护
 * - 服务失败或断路器打开时抛出异常
 * - 跟踪成功/失败率指标
 *
 * <p>Sentinel 资源：
 * - "permission:findByUrl" - 按 URL 查找权限
 * - "permission:findByUserId" - 按用户 ID 查找权限
 *
 * <p>当 Dubbo 不可用时，此实现用作回退方案。
 * 仅当不存在其他 PermissionService bean 时才会创建此实现。
 *
 * @author deng
 * @version 3.0
 * @since 2025-12-12
 */
@Component
@ConditionalOnMissingBean(PermissionService.class)
@Slf4j
public class FeignPermissionAccess implements PermissionService {
    private final SysPermissionServiceClient permissionServiceClient;
    private final MeterRegistry meterRegistry;

    public FeignPermissionAccess(SysPermissionServiceClient permissionServiceClient,
                                 MeterRegistry meterRegistry) {
        this.permissionServiceClient = permissionServiceClient;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Finds required permissions for a given URL and HTTP method via RestClient.
     *
     * <p>SECURITY: Fail-closed with Sentinel protection
     * - Throws exception if permission lookup fails
     * - Throws exception if Sentinel circuit is open
     *
     * @throws PermissionServiceException if permission lookup fails or circuit is open
     */
    @Override
    public List<String> findPermissionsByUrl(String url, String method) {
        try (Entry entry = SphU.entry("permission:findByUrl")) {
            List<String> permissions = permissionServiceClient.findPermissionsByUrl(url, method);
            meterRegistry.counter("security.permissions.lookup.success").increment();
            log.debug("Permission lookup success via RestClient: url={}, method={}, permissions={}",
                     url, method, permissions);
            return permissions != null ? permissions : List.of();

        } catch (BlockException ex) {
            // Sentinel circuit is open - deny access
            meterRegistry.counter("security.permissions.lookup.blocked").increment();
            log.error("SECURITY: Permission lookup BLOCKED by Sentinel - DENYING ACCESS. " +
                     "url={}, method={}", url, method, ex);
            throw new PermissionServiceException(
                "Permission service circuit open (rate limit/degraded) - access denied as safety measure", ex);

        } catch (Exception ex) {
            meterRegistry.counter("security.permissions.lookup.fail").increment();
            log.error("SECURITY: Permission lookup failed via RestClient - DENYING ACCESS. " +
                     "url={}, method={}", url, method, ex);

            // FAIL-CLOSED: Throw exception to deny access when permission check fails
            throw new PermissionServiceException(
                "Permission service unavailable via RestClient - access denied as safety measure", ex);
        }
    }

    /**
     * Finds all permissions for a given user via RestClient.
     *
     * <p>SECURITY: Fail-closed with Sentinel protection
     * - Throws exception if permission lookup fails
     * - Throws exception if Sentinel circuit is open
     *
     * @throws PermissionServiceException if permission lookup fails or circuit is open
     */
    @Override
    public Set<String> findAllPermissionsByUserId(UUID userId) {
        try (Entry entry = SphU.entry("permission:findByUserId")) {
            ApiResponse<Set<String>> resp = permissionServiceClient.getUserPermissions(userId);
            Set<String> perms = resp != null ? resp.data() : null;
            meterRegistry.counter("security.permissions.user.success").increment();
            log.debug("User permission lookup success via RestClient: userId={}, count={}",
                     userId, perms != null ? perms.size() : 0);
            return perms != null ? perms : Set.of();

        } catch (BlockException ex) {
            // Sentinel circuit is open - deny access
            meterRegistry.counter("security.permissions.user.blocked").increment();
            log.error("SECURITY: User permission lookup BLOCKED by Sentinel - DENYING ACCESS. " +
                     "userId={}", userId, ex);
            throw new PermissionServiceException(
                "Permission service circuit open (rate limit/degraded) - access denied as safety measure", ex);

        } catch (Exception ex) {
            meterRegistry.counter("security.permissions.user.fail").increment();
            log.error("SECURITY: User permission lookup failed via RestClient - DENYING ACCESS. " +
                     "userId={}", userId, ex);

            // FAIL-CLOSED: Throw exception to deny access when permission check fails
            throw new PermissionServiceException(
                "Permission service unavailable via RestClient - access denied as safety measure", ex);
        }
    }
}

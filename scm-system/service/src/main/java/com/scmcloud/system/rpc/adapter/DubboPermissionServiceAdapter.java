package com.scmcloud.system.rpc.adapter;

import com.scmcloud.common.security.PermissionService;
import com.scmcloud.system.api.PermissionDubboService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 基于 Dubbo �?PermissionService 接口实现�?
 *
 * <p>重构：已�?common/web 模块迁移�?system/service 模块�?
 * 这符合正确的架构分层——业务模块提供实现，
 * 基础设施模块 (common/web) 依赖于接口�?
 *
 * <p>安全性：实现了故障关闭模式——在服务失败时抛出异常，
 * 以防止由于权限检查失败而导致未经授权的访问�?
 *
 * <p>架构优势�?
 * - 通用模块不再依赖于业务模�?
 * - 遵循依赖倒置原则 (DIP)
 * - 允许每个项目使用不同�?PermissionService 实现
 *
 * @author 重构�?DubboPermissionAccess
 * @version 2.0
 * @since 2025-12-12
 */
@Component
@Primary
@ConditionalOnClass(DubboReference.class)
@Slf4j
public class DubboPermissionServiceAdapter implements PermissionService {

    @DubboReference
    private PermissionDubboService permissionDubboService;

    private final MeterRegistry meterRegistry;

    public DubboPermissionServiceAdapter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Finds required permissions for a given URL and HTTP method via Dubbo.
     *
     * <p>SECURITY: Fail-closed - throws exception if permission lookup fails.
     * This prevents granting access when permission service is unavailable.
     *
     * @throws PermissionServiceException if permission lookup fails
     */
    @Override
    public List<String> findPermissionsByUrl(String url, String method) {
        try {
            List<String> permissions = permissionDubboService.findPermissionsByUrl(url, method);
            meterRegistry.counter("security.permissions.dubbo.lookup.success").increment();
            log.debug("Permission lookup success via Dubbo: url={}, method={}, permissions={}",
                     url, method, permissions);
            return permissions != null ? permissions : List.of();

        } catch (Exception ex) {
            meterRegistry.counter("security.permissions.dubbo.lookup.fail").increment();
            log.error("SECURITY: Permission lookup failed via Dubbo - DENYING ACCESS. " +
                     "url={}, method={}", url, method, ex);

            // FAIL-CLOSED: Throw exception to deny access when permission check fails
            throw new PermissionServiceException(
                "Permission service unavailable via Dubbo - access denied as safety measure", ex);
        }
    }

    /**
     * Finds all permissions for a given user via Dubbo.
     *
     * <p>SECURITY: Fail-closed - throws exception if permission lookup fails.
     *
     * @throws PermissionServiceException if permission lookup fails
     */
    @Override
    public Set<String> findAllPermissionsByUserId(UUID userId) {
        try {
            Set<String> perms = permissionDubboService.findAllPermissionsByUserId(userId);
            meterRegistry.counter("security.permissions.dubbo.user.success").increment();
            log.debug("User permission lookup success via Dubbo: userId={}, count={}",
                     userId, perms != null ? perms.size() : 0);
            return perms != null ? perms : Set.of();

        } catch (Exception ex) {
            meterRegistry.counter("security.permissions.dubbo.user.fail").increment();
            log.error("SECURITY: User permission lookup failed via Dubbo - DENYING ACCESS. " +
                     "userId={}", userId, ex);

            // FAIL-CLOSED: Throw exception to deny access when permission check fails
            throw new PermissionServiceException(
                "Permission service unavailable via Dubbo - access denied as safety measure", ex);
        }
    }
}
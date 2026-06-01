package com.scmcloud.system.evaluator;

import com.scmcloud.common.web.domain.SecurityUser;
import com.scmcloud.system.service.ISysPermissionService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * 自定义权限评估器
 *
 * @author Deng
 * createData 2025/10/14 14:59
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomPermissionEvaluator implements PermissionEvaluator {
    private final ISysPermissionService permissionService;

    /**
     * 判断用户是否有指定权�?
     */
    @Override
    public boolean hasPermission(@NonNull Authentication authentication, @NonNull Object targetDomainObject,
                                 @NonNull Object permission) {
        if (!(authentication.getPrincipal() instanceof SecurityUser user)) {
            return false;
        }

        String permissionCode = permission.toString();

        // 检查用户是否有该权�?
        boolean hasPermission = permissionService.hasPermission(user.getUserId(), permissionCode);

        log.debug("Permission check - User: {}, Permission: {}, Result: {}",
                user.getUsername(), permissionCode, hasPermission);

        return hasPermission;
    }

    /**
     * 判断用户是否有指定资源的权限（基于资源ID�?
     */
    @Override
    public boolean hasPermission(@NonNull Authentication authentication, @NonNull Serializable targetId,
                                 @NonNull String targetType, @NonNull Object permission) {
        if (!(authentication.getPrincipal() instanceof SecurityUser user)) {
            return false;
        }

        // 可以实现更复杂的资源级权限控�?
        // 例如：检查用户是否可以访问特定ID的资�?
        boolean hasPermission = permissionService.hasResourcePermission(
                user.getUserId(), targetType, targetId, permission.toString());

        log.debug("Resource permission check - User: {}, TargetType: {}, TargetId: {}, Permission: {}, Result: {}",
                user.getUsername(), targetType, targetId, permission, hasPermission);

        return hasPermission;
    }
}


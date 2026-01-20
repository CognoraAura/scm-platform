package com.frog.common.tenant;

import com.frog.common.constant.RoleConstants;
import com.frog.common.exception.BusinessException;
import com.frog.common.response.ResultCode;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.UUID;

/**
 * 租户验证工具类

 * 提供租户上下文验证、数据归属验证等功能
 *
 * @author Claude Code
 * @since 2025-01-24
 */
@Slf4j
public class TenantValidationUtil {

    /**
     * 获取当前租户ID（如果未设置则抛出异常）
     *
     * @return 当前租户 ID
     * @throws BusinessException 如果租户上下文未设置
     */
    public static UUID getRequiredTenantId() {
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            log.error("租户上下文未设置，请求被拒绝");
            throw new BusinessException(ResultCode.TENANT_CONTEXT_MISSING.getCode(),
                    ResultCode.TENANT_CONTEXT_MISSING.getMessage());
        }
        return tenantId;
    }

    /**
     * 验证数据是否属于当前租户
     *
     * @param dataTenantId 数据所属的租户 ID
     * @throws BusinessException 如果数据不属于当前租户
     */
    public static void validateDataOwnership(UUID dataTenantId) {
        UUID currentTenantId = getRequiredTenantId();

        if (dataTenantId == null) {
            log.error("数据未关联租户，数据ID可能无效");
            throw new BusinessException(ResultCode.DATA_TENANT_MISSING.getCode(),
                    ResultCode.DATA_TENANT_MISSING.getMessage());
        }

        if (!currentTenantId.equals(dataTenantId)) {
            log.warn("租户数据访问越权：当前租户={}, 数据租户={}", currentTenantId, dataTenantId);
            throw new BusinessException(ResultCode.TENANT_DATA_ACCESS_DENIED.getCode(),
                    ResultCode.TENANT_DATA_ACCESS_DENIED.getMessage());
        }
    }

    /**
     * 验证用户是否为平台管理员（基于用户类型字符串）
     *
     * @param userType 用户类型
     * @return true=平台管理员, false=租户用户
     * @deprecated 建议使用无参的 isPlatformAdmin() 方法从 SecurityContext 获取
     */
    @Deprecated
    public static boolean isPlatformAdmin(String userType) {
        return RoleConstants.USER_TYPE_PLATFORM_ADMIN.equals(userType);
    }

    /**
     * 验证当前用户是否为平台管理员
     * 判断标准：
     * 1. 用户已登录且有有效的 SecurityContext
     * 2. 用户拥有平台管理员角色（ROLE_PLATFORM_ADMIN 或 ROLE_SUPER_ADMIN）
     * 3. 租户上下文为 NULL（平台管理员不属于任何租户）
     *
     * @return true=平台管理员, false=租户用户或未登录
     */
    public static boolean isPlatformAdmin() {
        try {
            // 1. 检查租户上下文（必要条件）
            UUID tenantId = TenantContextHolder.getTenantId();
            if (tenantId != null) {
                log.debug("用户属于租户 {}, 不是平台管理员", tenantId);
                return false;
            }

            // 2. 尝试从 SecurityContext 获取当前用户（需要 SecurityUtils）
            // 注意：为了避免循环依赖，这里使用反射调用 SecurityUtils
            // 如果无法获取到用户信息，则只依赖租户上下文判断
            try {
                Class<?> securityUtilsClass = Class.forName("com.frog.common.web.util.SecurityUtils");
                Object currentUser = securityUtilsClass.getMethod("getCurrentUser").invoke(null);

                if (currentUser == null) {
                    log.debug("未登录用户，不是平台管理员");
                    return false;
                }

                // 3. 检查角色（充分条件）
                Object rolesObj = currentUser.getClass().getMethod("getRoles").invoke(currentUser);
                if (rolesObj instanceof Set) {
                    @SuppressWarnings("unchecked")
                    Set<String> roles = (Set<String>) rolesObj;
                    boolean hasPlatformRole = roles.contains(RoleConstants.ROLE_PLATFORM_ADMIN)
                                           || roles.contains(RoleConstants.ROLE_SUPER_ADMIN);

                    if (!hasPlatformRole) {
                        log.debug("用户没有平台管理员角色");
                    }

                    return hasPlatformRole;
                }

            } catch (ClassNotFoundException e) {
                // SecurityUtils 类不存在，回退到基于租户ID的判断
                log.debug("SecurityUtils 类不存在，仅基于租户上下文判断");
            } catch (Exception e) {
                // 反射调用失败，回退到基于租户ID的判断
                log.debug("无法获取用户角色信息，仅基于租户上下文判断: {}", e.getMessage());
            }

            // 4. 兜底逻辑：如果无法获取角色信息，租户ID为NULL则认为是平台管理员
            // 这种情况通常发生在系统初始化或特殊场景
            return true;

        } catch (Exception e) {
            log.error("检查平台管理员权限时出错", e);
            return false; // 安全优先，异常情况返回 false
        }
    }

    /**
     * 验证当前用户是否为租户用户（非平台管理员）
     * 这是 !isPlatformAdmin() 的语义化版本，避免布尔值反转的代码异味
     *
     * @return true=租户用户, false=平台管理员
     */
    public static boolean isTenantUser() {
        return !isPlatformAdmin();
    }

    /**
     * 验证用户是否为租户管理员（基于用户类型字符串）
     *
     * @param userType 用户类型
     * @return true=租户管理员, false=其他
     * @deprecated 建议使用基于角色的判断方式
     */
    @Deprecated
    public static boolean isTenantAdmin(String userType) {
        return RoleConstants.USER_TYPE_TENANT_ADMIN.equals(userType);
    }

    /**
     * 验证用户是否有管理权限（平台管理员或租户管理员）
     *
     * @param userType 用户类型
     * @return true=有管理权限, false=普通用户
     * @deprecated 此方法依赖已废弃的方法，建议使用基于角色的权限检查
     */
    @Deprecated
    public static boolean hasAdminPrivilege(String userType) {
        return isPlatformAdmin(userType) || isTenantAdmin(userType);
    }

    /**
     * 验证角色是否属于当前租户或为平台角色
     *
     * @param roleTenantId 角色所属的租户ID（NULL表示平台角色）
     * @throws BusinessException 如果角色不属于当前租户且不是平台角色
     */
    public static void validateRoleAccess(UUID roleTenantId) {
        // 平台角色（tenant_id = NULL）所有租户都可以使用
        if (roleTenantId == null) {
            return;
        }

        // 租户角色必须属于当前租户
        UUID currentTenantId = getRequiredTenantId();
        if (!currentTenantId.equals(roleTenantId)) {
            log.warn("租户角色访问越权：当前租户={}, 角色租户={}", currentTenantId, roleTenantId);
            throw new BusinessException(ResultCode.TENANT_ROLE_ACCESS_DENIED.getCode(),
                    ResultCode.TENANT_ROLE_ACCESS_DENIED.getMessage());
        }
    }

    /**
     * 验证权限是否属于当前租户或为平台权限
     *
     * @param permissionTenantId 权限所属的租户ID（NULL表示平台权限）
     * @throws BusinessException 如果权限不属于当前租户且不是平台权限
     */
    public static void validatePermissionAccess(UUID permissionTenantId) {
        // 平台权限（tenant_id = NULL）所有租户都可以使用
        if (permissionTenantId == null) {
            return;
        }

        // 租户权限必须属于当前租户
        UUID currentTenantId = getRequiredTenantId();
        if (!currentTenantId.equals(permissionTenantId)) {
            log.warn("租户权限访问越权：当前租户={}, 权限租户={}", currentTenantId, permissionTenantId);
            throw new BusinessException(ResultCode.TENANT_PERMISSION_ACCESS_DENIED.getCode(),
                    ResultCode.TENANT_PERMISSION_ACCESS_DENIED.getMessage());
        }
    }

    /**
     * 验证是否允许创建平台级资源（角色、权限等）
     *
     * @param userType 用户类型
     * @throws BusinessException 如果用户无权创建平台级资源
     * @deprecated 使用 requirePlatformAdmin() 替代
     */
    @Deprecated
    public static void validatePlatformResourceCreation(String userType) {
        if (!isPlatformAdmin(userType)) {
            log.warn("非平台管理员尝试创建平台级资源，用户类型: {}", userType);
            throw new BusinessException(ResultCode.PLATFORM_RESOURCE_ACCESS_DENIED.getCode(),
                    ResultCode.PLATFORM_RESOURCE_ACCESS_DENIED.getMessage());
        }
    }

    /**
     * 要求当前用户必须是平台管理员，否则抛出异常
     * 这是 isTenantUser() 检查的便捷方法，避免布尔值反转的代码异味
     *
     * @throws BusinessException 如果当前用户不是平台管理员
     */
    public static void requirePlatformAdmin() {
        if (isTenantUser()) {
            log.warn("非平台管理员尝试执行平台管理操作");
            throw new BusinessException(ResultCode.PLATFORM_RESOURCE_ACCESS_DENIED.getCode(),
                    ResultCode.PLATFORM_RESOURCE_ACCESS_DENIED.getMessage());
        }
    }

    /**
     * 允许平台管理员临时访问指定租户的数据
     *
     * @param userType 用户类型
     * @param targetTenantId 目标租户 ID
     * @return 是否允许访问
     * @deprecated 此方法依赖已废弃的方法，建议直接使用 isPlatformAdmin() 进行判断
     */
    @Deprecated
    public static boolean allowCrossTenantAccess(String userType, UUID targetTenantId) {
        // 只有平台管理员可以跨租户访问
        if (!isPlatformAdmin(userType)) {
            return false;
        }

        UUID currentTenantId = TenantContextHolder.getTenantId();

        // 平台管理员的 tenant_id 为 NULL，或者目标租户与当前租户不同
        return currentTenantId == null || !currentTenantId.equals(targetTenantId);
    }

    /**
     * 验证部门是否属于当前租户
     *
     * @param deptTenantId 部门所属的租户 ID
     * @throws BusinessException 如果部门不属于当前租户
     */
    public static void validateDepartmentOwnership(UUID deptTenantId) {
        UUID currentTenantId = getRequiredTenantId();

        if (deptTenantId == null) {
            log.error("部门未关联租户");
            throw new BusinessException(ResultCode.DEPT_TENANT_MISSING.getCode(),
                    ResultCode.DEPT_TENANT_MISSING.getMessage());
        }

        if (!currentTenantId.equals(deptTenantId)) {
            log.warn("租户部门访问越权：当前租户={}, 部门租户={}", currentTenantId, deptTenantId);
            throw new BusinessException(ResultCode.TENANT_DEPT_ACCESS_DENIED.getCode(),
                    ResultCode.TENANT_DEPT_ACCESS_DENIED.getMessage());
        }
    }

    /**
     * 记录租户操作日志
     *
     * @param operation 操作类型
     * @param resourceType 资源类型
     * @param resourceId 资源 ID
     */
    public static void logTenantOperation(String operation, String resourceType, UUID resourceId) {
        UUID tenantId = TenantContextHolder.getTenantId();
        log.info("租户操作日志 - 租户ID: {}, 操作: {}, 资源类型: {}, 资源ID: {}",
            tenantId, operation, resourceType, resourceId);

        // TODO: 可以集成到租户操作日志表（tenant_operation_log）
    }
}

package com.frog.common.constant;

/**
 * 角色常量类
 * 定义系统中的角色类型和角色代码常量
 *
 * @author Claude Code
 * @since 2025-01-15
 */
public final class RoleConstants {

    private RoleConstants() {
        throw new UnsupportedOperationException("This is a constants class and cannot be instantiated");
    }

    // ==================== 角色类型 ====================

    /**
     * 平台角色类型
     * 跨所有租户，只有平台管理员可以管理
     */
    public static final String ROLE_TYPE_PLATFORM = "PLATFORM_ROLE";

    /**
     * 租户角色类型
     * 租户内角色，租户管理员可以管理
     */
    public static final String ROLE_TYPE_TENANT = "TENANT_ROLE";

    // ==================== 角色代码（Spring Security 角色名称）====================

    /**
     * 超级管理员角色
     * 拥有系统最高权限
     */
    public static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";

    /**
     * 平台管理员角色
     * 可以管理所有租户和平台级资源
     */
    public static final String ROLE_PLATFORM_ADMIN = "ROLE_PLATFORM_ADMIN";

    /**
     * 租户管理员角色
     * 可以管理本租户内的资源和用户
     */
    public static final String ROLE_TENANT_ADMIN = "ROLE_TENANT_ADMIN";

    /**
     * 租户普通用户角色
     */
    public static final String ROLE_TENANT_USER = "ROLE_TENANT_USER";

    // ==================== 角色分类 ====================

    /**
     * 业务角色
     */
    public static final String ROLE_CATEGORY_BUSINESS = "BUSINESS";

    /**
     * 职能角色
     */
    public static final String ROLE_CATEGORY_FUNCTIONAL = "FUNCTIONAL";

    /**
     * 自定义角色
     */
    public static final String ROLE_CATEGORY_CUSTOM = "CUSTOM";

    // ==================== 用户类型（兼容旧代码）====================

    /**
     * 平台管理员用户类型
     */
    public static final String USER_TYPE_PLATFORM_ADMIN = "PLATFORM_ADMIN";

    /**
     * 租户管理员用户类型
     */
    public static final String USER_TYPE_TENANT_ADMIN = "TENANT_ADMIN";

    /**
     * 租户普通用户类型
     */
    public static final String USER_TYPE_TENANT_USER = "TENANT_USER";
}
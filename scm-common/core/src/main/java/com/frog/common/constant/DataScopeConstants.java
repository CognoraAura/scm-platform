package com.frog.common.constant;

/**
 * 数据权限范围常量类
 * 定义系统中的数据权限范围类型
 *
 * @author Claude Code
 * @since 2025-01-15
 */
public final class DataScopeConstants {

    private DataScopeConstants() {
        throw new UnsupportedOperationException("This is a constants class and cannot be instantiated");
    }

    /**
     * 全部数据权限
     * 可以查看所有数据
     */
    public static final int SCOPE_ALL = 1;

    /**
     * 自定义数据权限
     * 可以查看指定部门的数据（通过 custom_dept_ids 指定）
     */
    public static final int SCOPE_CUSTOM = 2;

    /**
     * 本部门数据权限
     * 只能查看本部门的数据
     */
    public static final int SCOPE_DEPT = 3;

    /**
     * 本部门及以下数据权限
     * 可以查看本部门及其下级部门的数据
     */
    public static final int SCOPE_DEPT_AND_CHILD = 4;

    /**
     * 仅本人数据权限
     * 只能查看自己创建的数据
     */
    public static final int SCOPE_SELF = 5;
}
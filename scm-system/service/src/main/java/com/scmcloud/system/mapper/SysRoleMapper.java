package com.scmcloud.system.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.system.domain.entity.SysRole;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.UUID;

/**
 * <p>
 * 角色�?Mapper 接口
 * </p>
 *
 * @author author
 * @since 2025-10-15
 */
@Mapper
@DS("permission")
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 检查角色编码是否存在（不考虑租户�?
     */
    @Select("""
            SELECT COUNT(*) > 0 FROM sys_role
            WHERE role_code = #{roleCode} AND NOT deleted
            """)
    boolean existsByRoleCode(@Param("roleCode") String roleCode);

    /**
     * 检查角色编码在指定租户下是否存�?
     * 用于多租户环境下的唯一性校�?
     */
    @Select("""
            SELECT COUNT(*) > 0 FROM sys_role
            WHERE role_code = #{roleCode}
              AND (tenant_id = #{tenantId} OR (tenant_id IS NULL AND #{tenantId} IS NULL))
              AND NOT deleted
            """)
    boolean existsByRoleCodeAndTenantId(@Param("roleCode") String roleCode, @Param("tenantId") UUID tenantId);

    /**
     * 根据角色编码查询角色 ID
     */
    @Select("""
            SELECT id FROM sys_role
            WHERE role_code = #{roleCode} AND status = 1 AND NOT deleted
            """)
    UUID findIdByRoleCode(@Param("roleCode") String roleCode);

    /**
     * 根据角色编码查询角色
     */
    @Select("""
            SELECT * FROM sys_role
            WHERE role_code = #{roleCode} AND NOT deleted
            """)
    SysRole findByRoleCode(@Param("roleCode") String roleCode);

    /**
     * 获取角色等级
     * <p>
     * role_level 字段值越小，权限越高
     *
     * @param roleId 角色 ID
     * @return 角色等级（role_level），如果角色不存在则返回 null
     */
    @Select("""
            SELECT role_level FROM sys_role
            WHERE id = #{roleId} AND NOT deleted
            """)
    Integer getRoleLevel(@Param("roleId") UUID roleId);

    /**
     * 获取角色所属的租户 ID
     * <p>
     * 用于验证角色归属（NULL 表示平台角色�?
     *
     * @param roleId 角色 ID
     * @return 租户 ID（NULL 表示平台角色�?
     */
    @Select("""
            SELECT tenant_id FROM sys_role
            WHERE id = #{roleId} AND NOT deleted
            """)
    UUID getRoleTenantId(@Param("roleId") UUID roleId);
}

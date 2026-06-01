package com.scmcloud.system.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.common.dto.permission.PermissionDTO;
import com.scmcloud.system.domain.entity.SysPermission;
import org.apache.ibatis.annotations.*;

import java.io.Serializable;
import java.security.Permission;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * <p>
 * 权限�?Mapper 接口
 * </p>
 *
 * @author author
 * @since 2025-10-14
 */
@Mapper
@DS("permission")
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    /**
     * 查询用户权限
     */
    @Select("""
            SELECT DISTINCT p.permission_code FROM sys_permission p
            INNER JOIN sys_role_permission rp ON p.id = rp.permission_id
            INNER JOIN sys_user_role ur ON rp.role_id = ur.role_id
            WHERE ur.user_id = #{userId}
            AND ur.approval_status = 2
            AND (ur.expire_time IS NULL OR ur.expire_time > NOW())
            AND p.status = 1 AND NOT p.deleted
            """)
    Set<String> findAllPermissionsByUserId(@Param("userId") UUID userId);

    /**
     * 查询用户角色
     */
    @Select("""
            SELECT DISTINCT r.role_code FROM sys_role r
            INNER JOIN sys_user_role ur ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
            AND ur.approval_status = 2
            AND (ur.expire_time IS NULL OR ur.expire_time > NOW())
            AND r.status = 1 AND NOT r.deleted
            """)
    Set<String> findRolesByUserId(@Param("userId") UUID userId);

    /**
     * 查询角色权限�?
     */
    @Select("""
            SELECT * FROM sys_permission WHERE status = 1 AND NOT deleted
            ORDER BY sort_order ASC
            """)
    List<SysPermission> findPermissionTree();

    /**
     * 根据角色 ID查询权限
     */
    @Select("""
            SELECT p.* FROM sys_permission p
            INNER JOIN sys_role_permission rp ON p.id = rp.permission_id
            WHERE rp.role_id = #{roleId} AND p.status = 1 AND NOT p.deleted
            """)
    List<Permission> findPermissionsByRoleId(@Param("roleId") UUID roleId);

    /**
     * 检查资源权�?
     */
    @Select("""
            <script>
            SELECT COUNT(*) > 0 FROM sys_user_role ur
            INNER JOIN sys_role_permission rp ON ur.role_id = rp.role_id
            INNER JOIN sys_permission p ON rp.permission_id = p.id
            WHERE ur.user_id = #{userId}
            AND p.permission_code = #{permission}
            AND p.status = 1
            </script>
            """)
    boolean checkResourcePermission(@Param("userId") UUID userId,
                                    @Param("resourceType") String resourceType,
                                    @Param("resourceId") Serializable resourceId,
                                    @Param("permission") String permission);

    /**
     * 查询用户菜单�?
     */
    @Select("""
            SELECT DISTINCT p.* FROM sys_permission p
            INNER JOIN sys_role_permission rp ON p.id = rp.permission_id
            INNER JOIN sys_user_role ur ON rp.role_id = ur.role_id
            WHERE ur.user_id = #{userId}
            AND ur.approval_status = 2
            AND (ur.expire_time IS NULL OR ur.expire_time > NOW())
            AND p.permission_type IN (1, 2)
            AND p.visible = true AND p.status = 1 AND NOT p.deleted
            ORDER BY p.sort_order ASC
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "parentId", column = "parent_id"),
            @Result(property = "children", column = "id",
                    many = @Many(select = "findChildrenPermissions"))
    })
    List<PermissionDTO> findMenuTreeByUserId(@Param("userId") UUID userId);

    /**
     * 查询子权�?
     */
    @Select("""
            SELECT * FROM sys_permission
            WHERE parent_id = #{parentId}
            AND permission_type IN (1, 2)
            AND visible = true AND status = 1 AND NOT deleted
            ORDER BY sort_order ASC
            """)
    List<PermissionDTO> findChildrenPermissions(@Param("parentId") UUID parentId);

    /**
     * 根据 URL和方法查询需要的权限
     */
    @Select("""
            SELECT permission_code FROM sys_permission
            WHERE api_path = #{url}
            AND (http_method = #{method} OR http_method = '*')
            AND status = 1 AND NOT deleted
            """)
    List<String> findPermissionsByUrl(@Param("url") String url,
                                      @Param("method") String method);

    /**
     * 检查权限编码是否存�?
     */
    @Select("""
            SELECT COUNT(*) > 0 FROM sys_permission
            WHERE permission_code = #{permissionCode} AND NOT deleted
            """)
    boolean existsByPermissionCode(@Param("permissionCode") String permissionCode);
}

package com.scmcloud.system.service.query;

import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.system.domain.entity.SysUser;
import com.scmcloud.system.mapper.*;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色跨库查询服务
 * <p>
 * 处理与角色相关的跨库查询操作（db_permission �db_user �db_org�
 *
 * @author Deng
 * @since 2025-01-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleCrossDatabaseQueryService {
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleDeptMapper roleDeptMapper;
    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;

    /**
     * 获取角色的等�
     * <p>
     * 用于角色授权时的权限检�
     * 跨库查询：db_permission
     *
     * @param roleId 角色 ID
     * @return 角色等级（role_level�
     */
    @Slave
    @Timed(value = "cross_db_query", extraTags = {"method", "getRoleLevel"})
    public Integer getRoleLevel(UUID roleId) {
        if (roleId == null) {
            return null;
        }
        return roleMapper.getRoleLevel(roleId);
    }

    /**
     * 获取角色所属的租户 ID
     * <p>
     * 用于角色授权时验证角色归属（只能分配本租户或平台角色�
     * 跨库查询：db_permission
     *
     * @param roleId 角色 ID
     * @return 租户 ID（NULL 表示平台角色�
     */
    @Slave
    @Timed(value = "cross_db_query", extraTags = {"method", "getRoleTenantId"})
    public UUID getRoleTenantId(UUID roleId) {
        if (roleId == null) {
            return null;
        }
        return roleMapper.getRoleTenantId(roleId);
    }

    /**
     * 根据角色编码查询第一个有效用户的ID
     *
     * @param roleCode 角色编码
     * @return 第一个有效用户的 ID，如果没有则返回 null
     */
    @Slave
    @Timed(value = "cross_db_query", extraTags = {"method", "findFirstUserIdByRoleCode"})
    public UUID findFirstUserIdByRoleCode(String roleCode) {
        if (roleCode == null || roleCode.trim().isEmpty()) {
            return null;
        }

        // 1. �permission 库查询角�ID
        UUID roleId = roleMapper.findIdByRoleCode(roleCode);
        if (roleId == null) {
            return null;
        }

        // 2. �permission 库查询该角色的用�ID 列表
        List<UUID> userIds = userRoleMapper.findUserIdsByRoleId(roleId);
        if (userIds == null || userIds.isEmpty()) {
            return null;
        }

        // 3. �user 库查询有效用户，取第一�
        List<SysUser> users = userMapper.selectBasicInfoByIds(userIds);
        return users.stream()
                .filter(u -> u.getStatus() != null && u.getStatus() == 1)
                .min(Comparator.comparing(SysUser::getCreateTime))
                .map(SysUser::getId)
                .orElse(null);
    }

    /**
     * 获取角色可访问的部门 ID 列表（递归包含子部门）
     * <p>
     * 替代�SysRoleDeptMapper.findAccessibleDeptIds
     *
     * @param roleId 角色 ID
     * @return 可访问的部门 ID 列表
     */
    @Slave
    @Timed(value = "cross_db_query", extraTags = {"method", "findAccessibleDeptIds"})
    public List<UUID> findAccessibleDeptIds(UUID roleId) {
        if (roleId == null) {
            return Collections.emptyList();
        }

        Set<UUID> result = new HashSet<>();

        // 1. 获取不需要递归的部�ID
        List<UUID> directDeptIds = roleDeptMapper.findDeptIdsWithoutChildren(roleId);
        if (directDeptIds != null) {
            result.addAll(directDeptIds);
        }

        // 2. 获取需要递归子部门的部门 ID
        List<UUID> deptIdsWithChildren = roleDeptMapper.findDeptIdsWithChildren(roleId);
        if (deptIdsWithChildren != null && !deptIdsWithChildren.isEmpty()) {
            // 3. �org 库递归查询子部�
            List<UUID> allChildDepts = deptMapper.selectDeptsAndChildren(deptIdsWithChildren);
            if (allChildDepts != null) {
                result.addAll(allChildDepts);
            }
        }

        return new ArrayList<>(result);
    }

    /**
     * 查询即将过期的角色（包含用户信息�
     * <p>
     * 替代�SysUserMapper.findExpiringRoles
     *
     * @param days 天数
     * @return 即将过期的角色信息列�
     */
    @Slave
    @Timed(value = "cross_db_query", extraTags = {"method", "findExpiringRolesWithUserInfo"})
    public List<Map<String, Object>> findExpiringRolesWithUserInfo(Integer days) {
        if (days == null || days <= 0) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> expiringRoles = userRoleMapper.findExpiringRolesForNotification(days);
        return enrichRolesWithUserInfo(expiringRoles, true);
    }

    /**
     * 查询已过期的角色（包含用户信息）
     * <p>
     * 替代�SysUserMapper.findExpiredRoles
     *
     * @return 已过期的角色信息列表
     */
    @Slave
    @Timed(value = "cross_db_query", extraTags = {"method", "findExpiredRolesWithUserInfo"})
    public List<Map<String, Object>> findExpiredRolesWithUserInfo() {
        List<Map<String, Object>> expiredRoles = userRoleMapper.findExpiredRolesForCleanup();
        return enrichRolesWithUserInfo(expiredRoles, false);
    }

    /**
     * 为角色列表补充用户信�
     *
     * @param roles        角色列表
     * @param includeEmail 是否包含邮箱字段
     * @return 包含用户信息的角色列�
     */
    private List<Map<String, Object>> enrichRolesWithUserInfo(List<Map<String, Object>> roles, boolean includeEmail) {
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 收集用户 ID
        Set<UUID> userIds = roles.stream()
                .map(m -> (UUID) m.get("user_id"))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 2. �user 库批量查询用户信�
        Map<UUID, SysUser> userMap = Collections.emptyMap();
        if (!userIds.isEmpty()) {
            List<SysUser> users = userMapper.selectBasicInfoByIds(new ArrayList<>(userIds));
            userMap = users.stream()
                    .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));
        }

        // 3. 组装结果
        Map<UUID, SysUser> finalUserMap = userMap;
        return roles.stream()
                .map(m -> {
                    Map<String, Object> result = new HashMap<>(m);
                    UUID userId = (UUID) m.get("user_id");
                    SysUser user = finalUserMap.get(userId);
                    if (user != null) {
                        result.put("username", user.getUsername());
                        if (includeEmail) {
                            result.put("email", user.getEmail());
                        }
                    }
                    return result;
                })
                .collect(Collectors.toList());
    }
}
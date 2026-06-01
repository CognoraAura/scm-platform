package com.scmcloud.system.service.Impl;

import com.scmcloud.common.security.PermissionQueryService;
import com.scmcloud.system.domain.entity.SysDept;
import com.scmcloud.system.domain.entity.SysRole;
import com.scmcloud.system.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 权限查询服务实现�?

 * 提供权限、角色、数据权限等查询功能，支持缓�?
 *
 * @author Claude Code
 * @since 2025-01-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionQueryServiceImpl implements PermissionQueryService {
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;
    private final SysRoleMapper roleMapper;

    /**
     * 查询用户的所有权限编码（带缓存）
     */
    @Override
    @Cacheable(value = "userPermissions", key = "#userId")
    public Set<String> getUserPermissions(UUID userId) {
        if (userId == null) {
            return Collections.emptySet();
        }

        Set<String> permissions = userRoleMapper.findPermissionCodesByUserId(userId);
        return permissions != null ? permissions : Collections.emptySet();
    }

    /**
     * 查询用户的所有角色编码（带缓存）
     */
    @Override
    @Cacheable(value = "userRoles", key = "#userId")
    public Set<String> getUserRoles(UUID userId) {
        if (userId == null) {
            return Collections.emptySet();
        }

        Set<String> roles = userRoleMapper.findRoleCodesByUserId(userId);
        return roles != null ? roles : Collections.emptySet();
    }

    /**
     * 获取用户的数据权限范围（带缓存）

     * 取用户所有角色中权限范围最大的（数值最小的�?
     * NULL = 1（全部）, DEPT = 2（本部门�? DEPT_AND_SUB = 3（本部门及下级）, SELF = 4（仅本人�? CUSTOM = 5（自定义�?
     */
    @Override
    @Cacheable(value = "userDataScope", key = "#userId")
    public String getUserDataScope(UUID userId) {
        if (userId == null) {
            return "SELF"; // 默认仅本�?
        }

        // 获取用户所有有效角色的data_scope，取最小值（权限最大）
        Integer dataScopeValue = userRoleMapper.getUserDataScope(userId);

        if (dataScopeValue == null || dataScopeValue == 1) {
            return "ALL";
        } else if (dataScopeValue == 2) {
            return "DEPT";
        } else if (dataScopeValue == 3) {
            return "DEPT_AND_SUB";
        } else if (dataScopeValue == 4) {
            return "SELF";
        } else if (dataScopeValue == 5) {
            return "CUSTOM";
        }

        return "SELF"; // 默认仅本�?
    }

    /**
     * 获取用户的部门ID（带缓存�?
     */
    @Override
    @Cacheable(value = "userDeptId", key = "#userId")
    public UUID getUserDeptId(UUID userId) {
        if (userId == null) {
            return null;
        }

        return userMapper.getUserDeptId(userId);
    }

    /**
     * 获取部门路径（带缓存�?
     */
    @Override
    @Cacheable(value = "deptPath", key = "#deptId")
    public String getDeptPath(UUID deptId) {
        if (deptId == null) {
            return null;
        }

        SysDept dept = deptMapper.selectById(deptId);
        return dept != null ? dept.getDeptPath() : null;
    }

    /**
     * 获取用户可访问的部门ID列表（带缓存�?
     */
    @Override
    @Cacheable(value = "accessibleDeptIds", key = "#userId + ':' + #tenantId + ':' + #dataScope")
    public List<UUID> getAccessibleDepartmentIds(UUID userId, UUID tenantId, String dataScope) {
        if (userId == null) {
            return Collections.emptyList();
        }

        // ALL - 返回空列表表示可以访问所有部门（不需要过滤）
        if ("ALL".equals(dataScope)) {
            return Collections.emptyList();
        }

        // SELF - 不涉及部门过�?
        if ("SELF".equals(dataScope)) {
            return Collections.emptyList();
        }

        // 获取用户部门 ID
        UUID userDeptId = getUserDeptId(userId);
        if (userDeptId == null) {
            return Collections.emptyList();
        }

        // DEPT - 只能访问本部�?
        if ("DEPT".equals(dataScope)) {
            return List.of(userDeptId);
        }

        // DEPT_AND_SUB - 可以访问本部门及下级部门
        if ("DEPT_AND_SUB".equals(dataScope)) {
            List<UUID> deptIds = deptMapper.selectDeptAndChildren(userDeptId);
            return deptIds != null ? deptIds : List.of(userDeptId);
        }

        // CUSTOM - 自定义规则（查询 sys_role_dept 表）
        if ("CUSTOM".equals(dataScope)) {
            // TODO: 实现自定义数据权限规则查�?
            log.warn("CUSTOM data scope not implemented yet for userId: {}", userId);
            return List.of(userDeptId);
        }

        return Collections.emptyList();
    }

    /**
     * 获取角色等级（带缓存�?
     */
    @Override
    @Cacheable(value = "roleLevel", key = "#roleId")
    public Integer getRoleLevel(UUID roleId) {
        if (roleId == null) {
            return null;
        }

        SysRole role = roleMapper.selectById(roleId);
        return role != null ? role.getRoleLevel() : null;
    }

    /**
     * 获取用户的最高角色等级（带缓存）
     */
    @Override
    @Cacheable(value = "userMaxRoleLevel", key = "#userId")
    public Integer getUserMaxRoleLevel(UUID userId) {
        if (userId == null) {
            return null;
        }

        // 获取用户所有有效角�?ID
        List<UUID> roleIds = userRoleMapper.findEffectiveRoleIds(userId);
        if (roleIds == null || roleIds.isEmpty()) {
            return null;
        }

        // 查询所有角色的等级，取最大�?
        Integer maxLevel = null;
        for (UUID roleId : roleIds) {
            Integer level = getRoleLevel(roleId);
            if (level != null && (maxLevel == null || level > maxLevel)) {
                maxLevel = level;
            }
        }

        return maxLevel;
    }
}
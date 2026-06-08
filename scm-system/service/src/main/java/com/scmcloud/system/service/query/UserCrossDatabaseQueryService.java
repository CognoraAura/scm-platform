package com.scmcloud.system.service.query;

import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.system.domain.entity.SysUser;
import com.scmcloud.system.mapper.SysUserMapper;
import com.scmcloud.system.mapper.SysUserRoleMapper;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户跨库查询服务
 * <p>
 * 处理与用户相关的跨库查询操作（db_user �db_permission �db_org�
 *
 * @author Deng
 * @since 2025-01-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCrossDatabaseQueryService {
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;

    /**
     * 获取用户基本信息
     * <p>
     * 替代 SysUserMapper.selectById
     * 跨库查询：db_user
     *
     * @param userId 用户 ID
     * @return 用户实体
     */
    @Slave
    @Timed(value = "cross_db_query", extraTags = {"method", "getUserBasicInfo"})
    public SysUser getUserBasicInfo(UUID userId) {
        if (userId == null) {
            return null;
        }
        return userMapper.selectById(userId);
    }

    /**
     * 批量获取用户基本信息
     * <p>
     * 替代 SysUserMapper.selectBasicInfoByIds
     * 跨库查询：db_user
     *
     * @param userIds 用户 ID 列表
     * @return 用户实体列表
     */
    @Slave
    @Timed(value = "cross_db_query", extraTags = {"method", "getUserBasicInfoBatch"})
    public List<SysUser> getUserBasicInfoBatch(List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        return userMapper.selectBasicInfoByIds(userIds);
    }

    /**
     * 批量获取用户基本信息（Map 形式�
     *
     * @param userIds 用户 ID 列表
     * @return 用户 ID �用户实体 映射
     */
    @Slave
    public Map<UUID, SysUser> getUserBasicInfoMap(List<UUID> userIds) {
        List<SysUser> users = getUserBasicInfoBatch(userIds);
        return users.stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));
    }

    /**
     * 查询用户角色（带角色名称�
     * <p>
     * 替代 SysUserRoleMapper.findUserRolesWithNames
     * 跨库查询：db_permission
     *
     * @param userId 用户 ID
     * @return 角色列表（包�id, name 字段�
     */
    @Slave
    @Timed(value = "cross_db_query", extraTags = {"method", "findUserRolesWithNames"})
    @Cacheable(value = "userRoles", key = "#userId", unless = "#result.isEmpty()")
    public List<Map<String, Object>> findUserRolesWithNames(UUID userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return userRoleMapper.findUserRolesWithNames(userId);
    }

    /**
     * 查询用户角色编码集合
     * <p>
     * 替代 SysUserRoleMapper.findRoleCodesByUserId
     * 跨库查询：db_permission
     *
     * @param userId 用户 ID
     * @return 角色编码集合
     */
    @Slave
    @Timed(value = "cross_db_query", extraTags = {"method", "findRoleCodesByUserId"})
    @Cacheable(value = "userRoleCodes", key = "#userId", unless = "#result.isEmpty()")
    public Set<String> findRoleCodesByUserId(UUID userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        return userRoleMapper.findRoleCodesByUserId(userId);
    }

    /**
     * 获取用户的最大角色等�
     * <p>
     * 用于角色授权时的权限检查（只能分配不高于自己的角色�
     * 跨库查询：db_permission
     *
     * @param userId 用户 ID
     * @return 最大角色等级（role_level 最小值，因为等级越小权限越高），如果用户没有角色则返�null
     */
    @Slave
    @Timed(value = "cross_db_query", extraTags = {"method", "getUserMaxRoleLevel"})
    public Integer getUserMaxRoleLevel(UUID userId) {
        if (userId == null) {
            return null;
        }
        return userRoleMapper.getUserMaxRoleLevel(userId);
    }

    /**
     * 统计用户有效角色�
     * <p>
     * 替代 SysUserRoleMapper.countUserRoles
     * 跨库查询：db_permission
     *
     * @param userId 用户 ID
     * @return 有效角色�
     */
    @Slave
    public Integer countUserRoles(UUID userId) {
        return getCountOrDefault(userId, userRoleMapper::countUserRoles);
    }

    /**
     * 查询用户权限编码集合
     * <p>
     * 替代 SysUserRoleMapper.findPermissionCodesByUserId
     * 跨库查询：db_permission
     *
     * @param userId 用户 ID
     * @return 权限编码集合
     */
    @Slave
    @Timed(value = "cross_db_query", extraTags = {"method", "findPermissionCodesByUserId"})
    @Cacheable(value = "userPermissionCodes", key = "#userId", unless = "#result.isEmpty()")
    public Set<String> findPermissionCodesByUserId(UUID userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        return userRoleMapper.findPermissionCodesByUserId(userId);
    }

    /**
     * 获取用户数据权限范围
     * <p>
     * 替代 SysUserRoleMapper.getUserDataScope
     * 跨库查询：db_permission
     *
     * @param userId 用户 ID
     * @return 数据权限范围�-全部, 2-自定� 3-本部� 4-本部门及子部� 5-仅本人）
     */
    @Slave
    @Timed(value = "cross_db_query", extraTags = {"method", "getUserDataScope"})
    @Cacheable(value = "userDataScope", key = "#userId")
    public Integer getUserDataScope(UUID userId) {
        if (userId == null) {
            return 5; // 默认仅本�
        }
        Integer dataScope = userRoleMapper.getUserDataScope(userId);
        return dataScope != null ? dataScope : 5;
    }

    /**
     * 获取用户最大审批金�
     * <p>
     * 替代 SysUserRoleMapper.getMaxApprovalAmount
     * 跨库查询：db_permission
     *
     * @param userId 用户 ID
     * @return 最大审批金�
     */
    @Slave
    public BigDecimal getMaxApprovalAmount(UUID userId) {
        if (userId == null) {
            return BigDecimal.ZERO;
        }
        return userRoleMapper.getMaxApprovalAmount(userId);
    }

    /**
     * 检查用户是否拥有指定的临时角色
     * <p>
     * 替代 SysUserRoleMapper.hasTemporaryRole
     * 跨库查询：db_permission
     *
     * @param userId 用户 ID
     * @param roleId 角色 ID
     * @return 是否拥有该临时角�
     */
    @Slave
    public boolean hasTemporaryRole(UUID userId, UUID roleId) {
        if (userId == null || roleId == null) {
            return false;
        }
        return userRoleMapper.hasTemporaryRole(userId, roleId);
    }

    /**
     * 查询用户的临时角色列�
     * <p>
     * 替代 SysUserRoleMapper.findTemporaryRolesByUserId
     * 跨库查询：db_permission
     *
     * @param userId 用户 ID
     * @return 临时角色列表
     */
    @Slave
    @Cacheable(value = "userTemporaryRoles", key = "#userId", unless = "#result.isEmpty()")
    public List<Map<String, Object>> findTemporaryRolesByUserId(UUID userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return userRoleMapper.findTemporaryRolesByUserId(userId);
    }

    /**
     * 统计用户临时角色�
     * <p>
     * 替代 SysUserRoleMapper.countTemporaryRoles
     * 跨库查询：db_permission
     *
     * @param userId 用户 ID
     * @return 临时角色�
     */
    @Slave
    public Integer countTemporaryRoles(UUID userId) {
        return getCountOrDefault(userId, userRoleMapper::countTemporaryRoles);
    }

    /**
     * 统计用户即将过期的角色数
     * <p>
     * 替代 SysUserRoleMapper.countExpiringRoles
     * 跨库查询：db_permission
     *
     * @param userId 用户 ID
     * @param days   天数
     * @return 即将过期的角色数
     */
    @Slave
    public Integer countExpiringRoles(UUID userId, Integer days) {
        return getCountOrDefault(userId, uid -> userRoleMapper.countExpiringRoles(uid, days));
    }

    /**
     * 统计指定部门的用户数
     *
     * @param deptId 部门 ID
     * @return 用户数
     */
    @Slave
    public int countUsersByDeptId(UUID deptId) {
        if (deptId == null) {
            return 0;
        }
        Integer count = userMapper.countUsersByDeptId(deptId);
        return count != null ? count : 0;
    }

    /**
     * 批量统计指定部门的用户数
     *
     * @param deptIds 部门 ID 列表
     * @return 部门 ID -> 用户数 映射
     */
    @Slave
    public Map<UUID, Integer> countUsersByDeptIds(List<UUID> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<UUID, Map<String, Object>> raw = userMapper.countUsersByDeptIds(deptIds);
        if (raw == null) {
            return Collections.emptyMap();
        }
        Map<UUID, Integer> result = new HashMap<>();
        for (Map.Entry<UUID, Map<String, Object>> entry : raw.entrySet()) {
            Object count = entry.getValue().get("user_count");
            result.put(entry.getKey(), count instanceof Number ? ((Number) count).intValue() : 0);
        }
        return result;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 通用的计数查询方法，处理null检查和默认�
     *
     * @param userId 用户ID
     * @param countFunction 计数函数
     * @return 计数结果，null时返�
     */
    private Integer getCountOrDefault(UUID userId, java.util.function.Function<UUID, Integer> countFunction) {
        if (userId == null) {
            return 0;
        }
        Integer count = countFunction.apply(userId);
        return count != null ? count : 0;
    }

}

package com.scmcloud.system.service.query;

import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.common.dto.dept.DeptDTO;
import com.scmcloud.system.domain.entity.SysDept;
import com.scmcloud.system.domain.entity.SysUser;
import com.scmcloud.system.mapper.SysDeptMapper;
import com.scmcloud.system.mapper.SysUserMapper;
import com.scmcloud.system.mapper.SysUserRoleMapper;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 部门跨库查询服务
 * <p>
 * 处理与部门相关的跨库查询操作（db_org �db_user �db_permission�
 *
 * @author Deng
 * @since 2025-01-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeptCrossDatabaseQueryService {
    private final SysDeptMapper deptMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;

    /**
     * 查询部门树（包含负责人信息）
     * <p>
     * 替代�SysDeptMapper.selectDeptTree
     *
     * @return 部门 DTO 列表（包含负责人姓名�
     */
    @Slave
    @Timed(value = "cross_db_query", extraTags = {"method", "selectDeptTree"})
    public List<DeptDTO> selectDeptTree() {
        // 1. �org 库查询所有部�
        List<SysDept> depts = deptMapper.selectDeptList();
        if (depts == null || depts.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 收集所有负责人 ID
        Set<UUID> leaderIds = depts.stream()
                .map(SysDept::getLeaderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 3. �user 库批量查询负责人信息
        Map<UUID, String> leaderNameMap = new HashMap<>();
        if (!leaderIds.isEmpty()) {
            List<SysUser> leaders = userMapper.selectBasicInfoByIds(new ArrayList<>(leaderIds));
            leaderNameMap = leaders.stream()
                    .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName, (a, b) -> a));
        }

        // 4. 组装 DTO
        Map<UUID, String> finalLeaderNameMap = leaderNameMap;
        return depts.stream()
                .map(dept -> DeptDTO.builder()
                        .id(dept.getId())
                        .parentId(dept.getParentId())
                        .deptCode(dept.getDeptCode())
                        .deptName(dept.getDeptName())
                        .deptType(dept.getDeptType())
                        .leaderId(dept.getLeaderId())
                        .leaderName(dept.getLeaderId() != null ? finalLeaderNameMap.get(dept.getLeaderId()) : null)
                        .phone(dept.getPhone())
                        .email(dept.getEmail())
                        .isolationLevel(dept.getIsolationLevel())
                        .sortOrder(dept.getSortOrder())
                        .status(dept.getStatus())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 获取部门负责�ID
     * <p>
     * 替代 SysDeptMapper.getLeaderId
     * 跨库查询：db_org
     *
     * @param deptId 部门 ID
     * @return 负责�ID
     */
    @Slave
    public UUID getDeptLeaderId(UUID deptId) {
        if (deptId == null) {
            return null;
        }
        return deptMapper.getLeaderId(deptId);
    }

    /**
     * 查询用户的部门及其所有子部门 ID
     * <p>
     * 替代�SysUserMapper.findUserDeptAndChildren
     *
     * @param userId 用户 ID
     * @return 部门及子部门 ID 列表
     */
    @Slave
    @Timed(value = "cross_db_query", extraTags = {"method", "findUserDeptAndChildren"})
    public List<UUID> findUserDeptAndChildren(UUID userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        // 1. �user 库获取用户的部门 ID
        UUID deptId = userMapper.getUserDeptId(userId);
        if (deptId == null) {
            return Collections.emptyList();
        }

        // 2. �org 库递归查询部门及子部门
        return deptMapper.selectDeptAndChildren(deptId);
    }

    /**
     * 检查用户是否有权访问指定部�
     * <p>
     * 替代�SysUserMapper.hasAccessToDept
     *
     * @param userId 用户 ID
     * @param deptId 目标部门 ID
     * @return 是否有访问权�
     */
    @Slave
    @Timed(value = "cross_db_query", extraTags = {"method", "hasAccessToDept"})
    public boolean hasAccessToDept(UUID userId, UUID deptId) {
        if (userId == null || deptId == null) {
            return false;
        }

        // 1. 获取用户的数据权限范�
        Integer dataScope = userRoleMapper.getUserDataScope(userId);
        if (dataScope == null) {
            return false;
        }

        // 数据权限�-全部数据
        if (dataScope == 1) {
            return true;
        }

        // 2. 获取用户的部�ID
        UUID userDeptId = userMapper.getUserDeptId(userId);
        if (userDeptId == null) {
            return false;
        }

        // 数据权限�-本部�
        if (dataScope == 3) {
            return userDeptId.equals(deptId);
        }

        // 数据权限�-本部门及子部�
        if (dataScope == 4) {
            List<UUID> accessibleDepts = deptMapper.selectDeptAndChildren(userDeptId);
            return accessibleDepts.contains(deptId);
        }

        // 数据权限�-仅本人（不能访问其他部门�
        return false;
    }

    /**
     * 统计单个部门用户�
     * <p>
     * 替代 SysUserMapper.countUsersByDeptId
     * 跨库查询：db_user
     *
     * @param deptId 部门 ID
     * @return 用户�
     */
    @Slave
    public int countUsersByDeptId(UUID deptId) {
        if (deptId == null) {
            return 0;
        }
        return userMapper.countUsersByDeptId(deptId);
    }

    /**
     * 批量统计部门用户�
     * <p>
     * 替代 SysUserMapper.countUsersByDeptIds
     * 跨库查询：db_user
     *
     * @param deptIds 部门 ID 列表
     * @return 部门 ID �用户�映射
     */
    @Slave
    @Timed(value = "cross_db_query", extraTags = {"method", "countUsersByDeptIds"})
    public Map<UUID, Integer> countUsersByDeptIds(List<UUID> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<UUID, Map<String, Object>> countResult = userMapper.countUsersByDeptIds(deptIds);
        if (countResult == null) {
            return Collections.emptyMap();
        }

        Map<UUID, Integer> result = new HashMap<>();
        countResult.forEach((deptId, row) -> {
            Object count = row.get("user_count");
            result.put(deptId, count != null ? ((Number) count).intValue() : 0);
        });

        return result;
    }
}
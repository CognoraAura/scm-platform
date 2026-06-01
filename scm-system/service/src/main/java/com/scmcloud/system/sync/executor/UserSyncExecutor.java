package com.scmcloud.system.sync.executor;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.scmcloud.system.mapper.SysDeptMapper;
import com.scmcloud.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * 用户同步执行�?
 * <p>
 * 独立�?Bean，用于执行跨库事务操作�?
 * 避免 @Transactional 自调用问�?
 *
 * @author Deng
 * @since 2025-12-16
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserSyncExecutor {
    private final SysUserRoleMapper userRoleMapper;
    private final SysDeptMapper deptMapper;

    /**
     * 同步用户信息�?permission �?
     *
     * @param userId 用户 ID
     * @param data   用户数据
     */
    @DS("permission")
    @Transactional(rollbackFor = Exception.class)
    public void syncToPermissionDb(UUID userId, Map<String, Object> data) {
        String username = (String) data.get("username");
        String realName = (String) data.get("realName");
        Integer status = (Integer) data.get("status");

        int updated = userRoleMapper.updateUserRedundancy(userId, username, realName, status);
        log.debug("[UserSync] Updated {} rows in sys_user_role for user: {}", updated, userId);
    }

    /**
     * 同步用户信息�?org 库（负责人信息）
     *
     * @param userId 用户 ID
     * @param data   用户数据
     */
    @DS("org")
    @Transactional(rollbackFor = Exception.class)
    public void syncToOrgDb(UUID userId, Map<String, Object> data) {
        String realName = (String) data.get("realName");
        String phone = (String) data.get("phone");

        int updated = deptMapper.updateLeaderRedundancy(userId, realName, phone);
        log.debug("[UserSync] Updated {} rows in sys_dept for leader: {}", updated, userId);
    }

    /**
     * 标记用户�?permission 库中已删�?
     *
     * @param userId 用户 ID
     */
    @DS("permission")
    @Transactional(rollbackFor = Exception.class)
    public void markDeletedInPermissionDb(UUID userId) {
        userRoleMapper.updateUserStatus(userId, 0);
        log.debug("[UserSync] Marked user as deleted in permission db: {}", userId);
    }
}
package com.scmcloud.system.sync.executor;

import com.baomidou.dynamic.datasource.annotation.DS;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 角色同步执行�
 * <p>
 * 独立�Bean，用于执行跨库事务操作�
 * 避免 @Transactional 自调用问�
 *
 * @author Deng
 * @since 2025-12-16
 */
@Slf4j
@Component
public class RoleSyncExecutor {

    /**
     * 同步角色信息�approval �
     *
     * @param roleId   角色 ID
     * @param roleName 角色名称
     * @param roleCode 角色编码
     */
    @DS("approval")
    @Transactional(rollbackFor = Exception.class)
    public void syncToApprovalDb(UUID roleId, String roleName, String roleCode) {
        // 更新包含该角色的审批记录�role_names 数组
        log.debug("[RoleSync] Would update approval records for role: {}, name: {}", roleId, roleName);
    }

    /**
     * 标记角色�approval 库中已删�
     *
     * @param roleId 角色 ID
     */
    @DS("approval")
    @Transactional(rollbackFor = Exception.class)
    public void markRoleDeletedInApprovalDb(UUID roleId) {
        log.debug("[RoleSync] Would mark role as deleted in approval db: {}", roleId);
    }
}
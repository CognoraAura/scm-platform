package com.scmcloud.system.sync.handler;

import com.scmcloud.common.integration.sync.event.DataSyncEvent;
import com.scmcloud.common.integration.sync.handler.DataSyncHandler;
import com.scmcloud.common.integration.sync.reconciliation.DataReconciliationTask;
import com.scmcloud.system.domain.entity.SysUser;
import com.scmcloud.system.mapper.SysUserMapper;
import com.scmcloud.system.mapper.SysUserRoleMapper;
import com.scmcloud.system.sync.executor.UserSyncExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 鐢ㄦ埛鏁版嵁鍚屾澶勭悊锟?
 * <p>
 * 澶勭悊鐢ㄦ埛鏁版嵁鍙樻洿锛屽悓姝ユ洿鏂板啑浣欏瓧娈靛埌鍏朵粬搴擄細
 * - db_permission.sys_user_role (username, real_name, user_status)
 * - db_org.sys_dept (leader_name, leader_phone)
 *
 * @author Deng
 * @since 2025-12-16
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserSyncHandler implements DataSyncHandler, DataReconciliationTask.ReconcilableHandler {
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final UserSyncExecutor syncExecutor;

    @Override
    public String getAggregateType() {
        return "User";
    }

    @Override
    public void handle(DataSyncEvent event) throws DataSyncHandler.DataSyncException {
        UUID userId = UUID.fromString(event.getPrimaryId());
        Map<String, Object> data = event.getAfterData();

        log.debug("[UserSync] Handling event: userId={}, type={}", userId, event.getEventType());

        try {
            switch (event.getEventType()) {
                case INSERT, UPDATE -> syncUserInfo(userId, data);
                case DELETE -> syncExecutor.markDeletedInPermissionDb(userId);
                default -> log.warn("[UserSync] Unknown event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            throw new DataSyncHandler.DataSyncException("Failed to sync user: " + userId, e, true);
        }
    }

    @Override
    public void fullSync(String primaryId) {
        UUID userId = UUID.fromString(primaryId);
        SysUser user = userMapper.selectById(userId);
        if (user != null) {
            Map<String, Object> data = buildUserData(user);
            syncUserInfo(userId, data);
        }
    }

    /**
     * 閫氳繃鐙珛锟紹ean 鍚屾鐢ㄦ埛淇℃伅锛岀‘锟紷Transactional 锟紷DS 鐢熸晥
     */
    private void syncUserInfo(UUID userId, Map<String, Object> data) {
        syncExecutor.syncToPermissionDb(userId, data);
        syncExecutor.syncToOrgDb(userId, data);
    }

    private Map<String, Object> buildUserData(SysUser user) {
        Map<String, Object> data = new HashMap<>();
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("phone", user.getPhone());
        data.put("email", user.getEmail());
        data.put("status", user.getStatus());
        data.put("deptId", user.getDeptId());
        return data;
    }

    // ==================== 瀵硅处瀹炵幇 ====================

    @Override
    public DataReconciliationTask.ReconciliationReport reconcile(int batchSize, boolean autoFix) {
        log.info("[UserSync] Starting reconciliation, batchSize={}, autoFix={}", batchSize, autoFix);

        int totalChecked = 0;
        int inconsistentCount = 0;
        int fixedCount = 0;
        int failedCount = 0;

        // 1. 鑾峰彇鎵€鏈夋湁瑙掕壊鍏宠仈鐨勭敤锟絀D
        List<UUID> userIds = userRoleMapper.findAllDistinctUserIds();

        for (int i = 0; i < userIds.size(); i += batchSize) {
            List<UUID> batch = userIds.subList(i, Math.min(i + batchSize, userIds.size()));

            // 2. 锟絬ser 搴撹幏鍙栫敤鎴蜂俊锟?
            List<SysUser> users = userMapper.selectBasicInfoByIds(batch);
            Map<UUID, SysUser> userMap = users.stream()
                    .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));

            // 3. 妫€鏌ユ瘡涓敤鎴风殑鍐椾綑鏁版嵁
            for (UUID userId : batch) {
                totalChecked++;
                SysUser user = userMap.get(userId);

                if (user == null) {
                    // 鐢ㄦ埛宸插垹闄わ紝浣嗚鑹插叧鑱旇繕锟?
                    inconsistentCount++;
                    if (autoFix) {
                        try {
                            syncExecutor.markDeletedInPermissionDb(userId);
                            fixedCount++;
                        } catch (Exception e) {
                            failedCount++;
                            log.error("[UserSync] Failed to fix: userId={}", userId, e);
                        }
                    }
                } else if (autoFix) {
                    // 寮哄埗鍚屾纭繚鏁版嵁涓€锟?
                    try {
                        fullSync(userId.toString());
                        fixedCount++;
                    } catch (Exception e) {
                        failedCount++;
                    }
                }
            }
        }

        return new DataReconciliationTask.ReconciliationReport(
                totalChecked, inconsistentCount, fixedCount, failedCount);
    }
}

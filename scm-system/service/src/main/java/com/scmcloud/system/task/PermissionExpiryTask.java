package com.scmcloud.system.task;

import com.scmcloud.system.mapper.SysUserRoleMapper;
import com.scmcloud.system.notification.NotificationService;
import com.scmcloud.system.notification.model.NotificationChannel;
import com.scmcloud.system.notification.model.NotificationCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 鏉冮檺杩囨湡妫€鏌ュ畾鏃朵换锟?
 *
 * @author Deng
 * createData 2025/10/30 14:50
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PermissionExpiryTask {
    private final SysUserRoleMapper userRoleMapper;
    private final NotificationService notificationService;

    /**
     * 姣忓ぉ鍑屾櫒2鐐规鏌ュ苟澶勭悊杩囨湡鏉冮檺
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void checkExpiredPermissions() {
        log.info("Starting expired permissions check task");

        try {
            // 1. 鏌ヨ宸茶繃鏈熺殑瑙掕壊
            List<Map<String, Object>> expiredRoles = userRoleMapper.findExpiredRolesForCleanup();

            if (!expiredRoles.isEmpty()) {
                log.warn("Found {} expired roles", expiredRoles.size());

                // 璁板綍杩囨湡淇℃伅
                for (Map<String, Object> role : expiredRoles) {
                    log.info("Expired role: user={}, role={}, expireTime={}",
                            role.get("username"),
                            role.get("role_name"),
                            role.get("expire_time"));
                }

                // 2. 鏇存柊杩囨湡瑙掕壊鐘舵€侊紙涓嶇洿鎺ュ垹闄わ紝渚夸簬瀹¤锟?
                int updatedCount = userRoleMapper.updateExpiredRolesStatus();
                log.info("Updated {} expired role assignments", updatedCount);

                // 3. 鍙戦€佽繃鏈熼€氱煡锛圱ODO: 闆嗘垚閭欢/鐭俊鏈嶅姟锟?
                sendExpiryNotifications(expiredRoles);
            }

            log.info("Expired permissions check completed");

        } catch (Exception e) {
            log.error("Error during expired permissions check", e);
        }
    }

    /**
     * 姣忓ぉ涓婂崍9鐐规鏌ュ嵆灏嗚繃鏈熺殑鏉冮檺锛堟彁锟藉ぉ閫氱煡锟?
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void checkExpiringPermissions() {
        log.info("Starting expiring permissions check task");

        try {
            // 鏌ヨ7澶╁唴鍗冲皢杩囨湡鐨勮锟?
            List<Map<String, Object>> expiringRoles = userRoleMapper.findExpiringRolesForNotification(7);

            if (!expiringRoles.isEmpty()) {
                log.info("Found {} roles expiring in 7 days", expiringRoles.size());

                // 鍙戦€佸嵆灏嗚繃鏈熼€氱煡
                for (Map<String, Object> role : expiringRoles) {
                    log.info("Role expiring soon: user={}, role={}, expireTime={}",
                            role.get("username"),
                            role.get("role_name"),
                            role.get("expire_time"));

                    // TODO: 鍙戦€侀€氱煡閭欢
                    sendExpiringNotification(role);
                }
            }

            log.info("Expiring permissions check completed");

        } catch (Exception e) {
            log.error("Error during expiring permissions check", e);
        }
    }

    /**
     * 姣忓懆涓€鍑屾櫒3鐐规竻鐞嗚繃鏈熸潈闄愭暟鎹紙鍙€夛級
     * 濡傛灉涓嶉渶瑕佷繚鐣欒繃鏈熸暟鎹敤浜庡璁★紝鍙互鍚敤姝や换锟?
     */
    @Scheduled(cron = "0 0 3 ? * MON")
    public void cleanupExpiredPermissions() {
        log.info("Starting cleanup of expired permissions");

        try {
            // 鍒犻櫎杩囨湡瓒呰繃30澶╃殑瑙掕壊鍒嗛厤璁板綍
            int deletedCount = userRoleMapper.deleteExpiredRoles();
            log.info("Cleaned up {} expired role assignments", deletedCount);

        } catch (Exception e) {
            log.error("Error during expired permissions cleanup", e);
        }
    }

    /**
     * 鍙戦€佽繃鏈熼€氱煡
     * TODO: 闆嗘垚瀹為檯鐨勯€氱煡鏈嶅姟锛堥偖锟界煭淇?绔欏唴淇★級
     */
    private void sendExpiryNotifications(List<Map<String, Object>> expiredRoles) {
        for (Map<String, Object> role : expiredRoles) {
            try {
                String username = (String) role.get("username");
                String email = (String) role.get("email");
                String roleName = (String) role.get("role_name");

                log.info("Sending expiry notification to user: {}, role: {}", username, roleName);
                String subject = "Permission expired";
                String body = String.format("Hello %s, your assigned role %s has expired.", username, roleName);

                NotificationCommand command = NotificationCommand.builder()
                        .referenceId("permission-expired-" + roleName + "-" + username)
                        .username(username)
                        .email(email)
                        .subject(subject)
                        .content(body)
                        .templateCode("permission.expired")
                        .channel(NotificationChannel.EMAIL)
                        .channel(NotificationChannel.SYSTEM_MESSAGE)
                        .variable("username", username)
                        .variable("roleName", roleName)
                        .build();
                notificationService.send(command);
            } catch (Exception e) {
                log.error("Failed to send expiry notification", e);
            }
        }
    }

    /**
     * 鍙戦€佸嵆灏嗚繃鏈熼€氱煡
     */
    private void sendExpiringNotification(Map<String, Object> role) {
        try {
            String username = (String) role.get("username");
            String email = (String) role.get("email");
            String roleName = (String) role.get("role_name");
            Object expireTime = role.get("expire_time");

            log.info("Sending expiring notification to user: {}, role: {}, expireTime: {}",
                    username, roleName, expireTime);
            String subject = "Permission expiring soon";
            String message = String.format("Hello %s, your role %s will expire on %s. Please renew if needed.",
                    username, roleName, expireTime);

            NotificationCommand command = NotificationCommand.builder()
                    .referenceId("permission-expiring-" + roleName + "-" + username)
                    .username(username)
                    .email(email)
                    .subject(subject)
                    .content(message)
                    .templateCode("permission.expiring")
                    .channel(NotificationChannel.EMAIL)
                    .channel(NotificationChannel.SYSTEM_MESSAGE)
                    .variable("username", username)
                    .variable("roleName", roleName)
                    .variable("expireTime", expireTime)
                    .build();
            notificationService.send(command);

        } catch (Exception e) {
            log.error("Failed to send expiring notification", e);
        }
    }
}

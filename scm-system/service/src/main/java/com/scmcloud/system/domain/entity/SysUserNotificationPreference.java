package com.scmcloud.system.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * 用户通知偏好�?
 *
 * @author Deng
 * @since 2025-12-17
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sys_user_notification_preference")
public class SysUserNotificationPreference {

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private UUID id;

    @TableField("user_id")
    private UUID userId;

    @TableField("notification_type")
    private String notificationType;

    @TableField("channel")
    private String channel;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("quiet_hours_start")
    private LocalTime quietHoursStart;

    @TableField("quiet_hours_end")
    private LocalTime quietHoursEnd;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 通知类型枚举
     */
    @Getter
    public enum NotificationType {
        APPROVAL("APPROVAL", "审批通知"),
        SECURITY("SECURITY", "安全通知"),
        SYSTEM("SYSTEM", "系统通知"),
        MARKETING("MARKETING", "营销通知");

        private final String code;
        private final String desc;

        NotificationType(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }
    }

    /**
     * 通知渠道枚举
     */
    @Getter
    public enum Channel {
        EMAIL("EMAIL", "邮件"),
        SMS("SMS", "短信"),
        WECHAT("WECHAT", "微信"),
        DINGTALK("DINGTALK", "钉钉"),
        FEISHU("FEISHU", "飞书"),
        PUSH("PUSH", "推�?);
    
        private final String code;
        private final String desc;

        Channel(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }
    }

    /**
     * 判断当前时间是否在免打扰时段
     */
    public boolean isQuietHour() {
        if (quietHoursStart == null || quietHoursEnd == null) {
            return false;
        }
        LocalTime now = LocalTime.now();
        if (quietHoursStart.isBefore(quietHoursEnd)) {
            // 正常时段，例�?22:00 - 08:00
            return now.isAfter(quietHoursStart) && now.isBefore(quietHoursEnd);
        } else {
            // 跨午夜时段，例如 22:00 - 08:00（次日）
            return now.isAfter(quietHoursStart) || now.isBefore(quietHoursEnd);
        }
    }
}
package com.scmcloud.system.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 通知发送审计表
 *
 * @author Deng
 * @since 2025-12-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "sys_notification_audit", autoResultMap = true)
public class NotificationAuditLog {

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private UUID id;

    @TableField("reference_id")
    private String referenceId;

    @TableField("user_id")
    private UUID userId;

    @TableField("channel")
    private String channel;

    @TableField("status")
    private String status;

    @TableField("subject")
    private String subject;

    @TableField("username")
    private String username;

    @TableField("email")
    private String email;

    @TableField("phone")
    private String phone;

    @TableField("template_code")
    private String templateCode;

    @TableField("content")
    private String content;

    @TableField(value = "variables", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> variables;

    @TableField("error_message")
    private String errorMessage;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("max_retries")
    private Integer maxRetries;

    @TableField("next_retry_time")
    private LocalDateTime nextRetryTime;

    @TableField("sent_at")
    private LocalDateTime sentAt;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    // ==================== 冗余字段 ====================

    @TableField("real_name")
    private String realName;

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
     * 发送状态枚�?
     */
    @Getter
    public enum Status {
        PENDING("PENDING", "待发�?),
        SENT("SENT", "已发�?),
        FAILED("FAILED", "发送失�?),
        CANCELLED("CANCELLED", "已取�?);

        private final String code;
        private final String desc;

        Status(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }
    }
}
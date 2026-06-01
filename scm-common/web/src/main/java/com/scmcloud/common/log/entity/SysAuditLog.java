package com.scmcloud.common.log.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;

import java.io.Serial;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.util.UUID;

import lombok.*;
import lombok.experimental.Accessors;
/**
 * <p>
 * 操作审计日志�?
 * </p>
 *
 * @author author
 * @since 2025-10-14
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sys_audit_log")
@AllArgsConstructor
@NoArgsConstructor
public class SysAuditLog {

    @TableId(value = "id", type = IdType.AUTO)
    private UUID id;

    private UUID userId;

    private String username;

    private String realName;

    private UUID deptId;

    private String operationType;

    private String operationModule;

    private String operationDesc;

    private String requestUri;

    private String requestMethod;

    private String requestParams;

    private String responseData;

    private Integer responseStatus;

    private String ipAddress;

    private String location;

    private String userAgent;

    private String businessType;

    private String businessId;

    private String oldValue;

    private String newValue;

    private Integer riskLevel;

    private Integer status;

    private String errorMsg;

    private Integer executeTime;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

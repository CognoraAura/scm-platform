package com.scmcloud.system.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 鐢ㄦ埛瑙掕壊鍏宠仈锟? 鏀寔涓存椂瑙掕壊鎺堟潈
 *
 * @author Deng
 * @since 2025-12-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sys_user_role")
public class SysUserRole {

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private UUID id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private UUID tenantId;

    private UUID userId;

    private UUID roleId;

    @TableField("effective_time")
    private LocalDateTime effectiveTime;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    private Integer approvalStatus;

    private UUID approvedBy;

    private LocalDateTime approvedTime;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private UUID createBy;

    // ==================== 鍐椾綑瀛楁锛堟潵锟絛b_user.sys_user锟?==================

    @TableField("username")
    private String username;

    @TableField("real_name")
    private String realName;

    @TableField("user_status")
    private Integer userStatus;

    /**
     * 瀹℃壒鐘舵€佹灇锟?
     */
    @Getter
    public enum ApprovalStatus {
        PENDING(0, "Pending"),
        IN_PROGRESS(1, "In Progress"),
        APPROVED(2, "Approved"),
        REJECTED(3, "Rejected");

        private final int code;
        private final String desc;

        ApprovalStatus(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }
    }

    /**
     * 鍒ゆ柇鏄惁涓轰复鏃舵巿锟?
     */
    public boolean isTemporary() {
        return effectiveTime != null && expireTime != null;
    }

    /**
     * 鍒ゆ柇鏄惁鍦ㄦ湁鏁堟湡锟?
     */
    public boolean isEffective() {
        if (approvalStatus == null || approvalStatus != ApprovalStatus.APPROVED.getCode()) {
            return false;
        }
        if (!isTemporary()) {
            return true; // 姘镐箙鎺堟潈
        }
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(effectiveTime) && now.isBefore(expireTime);
    }
}

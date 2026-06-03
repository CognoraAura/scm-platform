package com.scmcloud.common.tenant;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;

/**
 * Base entity with mandatory tenant isolation.
 * All business entities should extend this class to ensure
 * tenant_id is auto-populated on INSERT via AuditMetaObjectHandler.
 */
@Data
public abstract class TenantAwareEntity implements Serializable {

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;
}

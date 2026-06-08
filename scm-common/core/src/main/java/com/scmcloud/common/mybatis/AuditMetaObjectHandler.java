package com.scmcloud.common.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.scmcloud.common.tenant.TenantContextHolder;
import com.scmcloud.common.util.UUIDv7Util;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * MyBatis-Plus 瀹¤瀛楁鑷姩濉厖澶勭悊锟?

 * 鑷姩濉厖瀛楁锟?
 * 1. id - UUIDv7锛圛NSERT鏃讹級
 * 2. tenant_id - 浠嶵hreadLocal鑾峰彇锛圛NSERT鏃讹級
 * 3. create_time - 褰撳墠鏃堕棿锛圛NSERT鏃讹級
 * 4. create_by - 褰撳墠鐢ㄦ埛ID锛圛NSERT鏃讹級
 * 5. update_time - 褰撳墠鏃堕棿锛圛NSERT鍜孶PDATE鏃讹級
 * 6. update_by - 褰撳墠鐢ㄦ埛ID锛圲PDATE鏃讹級
 * 7. deleted - false锛圛NSERT鏃讹級

 * 浣跨敤鏂瑰紡锟?
 * 瀹炰綋绫诲瓧娈垫坊锟紷TableField 娉ㄨВ锟?
 * <pre>
 * @TableField(fill = FieldFill.INSERT)
 * private UUID id;
 *
 * @TableField(fill = FieldFill.INSERT)
 * private UUID tenantId;
 *
 * @TableField(fill = FieldFill.INSERT)
 * private OffsetDateTime createTime;
 *
 * @TableField(fill = FieldFill.INSERT_UPDATE)
 * private OffsetDateTime updateTime;
 * </pre>
 *
 * @author Claude Code
 * @since 2025-01-24
 */
@Slf4j
@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

    /**
     * 鎻掑叆鏃惰嚜鍔ㄥ～锟?
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("Start insert fill ...");

        // 1. 鑷姩濉厖 id锛圲UIDv7锟?
        this.strictInsertFill(metaObject, "id", UUID.class, UUIDv7Util.generate());

        // 2. 鑷姩濉厖 tenant_id
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            // 妫€鏌ュ瓧娈电殑褰撳墠鍊肩被鍨?
            Object tenantIdValue = metaObject.getValue("tenantId");
            if (tenantIdValue instanceof UUID) {
                this.strictInsertFill(metaObject, "tenantId", UUID.class, tenantId);
            } else if (tenantIdValue instanceof String) {
                this.strictInsertFill(metaObject, "tenantId", String.class, tenantId.toString());
            } else {
                // 瀛楁鏈～鍏咃紝榛樿浣跨敤 String 绫诲瀷
                this.strictInsertFill(metaObject, "tenantId", String.class, tenantId.toString());
            }
        } else {
            log.warn("Tenant ID is null when inserting, entity: {}", metaObject.getOriginalObject().getClass().getName());
        }

        // 3. 鑷姩濉厖 create_time
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        this.strictInsertFill(metaObject, "createTime", OffsetDateTime.class, now);

        // 4. 鑷姩濉厖 create_by锛堥渶瑕佷粠SecurityContext鎴栧叾浠栧湴鏂硅幏鍙栧綋鍓嶇敤鎴凤級
        UUID currentUserId = getCurrentUserId();
        if (currentUserId != null) {
            this.strictInsertFill(metaObject, "createBy", UUID.class, currentUserId);
        }

        // 5. 鑷姩濉厖 update_time
        this.strictInsertFill(metaObject, "updateTime", OffsetDateTime.class, now);

        // 6. 鑷姩濉厖 update_by
        if (currentUserId != null) {
            this.strictInsertFill(metaObject, "updateBy", UUID.class, currentUserId);
        }

        // 7. 鑷姩濉厖 deleted锛堣蒋鍒犻櫎鏍囧織锟?
        this.strictInsertFill(metaObject, "deleted", Boolean.class, false);
    }

    /**
     * 鏇存柊鏃惰嚜鍔ㄥ～锟?
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("Start update fill ...");

        // 1. 鑷姩濉厖 update_time
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        this.strictUpdateFill(metaObject, "updateTime", OffsetDateTime.class, now);

        // 2. 鑷姩濉厖 update_by
        UUID currentUserId = getCurrentUserId();
        if (currentUserId != null) {
            this.strictUpdateFill(metaObject, "updateBy", UUID.class, currentUserId);
        }
    }

    /**
     * 鑾峰彇褰撳墠鐧诲綍鐢ㄦ埛ID
     * 锟絊ecurityContextHolder 涓幏鍙栬璇佷俊鎭紝鏀寔 SecurityUser锛堥€氳繃鍙嶅皠锛夊拰 Subject 鍥為€€
     */
    private UUID getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return null;
            }

            Object principal = authentication.getPrincipal();
            if (principal == null) {
                return null;
            }

            // SecurityUser.getUserId() via reflection (avoids circular dependency with scm-common-security-core)
            try {
                var method = principal.getClass().getMethod("getUserId");
                Object value = method.invoke(principal);
                if (value instanceof UUID uuid) {
                    return uuid;
                }
                if (value != null) {
                    return UUID.fromString(value.toString());
                }
            } catch (NoSuchMethodException ignored) {
                // principal does not have getUserId()
            } catch (Exception e) {
                log.debug("Failed to invoke getUserId() via reflection: {}", e.getMessage());
            }

            // Fallback: try subject as UUID (e.g. JWT subject claim)
            String name = authentication.getName();
            if (name != null && !name.isBlank() && !"anonymousUser".equals(name)) {
                try {
                    return UUID.fromString(name);
                } catch (IllegalArgumentException ignored) {
                    // name is not a UUID
                }
            }
        } catch (Exception e) {
            log.debug("Failed to get current user ID from SecurityContext: {}", e.getMessage());
        }
        return null;
    }
}
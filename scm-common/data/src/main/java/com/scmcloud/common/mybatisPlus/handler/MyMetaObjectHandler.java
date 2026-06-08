package com.scmcloud.common.mybatisPlus.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.scmcloud.common.security.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 瀛楁鑷姩濉厖澶勭悊锟?
 *
 * @author Deng
 * createData 2025/10/15 14:37
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class MyMetaObjectHandler implements MetaObjectHandler {
    private final SecurityContext securityContext;

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());

        UUID userId = securityContext.getCurrentUserId();
        if (userId != null) {
            Object createByValue = metaObject.getValue("createBy");
            if (createByValue instanceof String) {
                this.strictInsertFill(metaObject, "createBy", String.class, userId.toString());
            } else {
                this.strictInsertFill(metaObject, "createBy", UUID.class, userId);
            }
        }

        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());

        if (userId != null) {
            Object updateByValue = metaObject.getValue("updateBy");
            if (updateByValue instanceof String) {
                this.strictInsertFill(metaObject, "updateBy", String.class, userId.toString());
            } else {
                this.strictInsertFill(metaObject, "updateBy", UUID.class, userId);
            }
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());

        UUID userId = securityContext.getCurrentUserId();
        if (userId != null) {
            Object updateByValue = metaObject.getValue("updateBy");
            if (updateByValue instanceof String) {
                this.strictUpdateFill(metaObject, "updateBy", String.class, userId.toString());
            } else {
                this.strictUpdateFill(metaObject, "updateBy", UUID.class, userId);
            }
        }
    }
}

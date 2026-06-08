package com.scmcloud.common.security.annotation;

import java.lang.annotation.*;

/**
 *
 *
 * @author Deng
 * createData 2025/10/24 15:07
 * @version 1.0
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EncryptField {

    /**
     * 鍔犲瘑绠楁硶锛堥鐣欙紝褰撳墠浠呮敮鎸丄ES锟?
     */
    String algorithm() default "AES";
}

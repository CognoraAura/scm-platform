package com.scmcloud.common.security.idempotent;

import java.lang.annotation.*;

/**
 * 幂等性注�
 * 用于防止接口重复提交
 *
 * @author Deng
 * createData 2025/10/31 10:19
 * @version 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * 幂等�key的前缀
     */
    String prefix() default "idempotent:";

    /**
     * 幂等性key的SpEL表达�
     * 例如: #userId �#request.orderId
     */
    String key() default "";

    /**
     * 过期时间（秒�
     */
    int expireTime() default 300;

    /**
     * 提示信息
     */
    String message() default "请勿重复提交";

    /**
     * 幂等性类�
     */
    Type type() default Type.TOKEN;

    enum Type {
        /**
         * Token模式：客户端先获取token，提交时验证
         */
        TOKEN,

        /**
         * 参数模式：根据参数生成唯一key
         */
        PARAM,

        /**
         * 请求路径模式：根据请求路�用户ID
         */
        PATH
    }
}
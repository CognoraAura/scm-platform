package com.scmcloud.common.sentinel.annotation;

import java.lang.annotation.*;

/**
 * 接口限流注解
 *
 * @author Deng
 * createData 2025/10/20 11:00
 * @version 1.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 资源名称（默认使用方法签名）
     */
    String value() default "";

    /**
     * QPS 阈�?
     */
    int qps() default 100;

    /**
     * 限流类型�?-QPS 2-线程�?
     */
    int grade() default 1;

    /**
     * 是否开启集群限�?
     */
    boolean clusterMode() default false;
}

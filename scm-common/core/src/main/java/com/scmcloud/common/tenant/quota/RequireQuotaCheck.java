package com.scmcloud.common.tenant.quota;

import java.lang.annotation.*;

/**
 * 配额检查注�?

 * 使用示例�?
 * <pre>
 * @RequireQuotaCheck(quotaType = QuotaType.ORDERS, increment = 1)
 * public Order createOrder(OrderCreateDTO dto) {
 *     // 在方法执行前会自动检查租户的订单配额
 *     // 如果配额不足，抛�?QuotaExceededException
 * }
 * </pre>
 *
 * @author Claude Code
 * @since 2025-01-24
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireQuotaCheck {

    /**
     * 配额类型
     */
    QuotaType quotaType();

    /**
     * 消耗的配额数量（默�?�?
     */
    int increment() default 1;

    /**
     * 是否在方法成功后才消耗配额（默认false，即方法执行前就消耗）
     * 如果为true，需要配�?@AfterReturning 实现
     */
    boolean consumeAfterSuccess() default false;
}
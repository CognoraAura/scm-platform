package com.scmcloud.common.tenant;

import com.scmcloud.common.tenant.quota.QuotaService;
import com.scmcloud.common.tenant.quota.QuotaType;
import com.scmcloud.common.tenant.quota.RequireQuotaCheck;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * 配额检�?AOP 拦截�?

 * 使用方式�?
 * <pre>
 * @RequireQuotaCheck(quotaType = QuotaType.ORDERS, increment = 1)
 * public Order createOrder(OrderCreateDTO dto) {
 *     // ...
 * }
 * </pre>
 *
 * @author Claude Code
 * @since 2025-01-24
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class QuotaChecker {
    private final QuotaService quotaService;

    /**
     * 在方法执行前检查配�?
     */
    @Before("@annotation(com.frog.common.tenant.quota.RequireQuotaCheck)")
    public void checkQuota(JoinPoint joinPoint) {
        // 获取租户 ID
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            log.warn("Tenant ID is null, skipping quota check");
            return;
        }

        // 获取注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequireQuotaCheck annotation = method.getAnnotation(RequireQuotaCheck.class);

        if (annotation == null) {
            return;
        }

        QuotaType quotaType = annotation.quotaType();
        int increment = annotation.increment();

        log.debug("Checking quota for tenant={}, type={}, increment={}",
                tenantId, quotaType, increment);

        // 检查配�?
        boolean hasQuota = quotaService.checkAndConsumeQuota(tenantId, quotaType, increment);

        if (!hasQuota) {
            log.warn("Quota exceeded for tenant={}, type={}", tenantId, quotaType);
            throw new QuotaExceededException(
                    String.format("租户配额已用尽：%s，请升级套餐或联系客�?, quotaType.getDescription())
            );
        }

        log.debug("Quota check passed for tenant={}, type={}", tenantId, quotaType);
    }

    /**
     * 配额超限异常
     */
    public static class QuotaExceededException extends RuntimeException {
        public QuotaExceededException(String message) {
            super(message);
        }
    }
}
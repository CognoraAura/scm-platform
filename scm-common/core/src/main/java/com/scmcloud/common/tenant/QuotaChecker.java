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
 * 閰嶉妫€锟紸OP 鎷︽埅锟?

 * 浣跨敤鏂瑰紡锟?
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
     * 鍦ㄦ柟娉曟墽琛屽墠妫€鏌ラ厤锟?
     */
    @Before("@annotation(com.scmcloud.common.tenant.quota.RequireQuotaCheck)")
    public void checkQuota(JoinPoint joinPoint) {
        // 鑾峰彇绉熸埛 ID
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            log.warn("Tenant ID is null, skipping quota check");
            return;
        }

        // 鑾峰彇娉ㄨВ
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

        // 妫€鏌ラ厤锟?
        boolean hasQuota = quotaService.checkAndConsumeQuota(tenantId, quotaType, increment);

        if (!hasQuota) {
            log.warn("Quota exceeded for tenant={}, type={}", tenantId, quotaType);
            throw new QuotaExceededException(
                    String.format("绉熸埛閰嶉宸茬敤灏斤細%s锛岃鍗囩骇濂楅鎴栬仈绯诲鏈?, quotaType.getDescription())
            );
        }

        log.debug("Quota check passed for tenant={}, type={}", tenantId, quotaType);
    }

    /**
     * 閰嶉瓒呴檺寮傚父
     */
    public static class QuotaExceededException extends RuntimeException {
        public QuotaExceededException(String message) {
            super(message);
        }
    }
}
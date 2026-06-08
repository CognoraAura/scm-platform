package com.scmcloud.system.service.dubbo.adapter;

import com.scmcloud.common.security.PermissionService;
import com.scmcloud.system.api.PermissionDubboService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 鍩轰簬 Dubbo 锟絇ermissionService 鎺ュ彛瀹炵幇锟?
 *
 * <p>閲嶆瀯锛氬凡锟絚ommon/web 妯″潡杩佺Щ锟絪ystem/service 妯″潡锟?
 * 杩欑鍚堟纭殑鏋舵瀯鍒嗗眰鈥斺€斾笟鍔℃ā鍧楁彁渚涘疄鐜帮紝
 * 鍩虹璁炬柦妯″潡 (common/web) 渚濊禆浜庢帴鍙ｏ拷
 *
 * <p>瀹夊叏鎬э細瀹炵幇浜嗘晠闅滃叧闂ā寮忊€斺€斿湪鏈嶅姟澶辫触鏃舵姏鍑哄紓甯革紝
 * 浠ラ槻姝㈢敱浜庢潈闄愭鏌ュけ璐ヨ€屽鑷存湭缁忔巿鏉冪殑璁块棶锟?
 *
 * <p>鏋舵瀯浼樺娍锟?
 * - 閫氱敤妯″潡涓嶅啀渚濊禆浜庝笟鍔℃ā锟?
 * - 閬靛惊渚濊禆鍊掔疆鍘熷垯 (DIP)
 * - 鍏佽姣忎釜椤圭洰浣跨敤涓嶅悓锟絇ermissionService 瀹炵幇
 *
 * @author 閲嶆瀯锟紻ubboPermissionAccess
 * @version 2.0
 * @since 2025-12-12
 */
@Component
@Primary
@ConditionalOnClass(DubboReference.class)
@Slf4j
public class DubboPermissionServiceAdapter implements PermissionService {

    @DubboReference
    private PermissionDubboService permissionDubboService;

    private final MeterRegistry meterRegistry;

    public DubboPermissionServiceAdapter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Finds required permissions for a given URL and HTTP method via Dubbo.
     *
     * <p>SECURITY: Fail-closed - throws exception if permission lookup fails.
     * This prevents granting access when permission service is unavailable.
     *
     * @throws PermissionServiceException if permission lookup fails
     */
    @Override
    public List<String> findPermissionsByUrl(String url, String method) {
        try {
            List<String> permissions = permissionDubboService.findPermissionsByUrl(url, method);
            meterRegistry.counter("security.permissions.dubbo.lookup.success").increment();
            log.debug("Permission lookup success via Dubbo: url={}, method={}, permissions={}",
                     url, method, permissions);
            return permissions != null ? permissions : List.of();

        } catch (Exception ex) {
            meterRegistry.counter("security.permissions.dubbo.lookup.fail").increment();
            log.error("SECURITY: Permission lookup failed via Dubbo - DENYING ACCESS. " +
                     "url={}, method={}", url, method, ex);

            // FAIL-CLOSED: Throw exception to deny access when permission check fails
            throw new PermissionServiceException(
                "Permission service unavailable via Dubbo - access denied as safety measure", ex);
        }
    }

    /**
     * Finds all permissions for a given user via Dubbo.
     *
     * <p>SECURITY: Fail-closed - throws exception if permission lookup fails.
     *
     * @throws PermissionServiceException if permission lookup fails
     */
    @Override
    public Set<String> findAllPermissionsByUserId(UUID userId) {
        try {
            Set<String> perms = permissionDubboService.findAllPermissionsByUserId(userId);
            meterRegistry.counter("security.permissions.dubbo.user.success").increment();
            log.debug("User permission lookup success via Dubbo: userId={}, count={}",
                     userId, perms != null ? perms.size() : 0);
            return perms != null ? perms : Set.of();

        } catch (Exception ex) {
            meterRegistry.counter("security.permissions.dubbo.user.fail").increment();
            log.error("SECURITY: User permission lookup failed via Dubbo - DENYING ACCESS. " +
                     "userId={}", userId, ex);

            // FAIL-CLOSED: Throw exception to deny access when permission check fails
            throw new PermissionServiceException(
                "Permission service unavailable via Dubbo - access denied as safety measure", ex);
        }
    }
}
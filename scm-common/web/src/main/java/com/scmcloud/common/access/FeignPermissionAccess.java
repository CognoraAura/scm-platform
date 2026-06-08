package com.scmcloud.common.access;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.scmcloud.common.rest.client.SysPermissionServiceClient;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.common.security.PermissionService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 鍩轰簬 RestClient 锟絇ermissionService 鍥為€€瀹炵幇锟?
 *
 * <p>宸茶縼绉伙細锟紽eign 鏇存敼锟絉estClient + @HttpExchange锟?25-12-29锟?
 *
 * <p>宸查噸鏋勶細鐜板湪瀹炵幇 PermissionService 鎺ュ彛锛坈ommon/core锛夛紝
 * 鑰屼笉锟絇ermissionAccessPort銆傝繖瑙ｈ€︿簡 common/web 锟絪ystem/api锟?
 *
 * <p>瀹夊叏鎬э細浣跨敤 Sentinel 鏂矾鍣ㄥ疄鐜版晠闅滃叧闂ā锟?
 * - 浣跨敤 Sentinel SphU 杩涜鎵嬪姩璧勬簮淇濇姢
 * - 鏈嶅姟澶辫触鎴栨柇璺櫒鎵撳紑鏃舵姏鍑哄紓锟?
 * - 璺熻釜鎴愬姛/澶辫触鐜囨寚锟?
 *
 * <p>Sentinel 璧勬簮锟?
 * - "permission:findByUrl" - 锟経RL 鏌ユ壘鏉冮檺
 * - "permission:findByUserId" - 鎸夌敤锟絀D 鏌ユ壘鏉冮檺
 *
 * <p>锟紻ubbo 涓嶅彲鐢ㄦ椂锛屾瀹炵幇鐢ㄤ綔鍥為€€鏂规锟?
 * 浠呭綋涓嶅瓨鍦ㄥ叾锟絇ermissionService bean 鏃舵墠浼氬垱寤烘瀹炵幇锟?
 *
 * @author deng
 * @version 3.0
 * @since 2025-12-12
 */
@Component
@ConditionalOnMissingBean(PermissionService.class)
@Slf4j
public class FeignPermissionAccess implements PermissionService {
    private final SysPermissionServiceClient permissionServiceClient;
    private final MeterRegistry meterRegistry;

    public FeignPermissionAccess(SysPermissionServiceClient permissionServiceClient, MeterRegistry meterRegistry) {
        this.permissionServiceClient = permissionServiceClient;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Finds required permissions for a given URL and HTTP method via RestClient.
     *
     * <p>SECURITY: Fail-closed with Sentinel protection
     * - Throws exception if permission lookup fails
     * - Throws exception if Sentinel circuit is open
     *
     * @throws PermissionServiceException if permission lookup fails or circuit is open
     */
    @Override
    public List<String> findPermissionsByUrl(String url, String method) {
        try (Entry entry = SphU.entry("permission:findByUrl")) {
            List<String> permissions = permissionServiceClient.findPermissionsByUrl(url, method);
            meterRegistry.counter("security.permissions.lookup.success").increment();
            log.debug("Permission lookup success via RestClient: url={}, method={}, permissions={}",
                     url, method, permissions);
            return permissions != null ? permissions : List.of();

        } catch (BlockException ex) {
            // Sentinel circuit is open - deny access
            meterRegistry.counter("security.permissions.lookup.blocked").increment();
            log.error("SECURITY: Permission lookup BLOCKED by Sentinel - DENYING ACCESS. " +
                     "url={}, method={}", url, method, ex);
            throw new PermissionServiceException(
                "Permission service circuit open (rate limit/degraded) - access denied as safety measure", ex);

        } catch (Exception ex) {
            meterRegistry.counter("security.permissions.lookup.fail").increment();
            log.error("SECURITY: Permission lookup failed via RestClient - DENYING ACCESS. " +
                     "url={}, method={}", url, method, ex);

            // FAIL-CLOSED: Throw exception to deny access when permission check fails
            throw new PermissionServiceException(
                "Permission service unavailable via RestClient - access denied as safety measure", ex);
        }
    }

    /**
     * Finds all permissions for a given user via RestClient.
     *
     * <p>SECURITY: Fail-closed with Sentinel protection
     * - Throws exception if permission lookup fails
     * - Throws exception if Sentinel circuit is open
     *
     * @throws PermissionServiceException if permission lookup fails or circuit is open
     */
    @Override
    public Set<String> findAllPermissionsByUserId(UUID userId) {
        try (Entry entry = SphU.entry("permission:findByUserId")) {
            ApiResponse<Set<String>> resp = permissionServiceClient.getUserPermissions(userId);
            Set<String> perms = resp != null ? resp.data() : null;
            meterRegistry.counter("security.permissions.user.success").increment();
            log.debug("User permission lookup success via RestClient: userId={}, count={}",
                     userId, perms != null ? perms.size() : 0);
            return perms != null ? perms : Set.of();

        } catch (BlockException ex) {
            // Sentinel circuit is open - deny access
            meterRegistry.counter("security.permissions.user.blocked").increment();
            log.error("SECURITY: User permission lookup BLOCKED by Sentinel - DENYING ACCESS. " +
                     "userId={}", userId, ex);
            throw new PermissionServiceException(
                "Permission service circuit open (rate limit/degraded) - access denied as safety measure", ex);

        } catch (Exception ex) {
            meterRegistry.counter("security.permissions.user.fail").increment();
            log.error("SECURITY: User permission lookup failed via RestClient - DENYING ACCESS. " +
                     "userId={}", userId, ex);

            // FAIL-CLOSED: Throw exception to deny access when permission check fails
            throw new PermissionServiceException(
                "Permission service unavailable via RestClient - access denied as safety measure", ex);
        }
    }
}

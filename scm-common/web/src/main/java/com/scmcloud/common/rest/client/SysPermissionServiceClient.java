package com.scmcloud.common.rest.client;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.scmcloud.common.dto.permission.ApiPermissionDTO;
import com.scmcloud.common.dto.permission.PermissionDTO;
import com.scmcloud.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 鏉冮檺鏈嶅姟瀹㈡埛绔紙@HttpExchange 鐗堟湰锟?
 * <p>鏇夸唬 OpenFeign 锟絊ysPermissionServiceClient</p>
 *
 * <p>鏋舵瀯璇存槑锟?
 * <ul>
 *   <li>涓昏閫氫俊锛欴ubbo (PermissionDubboService) - 楂樻€ц兘 RPC</li>
 *   <li>闄嶇骇澶囩敤锛歊estClient + @HttpExchange (SysPermissionServiceClient) - HTTP REST</li>
 * </ul>
 *
 * <p>姝ゅ鎴风锟絪ystem-service 锟絊ysPermissionController 绔偣瀵瑰簲
 *
 * <p><strong>閲嶈瀹夊叏鐗规€э細Fail-Closed 妯″紡</strong>
 * <ul>
 *   <li>鏉冮檺鏌ヨ澶辫触鏃讹紝榛樿鎷掔粷璁块棶锛堣€屼笉鏄斁琛岋級</li>
 *   <li>{@code findPermissionsByUrl} 澶辫触鏃舵姏锟紸ccessDeniedException</li>
 *   <li>{@code getUserPermissions} 澶辫触鏃惰繑鍥炵┖闆嗗悎锛堟嫆缁濇墍鏈夋潈闄愶級</li>
 * </ul>
 *
 * @author Claude
 * @since 2025-12-29
 */
@HttpExchange("/api/system/permissions")
public interface SysPermissionServiceClient {

    /**
     * Logger for security-critical fallback methods
     * <p>Used for access control failures (Fail-Closed events):
     * <ul>
     *   <li>{@code getUserPermissionsFallback} - User permission lookup failure</li>
     *   <li>{@code findPermissionsByUrlFallback} - URL permission lookup failure</li>
     * </ul>
     */
    Logger log = LoggerFactory.getLogger(SysPermissionServiceClient.class);

    /**
     * 鏌ヨ鏉冮檺锟?
     * <p>瀵瑰簲: SysPermissionController.tree()</p>
     *
     * @return 鏉冮檺锟?
     */
    @GetExchange("/tree")
    @SentinelResource(
        value = "permission-service:getTree",
        fallback = "getPermissionTreeFallback"
    )
    ApiResponse<List<PermissionDTO>> getPermissionTree();

    /**
     * 鏌ヨ鏉冮檺鏍戠殑闄嶇骇鏂规硶
     * <p>Note: Sentinel Dashboard 浼氳褰曢檷绾т簨锟?p>
     *
     * @param ex 寮傚父
     * @return 绌哄垪锟?
     */
    default ApiResponse<List<PermissionDTO>> getPermissionTreeFallback(Throwable ex) {
        return ApiResponse.success(new ArrayList<>());
    }

    /**
     * 鏌ヨ鐢ㄦ埛鏉冮檺锛堢敤锟紽eignPermissionAccess锟?
     * <p>瀵瑰簲: SysPermissionController.getUserPermissions()</p>
     * <p>Dubbo: PermissionDubboService.findAllPermissionsByUserId()</p>
     *
     * <p><strong>SECURITY: Fail-Closed 妯″紡</strong> - 澶辫触鏃惰繑鍥炵┖闆嗗悎锛堟嫆缁濇墍鏈夋潈闄愶級</p>
     *
     * @param userId 鐢ㄦ埛 ID
     * @return 鐢ㄦ埛鏉冮檺闆嗗悎
     */
    @GetExchange("/user/{userId}")
    @SentinelResource(
        value = "permission-service:getUserPermissions",
        fallback = "getUserPermissionsFallback"
    )
    ApiResponse<Set<String>> getUserPermissions(@PathVariable UUID userId);

    /**
     * 鏌ヨ鐢ㄦ埛鏉冮檺鐨勯檷绾ф柟锟?
     * <p><strong>SECURITY: Fail-Closed</strong> - 杩斿洖绌洪泦鍚堬紝鎷掔粷鎵€鏈夋潈锟?p>
     *
     * @param userId 鐢ㄦ埛 ID
     * @param ex 寮傚父
     * @return 绌烘潈闄愰泦锟?
     */
    default ApiResponse<Set<String>> getUserPermissionsFallback(UUID userId, Throwable ex) {
        log.error("SECURITY ALERT: Permission lookup failed for userId={} - DENYING ALL ACCESS. Error: {}",
                  userId, ex.getMessage());
        // Fail-Closed: 鏉冮檺鏌ヨ澶辫触鏃惰繑鍥炵┖闆嗗悎锛堟嫆缁濇墍鏈夋潈闄愶級
        return ApiResponse.success(Collections.emptySet());
    }

    /**
     * 鏍规嵁 ID 鑾峰彇鏉冮檺璇︽儏
     * <p>瀵瑰簲: SysPermissionController.getById()</p>
     *
     * @param id 鏉冮檺 ID
     * @return 鏉冮檺璇︽儏
     */
    @GetExchange("/{id}")
    @SentinelResource(
        value = "permission-service:getById",
        fallback = "getPermissionByIdFallback"
    )
    ApiResponse<PermissionDTO> getPermissionById(@PathVariable UUID id);

    /**
     * 鏍规嵁 ID 鑾峰彇鏉冮檺鐨勯檷绾ф柟锟?
     * <p>Note: Sentinel Dashboard 浼氳褰曢檷绾т簨锟?p>
     *
     * @param id 鏉冮檺 ID
     * @param ex 寮傚父
     * @return 澶辫触鍝嶅簲
     */
    default ApiResponse<PermissionDTO> getPermissionByIdFallback(UUID id, Throwable ex) {
        return ApiResponse.fail(503, "鏉冮檺鏈嶅姟鏆傛椂涓嶅彲鐢?);
    }

    /**
     * 鏍规嵁 URL 锟紿TTP 鏂规硶鏌ヨ鏉冮檺锛堢敤锟紽eignPermissionAccess锟?
     * <p>瀵瑰簲: SysPermissionController.findPermissionsByUrl()</p>
     * <p>Dubbo: PermissionDubboService.findPermissionsByUrl()</p>
     *
     * <p><strong>SECURITY: Fail-Closed 妯″紡</strong> - 澶辫触鏃舵姏锟紸ccessDeniedException锛堟嫆缁濊闂級</p>
     *
     * @param url URL 璺緞
     * @param method HTTP 鏂规硶
     * @return 鏉冮檺缂栫爜鍒楄〃
     */
    @GetExchange("/find-by-url")
    @SentinelResource(
        value = "permission-service:findByUrl",
        fallback = "findPermissionsByUrlFallback"
    )
    List<String> findPermissionsByUrl(
        @RequestParam("url") String url,
        @RequestParam("method") String method
    );

    /**
     * 鏍规嵁 URL 鏌ヨ鏉冮檺鐨勯檷绾ф柟锟?
     * <p><strong>SECURITY: Fail-Closed</strong> - 鎶涘嚭寮傚父锛屾嫆缁濊锟?p>
     *
     * @param url URL 璺緞
     * @param method HTTP 鏂规硶
     * @param ex 寮傚父
     * @throws AccessDeniedException 濮嬬粓鎶涘嚭锛團ail-Closed 绛栫暐锟?
     */
    default List<String> findPermissionsByUrlFallback(
        String url,
        String method,
        Throwable ex) {

        log.error("SECURITY ALERT: Permission lookup by URL failed - DENYING ACCESS. " +
                  "url={}, method={}, error={}",
                  url, method, ex.getMessage());

        // Fail-Closed: 鏉冮檺鏌ヨ澶辫触鏃跺繀椤绘嫆缁濊闂紙瀹夊叏绗竴锟?
        throw new AccessDeniedException(
            "Permission service unavailable (Sentinel circuit open or error) - access denied as safety measure",
            ex
        );
    }

    /**
     * 鏌ヨ鎵€锟紸PI 鏉冮檺锛堢敤锟紻ynamicPermissionLoader锟?
     * <p>瀵瑰簲: SysPermissionController.findApiPermissions()</p>
     *
     * @return API 鏉冮檺鍒楄〃锛屽寘鍚矾寰勩€丠TTP 鏂规硶鍜屾潈闄愮紪锟?
     */
    @GetExchange("/api")
    @SentinelResource(
        value = "permission-service:getApiPermissions",
        fallback = "findApiPermissionsFallback"
    )
    List<ApiPermissionDTO> findApiPermissions();

    /**
     * 鏌ヨ API 鏉冮檺鐨勯檷绾ф柟锟?
     * <p>Note: Sentinel Dashboard 浼氳褰曢檷绾т簨锟?p>
     *
     * @param ex 寮傚父
     * @return 绌哄垪锟?
     */
    default List<ApiPermissionDTO> findApiPermissionsFallback(Throwable ex) {
        return Collections.emptyList();
    }
}

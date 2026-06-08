package com.scmcloud.common.tenant;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * 绉熸埛杩囨护锟?
 * 浠庤姹傚ご涓彁鍙栫鎴稩D锛岃缃埌 ThreadLocal

 * 鏀寔鐨勭鎴稩D鏉ユ簮锛堜紭鍏堢骇浠庨珮鍒颁綆锛夛細
 * 1. HTTP Header: X-Tenant-Id
 * 2. HTTP Header: Tenant-Id
 * 3. Request Parameter: tenantId
 * 4. JWT Token 涓殑 tenant_id claim锛堥渶閰嶅悎JWT瑙ｆ瀽锟?
 *
 * @author Claude Code
 * @since 2025-01-24
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TenantFilter implements Filter {
    private static final String HEADER_TENANT_ID = "X-Tenant-Id";
    private static final String HEADER_TENANT_ID_ALT = "Tenant-Id";
    private static final String PARAM_TENANT_ID = "tenantId";
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int MAX_TOKEN_PAYLOAD_SIZE = 4096;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        try {
            // 鎻愬彇绉熸埛ID
            UUID tenantId = extractTenantId(httpRequest);

            if (tenantId != null) {
                TenantContextHolder.setTenantId(tenantId);
                log.debug("Tenant filter set tenant ID: {} for request: {}",
                        tenantId, httpRequest.getRequestURI());
            } else {
                log.warn("No tenant ID found in request: {}", httpRequest.getRequestURI());
                // 鍙互閫夋嫨鎶涘紓甯告垨鍏佽缁х画锛堟牴鎹笟鍔￠渶姹傦級
                // throw new TenantContextHolder.TenantNotFoundException("Tenant ID is required");
            }

            // 缁х画鎵ц
            chain.doFilter(request, response);
        } finally {
            // 娓呯悊 ThreadLocal锛岄伩鍏嶅唴瀛樻硠锟?
            TenantContextHolder.clear();
        }
    }

    /**
     * 浠庤姹備腑鎻愬彇绉熸埛ID

     * 浼樺厛绾э細
     * 1. X-Tenant-Id header
     * 2. Tenant-Id header
     * 3. tenantId parameter
     * 4. JWT token锛堝鏋滃凡閰嶇疆锟?
     */
    private UUID extractTenantId(HttpServletRequest request) {
        // 1. 锟絏-Tenant-Id header
        String tenantIdStr = request.getHeader(HEADER_TENANT_ID);

        // 2. 锟絋enant-Id header
        if (tenantIdStr == null || tenantIdStr.trim().isEmpty()) {
            tenantIdStr = request.getHeader(HEADER_TENANT_ID_ALT);
        }

        // 3. 浠庤姹傚弬锟?
        if (tenantIdStr == null || tenantIdStr.trim().isEmpty()) {
            tenantIdStr = request.getParameter(PARAM_TENANT_ID);
        }

        // 4. 锟絁WT Token 涓彁锟?
        if (tenantIdStr == null || tenantIdStr.trim().isEmpty()) {
            tenantIdStr = extractFromJwtToken(request);
        }

        // 杞崲锟経UID
        if (tenantIdStr != null && !tenantIdStr.trim().isEmpty()) {
            try {
                return UUID.fromString(tenantIdStr.trim());
            } catch (IllegalArgumentException e) {
                log.error("Invalid tenant ID format: {}", tenantIdStr, e);
                throw new IllegalArgumentException("Invalid tenant ID format: " + tenantIdStr);
            }
        }

        return null;
    }

    /**
     * 浠嶫WT Token涓彁鍙栫鎴稩D
     * 杞婚噺绾у疄鐜帮細Base64瑙ｇ爜JWT payload锛屾彁锟絫enant_id claim
     * 涓嶅仛绛惧悕楠岃瘉锛堢鍚嶉獙璇佺敱 JwtAuthenticationFilter 璐熻矗锟?
     */
    private String extractFromJwtToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HEADER_AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            return null;
        }

        try {
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));

            if (payloadJson.length() > MAX_TOKEN_PAYLOAD_SIZE) {
                log.warn("JWT payload exceeds max size, skipping tenant extraction");
                return null;
            }

            Map<String, Object> claims = objectMapper.readValue(payloadJson,
                    new TypeReference<Map<String, Object>>() {});

            Object tenantId = claims.get("tenant_id");
            if (tenantId != null) {
                return tenantId.toString();
            }
        } catch (Exception e) {
            log.debug("Failed to extract tenant_id from JWT: {}", e.getMessage());
        }

        return null;
    }
}
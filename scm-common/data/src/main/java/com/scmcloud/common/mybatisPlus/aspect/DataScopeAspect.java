package com.scmcloud.common.mybatisPlus.aspect;

import com.scmcloud.common.mybatisPlus.annotation.DataScope;
import com.scmcloud.common.mybatisPlus.context.DataScopeContextHolder;
import com.scmcloud.common.mybatisPlus.context.DataScopeFilter;
import com.scmcloud.common.mybatisPlus.service.DataPermissionService;
import com.scmcloud.common.security.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 鏁版嵁鏉冮檺鍒囬潰
 * 鏍规嵁鐢ㄦ埛瑙掕壊鐨刣ataScope鑷姩娉ㄥ叆SQL杩囨护鏉′欢
 *
 * <p>REFACTORED: Now depends on SecurityContext interface instead of concrete SecurityUser class.
 * This follows Dependency Inversion Principle (DIP) and decouples data layer from web layer.
 *
 * @author Deng
 * createData 2025/10/30 11:15
 * @version 2.0 - Refactored to use SecurityContext interface
 */
@Aspect
@Component
@Slf4j
public class DataScopeAspect {

    private final SecurityContext securityContext;
    private final DataPermissionService dataPermissionService;

    /**
     * Constructor injection of SecurityContext and DataPermissionService.
     *
     * @param securityContext Security context interface (implementation provided by web layer)
     * @param dataPermissionService Data permission service for custom rules
     */
    public DataScopeAspect(SecurityContext securityContext, DataPermissionService dataPermissionService) {
        this.securityContext = securityContext;
        this.dataPermissionService = dataPermissionService;
    }

    /**
     * 鎷︽埅甯︽湁@DataScope娉ㄨВ鐨勬柟锟?
     */
    @Around("@annotation(dataScope)")
    public Object around(ProceedingJoinPoint point, DataScope dataScope) throws Throwable {
        try {
            // Check if user is authenticated via interface
            if (!securityContext.isAuthenticated()) {
                log.debug("User not authenticated, skipping data scope filtering");
                return point.proceed();
            }

            // Get user info via interface (no direct dependency on SecurityUser)
            UUID userId = securityContext.getCurrentUserId();
            UUID deptId = securityContext.getCurrentDeptId();
            Integer dataScopeLevel = securityContext.getDataScopeLevel();

            // Null check for userId (required for data scope)
            if (userId == null) {
                log.warn("Authenticated user has null userId, skipping data scope");
                return point.proceed();
            }

            // Build SQL filter based on data scope level
            DataScopeFilter filter = buildSqlFilter(dataScopeLevel, userId, deptId, dataScope);

            // 璁剧疆鍒癟hreadLocal锛岀敱DataScopeInterceptor浣跨敤
            DataScopeContextHolder.set(filter);

            log.debug("Data scope applied: userId={}, level={}, filter={}",
                    userId, dataScopeLevel, filter.getClause());

            return point.proceed();
        } finally {
            // 娓呯悊 ThreadLocal
            DataScopeContextHolder.clear();
        }
    }

    /**
     * Builds SQL filter clause for data scope.
     * Adapted for PostgreSQL native UUID type.
     * SECURITY: Validates table aliases to prevent SQL injection through annotation parameters.
     */
    private DataScopeFilter buildSqlFilter(Integer dataScope, UUID userId, UUID deptId, DataScope annotation) {
        String deptAlias = validateSqlIdentifier(annotation.deptAlias(), "dept_id");
        String userAlias = validateSqlIdentifier(annotation.userAlias(), "create_by");

        return switch (dataScope) {
            case 1 -> // 鍏ㄩ儴鏁版嵁鏉冮檺
                    new DataScopeFilter("1=1", java.util.Collections.emptyMap());

            case 2 -> // 鑷畾涔夋暟鎹潈闄愶紙浠庢暟鎹簱鏌ヨ閰嶇疆锟?
                    buildCustomDataScope(userId, deptAlias, userAlias);

            case 3 -> // 鏈儴闂ㄦ暟鎹潈锟?PostgreSQL UUID)
                    deptId != null
                            ? new DataScopeFilter(
                                    deptAlias + " = #{__ds_deptId}::uuid",
                                    java.util.Map.of("__ds_deptId", deptId.toString()))
                            : new DataScopeFilter("1=0", java.util.Collections.emptyMap());

            case 4 -> // 鏈儴闂ㄥ強浠ヤ笅鏁版嵁鏉冮檺
                    deptId != null
                            ? buildDeptAndChildrenScope(deptId, deptAlias)
                            : new DataScopeFilter("1=0", java.util.Collections.emptyMap());

            case 5 -> // 浠呮湰浜烘暟鎹潈锟?PostgreSQL UUID)
                    new DataScopeFilter(
                            userAlias + " = #{__ds_userId}::uuid",
                            java.util.Map.of("__ds_userId", userId.toString()));

            default ->
                    new DataScopeFilter("1=0", java.util.Collections.emptyMap()); // 鏃犳潈锟?
        };
    }

    /**
     * 鏋勫缓鑷畾涔夋暟鎹潈锟?
     * 锟絪ys_role_dept 琛ㄦ煡璇㈢敤鎴风殑鑷畾涔夋潈闄愯锟?
     */
    private DataScopeFilter buildCustomDataScope(UUID userId, String deptAlias, String userAlias) {
        // 鏌ヨ鐢ㄦ埛鐨勮嚜瀹氫箟鏁版嵁鏉冮檺閮ㄩ棬鍒楄〃
        List<UUID> customDepts = dataPermissionService.findCustomDeptPermissions(userId);

        if (customDepts == null || customDepts.isEmpty()) {
            // 娌℃湁鑷畾涔夋潈闄愰厤缃紝闄嶇骇涓轰粎鏈汉
            log.debug("No custom data permission found for user {}, fallback to self only", userId);
            return new DataScopeFilter(
                    userAlias + " = #{__ds_userId}::uuid",
                    java.util.Map.of("__ds_userId", userId.toString())
            );
        }

        // 鏋勫缓 IN 瀛愬彞 (PostgreSQL UUID 鏁扮粍)
        Map<String, Object> params = new HashMap<>();
        params.put("__ds_userId", userId.toString());

        // 浣跨敤 PostgreSQL 锟紸NY 璇硶閰嶅悎鏁扮粍锛屾洿楂樻晥
        String deptList = customDepts.stream()
                .map(UUID::toString)
                .map(s -> "'" + s + "'::uuid")
                .collect(Collectors.joining(","));

        // 缁勫悎鏉′欢锛氶儴闂ㄥ湪鑷畾涔夊垪琛ㄤ腑 OR 鏈汉鍒涘缓鐨勬暟锟?
        String clause = String.format("(%s IN (%s) OR %s = #{__ds_userId}::uuid)",
                deptAlias, deptList, userAlias);

        log.debug("Custom data scope for user {}: {} depts", userId, customDepts.size());
        return new DataScopeFilter(clause, params);
    }

    /**
     * 鏋勫缓閮ㄩ棬鍙婂瓙閮ㄩ棬鏉冮檺
     * 浣跨敤 PostgreSQL 閫掑綊 CTE 鏌ヨ閮ㄩ棬锟?
     */
    private DataScopeFilter buildDeptAndChildrenScope(UUID deptId, String deptAlias) {
        // 浣跨敤閫掑綊CTE鏌ヨ鎵€鏈夊瓙閮ㄩ棬 (PostgreSQL 鍘熺敓 UUID)
        String clause = """
                %s IN (
                    WITH RECURSIVE dept_tree AS (
                        SELECT id FROM sys_dept WHERE id = #{__ds_deptId}::uuid AND NOT deleted
                        UNION ALL
                        SELECT d.id FROM sys_dept d
                        INNER JOIN dept_tree dt ON d.parent_id = dt.id
                        WHERE NOT d.deleted
                    )
                    SELECT id FROM dept_tree
                )
                """.formatted(deptAlias);
        return new DataScopeFilter(clause, java.util.Map.of("__ds_deptId", deptId.toString()));
    }

    /**
     * Validates SQL identifier (table/column alias) to prevent SQL injection.
     * Only allows alphanumeric characters, underscore, and dot (for qualified names).
     *
     * @param identifier The identifier from @DataScope annotation
     * @param defaultValue Default value if identifier is invalid
     * @return Validated identifier or default
     * @throws IllegalArgumentException if identifier contains dangerous characters
     */
    private String validateSqlIdentifier(String identifier, String defaultValue) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return defaultValue;
        }

        // SQL identifiers should only contain: a-z, A-Z, 0-9, underscore, dot
        // Dot allows qualified names like "table.column"
        if (!identifier.matches("^[a-zA-Z0-9_.]+$")) {
            log.error("SECURITY: Invalid SQL identifier in @DataScope annotation: '{}'. " +
                     "Only alphanumeric, underscore, and dot allowed.", identifier);
            throw new IllegalArgumentException(
                "Invalid table/column alias in @DataScope: " + identifier);
        }

        // Additional safety: reject SQL keywords commonly used in attacks
        String lower = identifier.toLowerCase(Locale.ROOT);
        String[] forbiddenKeywords = {
            "select", "from", "where", "union", "insert", "update", "delete",
            "drop", "create", "alter", "exec", "execute", "or", "and"
        };

        for (String keyword : forbiddenKeywords) {
            if (lower.equals(keyword)) {
                log.error("SECURITY: SQL keyword used as identifier in @DataScope: '{}'", identifier);
                throw new IllegalArgumentException(
                    "SQL keyword cannot be used as alias in @DataScope: " + identifier);
            }
        }

        return identifier;
    }
}

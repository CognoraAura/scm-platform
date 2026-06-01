package com.scmcloud.common.security.util;

import com.scmcloud.common.web.domain.SecurityUser;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 过滤器旁路检查工具类
 * <p>提供公共的白名单匹配和旁路判断逻辑，供多个安全过滤器复�?
 *
 * @author Deng
 * @version 1.0
 */
public final class FilterBypassHelper {
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private FilterBypassHelper() {
    }

    /**
     * 检�?URI是否匹配任意模式
     *
     * @param uri      请求 URI
     * @param patterns 匹配模式列表（支持Ant风格�?
     * @return 是否匹配
     */
    public static boolean matchesAny(String uri, List<String> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }
        return patterns.stream()
                .anyMatch(p -> PATH_MATCHER.match(p, uri) || uri.startsWith(p));
    }

    /**
     * 检查是否应该旁路过滤器
     *
     * @param uri               请求 URI
     * @param user              当前用户
     * @param bypassPaths       旁路路径列表
     * @param bypassUsers       旁路用户列表
     * @param bypassRoles       旁路角色列表
     * @param bypassPermissions 旁路权限列表
     * @return 是否应该旁路
     */
    public static boolean shouldBypass(String uri, SecurityUser user, List<String> bypassPaths, List<String> bypassUsers,
                                       List<String> bypassRoles, List<String> bypassPermissions) {
        // 1. 检查路径匹�?
        if (matchesAny(uri, bypassPaths)) {
            return true;
        }

        if (user == null) {
            return false;
        }

        // 2. 检查用户名匹配
        if (bypassUsers != null && bypassUsers.stream()
                .anyMatch(u -> Objects.equals(u, user.getUsername()))) {
            return true;
        }

        // 3. 检查角色匹�?
        Set<String> roles = user.getRoles();
        if (roles != null && bypassRoles != null &&
                roles.stream().anyMatch(bypassRoles::contains)) {
            return true;
        }

        // 4. 检查权限匹�?
        Set<String> permissions = user.getPermissions();
        return permissions != null && bypassPermissions != null &&
                permissions.stream()
                        .anyMatch(bypassPermissions::contains);
    }
}
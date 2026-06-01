package com.scmcloud.common.security.loader;

import com.scmcloud.common.dto.permission.ApiPermissionDTO;
import com.scmcloud.common.rest.client.SysPermissionServiceClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 动态权限加载服�?
 * 支持权限热更新，无需重启应用
 *
 * @author Deng
 * createData 2025/11/7 10:18
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DynamicPermissionLoader {
    private final SysPermissionServiceClient permissionServiceClient;
    private final CacheManager cacheManager;
    private final ApplicationEventPublisher eventPublisher;

    // 内存缓存：URL -> 所需权限列表
    private final Map<String, Set<String>> urlPermissionCache = new ConcurrentHashMap<>();

    // 权限版本号（用于检测变更）
    private final AtomicLong permissionVersion = new AtomicLong(0L);

    private static final String PERMISSION_CACHE_NAME = "permissionMapping";
    private static final String PERM_MAPPING_CACHE_KEY = "dynamic:permission:mapping";

    @PostConstruct
    public void initFromCache() {
        try {
            Cache permCache = cacheManager.getCache(PERMISSION_CACHE_NAME);
            if (permCache != null) {
                Object rawCached = permCache.get(PERM_MAPPING_CACHE_KEY, Map.class);
                if (rawCached instanceof Map<?, ?> rawMap && !rawMap.isEmpty()) {
                    urlPermissionCache.clear();
                    urlPermissionCache.putAll(normalizePermissionMap(rawMap));

                    // 初始化版本号
                    permissionVersion.set(1L);
                    log.info("Initialized dynamic permission cache from TwoLevelCache, size={}",
                            urlPermissionCache.size());
                }
            }
        } catch (Exception e) {
            log.warn("Init from cache failed", e);
        }
    }

    /**
     * 将原始缓存数据转换为类型安全的权限映�?
     *
     * @param rawMap 从缓存获取的原始 Map
     * @return 类型安全的权限映�?(URL -> 权限集合)
     */
    private static Map<String, Set<String>> normalizePermissionMap(Map<?, ?> rawMap) {
        Map<String, Set<String>> normalized = new HashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() instanceof String key && entry.getValue() instanceof Collection<?> values) {
                Set<String> permSet = new HashSet<>();
                for (Object val : values) {
                    if (val instanceof String strVal) {
                        permSet.add(strVal);
                    }
                }
                normalized.put(key, permSet);
            }
        }
        return normalized;
    }

    /**
     * 初始化加载权限配�?
     */
    public void loadPermissions() {
        log.info("Loading dynamic permissions...");

        try {
            // 查询所�?API类型的权�?
            List<ApiPermissionDTO> apiPermissions = permissionServiceClient.findApiPermissions();

            Map<String, Set<String>> newCache = new HashMap<>();

            for (ApiPermissionDTO perm : apiPermissions) {
                String apiPath = perm.getApiPath();
                String httpMethod = perm.getHttpMethod();
                String permissionCode = perm.getPermissionCode();

                if (apiPath != null && permissionCode != null) {
                    String key = buildKey(httpMethod, apiPath);
                    newCache.computeIfAbsent(key, k -> new HashSet<>())
                            .add(permissionCode);
                }
            }

            // 原子性替换缓�?
            urlPermissionCache.clear();
            urlPermissionCache.putAll(newCache);

            // 持久化到多级缓存（供多实例共享，冷启动加速）
            Cache permCache = cacheManager.getCache(PERMISSION_CACHE_NAME);
            if (permCache != null) {
                permCache.put(PERM_MAPPING_CACHE_KEY, newCache);
            }

            permissionVersion.incrementAndGet();

            log.info("Loaded {} API permission mappings, version: {}",
                    newCache.size(), permissionVersion.get());

            // 发布权限更新事件
            eventPublisher.publishEvent(new PermissionRefreshEvent(this, permissionVersion.get()));

        } catch (Exception e) {
            log.error("Failed to load permissions", e);
        }
    }

    /**
     * 定时刷新权限（每5分钟�?
     */
    @Scheduled(fixedRate = 300000)
    public void scheduleRefresh() {
        log.debug("Scheduled permission refresh triggered");
        loadPermissions();
        clearRelatedCaches();
    }

    /**
     * 手动刷新权限
     */
    public void manualRefresh() {
        log.info("Manual permission refresh triggered");
        loadPermissions();
        clearRelatedCaches();
    }

    /**
     * 检�?URL是否需要权�?
     */
    public boolean requiresPermission(String method, String url) {
        String key = buildKey(method, url);
        return urlPermissionCache.containsKey(key);
    }

    /**
     * 获取 URL所需的权�?
     */
    public Set<String> getRequiredPermissions(String method, String url) {
        String key = buildKey(method, url);
        Set<String> permissions = urlPermissionCache.get(key);

        // 如果没有精确匹配，尝试通配符匹�?
        if (permissions == null || permissions.isEmpty()) {
            permissions = matchWildcardPermissions(method, url);
        }

        return permissions != null ? permissions : Collections.emptySet();
    }

    /**
     * 通配符匹�?
     * 支持路径参数: /api/users/{id} 匹配 /api/users/123
     */
    private Set<String> matchWildcardPermissions(String method, String url) {
        for (Map.Entry<String, Set<String>> entry : urlPermissionCache.entrySet()) {
            String pattern = entry.getKey();

            if (matchesPattern(pattern, method, url)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 模式匹配
     */
    private boolean matchesPattern(String pattern, String method, String url) {
        // 提取方法和路�?
        String[] patternParts = pattern.split(":", 2);
        if (patternParts.length != 2) {
            return false;
        }

        String patternMethod = patternParts[0];
        String patternPath = patternParts[1];

        // 方法匹配�? 表示所有方法）
        if (!"*".equals(patternMethod) && !method.equals(patternMethod)) {
            return false;
        }

        // 路径匹配
        return matchesPath(patternPath, url);
    }

    /**
     * 路径匹配算法
     * 支持: /api/users/{id}, /api/users/*, /api/**
     */
    private boolean matchesPath(String pattern, String path) {
        // 分割路径�?
        String[] patternSegments = pattern.split("/");
        String[] pathSegments = path.split("/");

        // ** 通配符：匹配任意层级
        if (pattern.contains("**")) {
            return matchesDeepWildcard(patternSegments, pathSegments);
        }

        // 长度不匹�?
        if (patternSegments.length != pathSegments.length) {
            return false;
        }

        // 逐段匹配
        for (int i = 0; i < patternSegments.length; i++) {
            String patternSeg = patternSegments[i];
            String pathSeg = pathSegments[i];

            // {xxx} 路径参数
            if (patternSeg.startsWith("{") && patternSeg.endsWith("}")) {
                continue;
            }

            // * 单层通配�?
            if ("*".equals(patternSeg)) {
                continue;
            }

            // 精确匹配
            if (!patternSeg.equals(pathSeg)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 深度通配符匹�?
     */
    private boolean matchesDeepWildcard(String[] patternSegments, String[] pathSegments) {
        int patternIdx = 0;
        int pathIdx = 0;

        while (patternIdx < patternSegments.length && pathIdx < pathSegments.length) {
            String patternSeg = patternSegments[patternIdx];

            if ("**".equals(patternSeg)) {
                // ** 匹配剩余所有路�?
                return true;
            }

            if (patternSeg.equals(pathSegments[pathIdx]) || "*".equals(patternSeg) ||
                    (patternSeg.startsWith("{") && patternSeg.endsWith("}"))) {
                patternIdx++;
                pathIdx++;
            } else {
                return false;
            }
        }

        return patternIdx == patternSegments.length && pathIdx == pathSegments.length;
    }

    /**
     * 构建缓存 key
     */
    private String buildKey(String method, String path) {
        return (method != null ? method : "*") + ":" + path;
    }

    /**
     * 清理相关缓存
     */
    private void clearRelatedCaches() {
        try {
            // 清理权限相关的所有缓�?
            String[] cacheNames = {
                    "userPermissions", "userRoles", "permissionTree",
                    "rolePermissions", "userInfo"
            };

            for (String cacheName : cacheNames) {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                    log.debug("Cleared cache: {}", cacheName);
                }
            }
        } catch (Exception e) {
            log.error("Failed to clear caches", e);
        }
    }

    /**
     * 获取权限版本�?
     */
    public long getPermissionVersion() {
        return permissionVersion.get();
    }

    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("version", permissionVersion.get());
        stats.put("cachedMappings", urlPermissionCache.size());
        stats.put("memorySize", estimateMemorySize());
        return stats;
    }

    /**
     * 估算内存占用
     */
    private long estimateMemorySize() {
        long size = 0;
        for (Map.Entry<String, Set<String>> entry : urlPermissionCache.entrySet()) {
            size += entry.getKey().length() * 2L; // String 占用
            size += entry.getValue().size() * 50L; // Set 元素估算
        }
        return size;
    }

    /**
     * 权限刷新事件
     */
    @Getter
    public static class PermissionRefreshEvent extends ApplicationEvent {
        private final long version;

        public PermissionRefreshEvent(Object source, long version) {
            super(source);
            this.version = version;
        }
    }
}

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
 * 鍔ㄦ€佹潈闄愬姞杞芥湇锟?
 * 鏀寔鏉冮檺鐑洿鏂帮紝鏃犻渶閲嶅惎搴旂敤
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

    // 鍐呭瓨缂撳瓨锛歎RL -> 鎵€闇€鏉冮檺鍒楄〃
    private final Map<String, Set<String>> urlPermissionCache = new ConcurrentHashMap<>();

    // 鏉冮檺鐗堟湰鍙凤紙鐢ㄤ簬妫€娴嬪彉鏇达級
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

                    // 鍒濆鍖栫増鏈彿
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
     * 灏嗗師濮嬬紦瀛樻暟鎹浆鎹负绫诲瀷瀹夊叏鐨勬潈闄愭槧锟?
     *
     * @param rawMap 浠庣紦瀛樿幏鍙栫殑鍘熷 Map
     * @return 绫诲瀷瀹夊叏鐨勬潈闄愭槧锟?URL -> 鏉冮檺闆嗗悎)
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
     * 鍒濆鍖栧姞杞芥潈闄愰厤锟?
     */
    public void loadPermissions() {
        log.info("Loading dynamic permissions...");

        try {
            // 鏌ヨ鎵€锟紸PI绫诲瀷鐨勬潈锟?
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

            // 鍘熷瓙鎬ф浛鎹㈢紦锟?
            urlPermissionCache.clear();
            urlPermissionCache.putAll(newCache);

            // 鎸佷箙鍖栧埌澶氱骇缂撳瓨锛堜緵澶氬疄渚嬪叡浜紝鍐峰惎鍔ㄥ姞閫燂級
            Cache permCache = cacheManager.getCache(PERMISSION_CACHE_NAME);
            if (permCache != null) {
                permCache.put(PERM_MAPPING_CACHE_KEY, newCache);
            }

            permissionVersion.incrementAndGet();

            log.info("Loaded {} API permission mappings, version: {}",
                    newCache.size(), permissionVersion.get());

            // 鍙戝竷鏉冮檺鏇存柊浜嬩欢
            eventPublisher.publishEvent(new PermissionRefreshEvent(this, permissionVersion.get()));

        } catch (Exception e) {
            log.error("Failed to load permissions", e);
        }
    }

    /**
     * 瀹氭椂鍒锋柊鏉冮檺锛堟瘡5鍒嗛挓锟?
     */
    @Scheduled(fixedRate = 300000)
    public void scheduleRefresh() {
        log.debug("Scheduled permission refresh triggered");
        loadPermissions();
        clearRelatedCaches();
    }

    /**
     * 鎵嬪姩鍒锋柊鏉冮檺
     */
    public void manualRefresh() {
        log.info("Manual permission refresh triggered");
        loadPermissions();
        clearRelatedCaches();
    }

    /**
     * 妫€锟経RL鏄惁闇€瑕佹潈锟?
     */
    public boolean requiresPermission(String method, String url) {
        String key = buildKey(method, url);
        return urlPermissionCache.containsKey(key);
    }

    /**
     * 鑾峰彇 URL鎵€闇€鐨勬潈锟?
     */
    public Set<String> getRequiredPermissions(String method, String url) {
        String key = buildKey(method, url);
        Set<String> permissions = urlPermissionCache.get(key);

        // 濡傛灉娌℃湁绮剧‘鍖归厤锛屽皾璇曢€氶厤绗﹀尮锟?
        if (permissions == null || permissions.isEmpty()) {
            permissions = matchWildcardPermissions(method, url);
        }

        return permissions != null ? permissions : Collections.emptySet();
    }

    /**
     * 閫氶厤绗﹀尮锟?
     * 鏀寔璺緞鍙傛暟: /api/users/{id} 鍖归厤 /api/users/123
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
     * 妯″紡鍖归厤
     */
    private boolean matchesPattern(String pattern, String method, String url) {
        // 鎻愬彇鏂规硶鍜岃矾锟?
        String[] patternParts = pattern.split(":", 2);
        if (patternParts.length != 2) {
            return false;
        }

        String patternMethod = patternParts[0];
        String patternPath = patternParts[1];

        // 鏂规硶鍖归厤锟?琛ㄧず鎵€鏈夋柟娉曪級
        if (!"*".equals(patternMethod) && !method.equals(patternMethod)) {
            return false;
        }

        // 璺緞鍖归厤
        return matchesPath(patternPath, url);
    }

    /**
     * 璺緞鍖归厤绠楁硶
     * 鏀寔: /api/users/{id}, /api/users/*, /api/**
     */
    private boolean matchesPath(String pattern, String path) {
        // 鍒嗗壊璺緞锟?
        String[] patternSegments = pattern.split("/");
        String[] pathSegments = path.split("/");

        // ** 閫氶厤绗︼細鍖归厤浠绘剰灞傜骇
        if (pattern.contains("**")) {
            return matchesDeepWildcard(patternSegments, pathSegments);
        }

        // 闀垮害涓嶅尮锟?
        if (patternSegments.length != pathSegments.length) {
            return false;
        }

        // 閫愭鍖归厤
        for (int i = 0; i < patternSegments.length; i++) {
            String patternSeg = patternSegments[i];
            String pathSeg = pathSegments[i];

            // {xxx} 璺緞鍙傛暟
            if (patternSeg.startsWith("{") && patternSeg.endsWith("}")) {
                continue;
            }

            // * 鍗曞眰閫氶厤锟?
            if ("*".equals(patternSeg)) {
                continue;
            }

            // 绮剧‘鍖归厤
            if (!patternSeg.equals(pathSeg)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 娣卞害閫氶厤绗﹀尮锟?
     */
    private boolean matchesDeepWildcard(String[] patternSegments, String[] pathSegments) {
        int patternIdx = 0;
        int pathIdx = 0;

        while (patternIdx < patternSegments.length && pathIdx < pathSegments.length) {
            String patternSeg = patternSegments[patternIdx];

            if ("**".equals(patternSeg)) {
                // ** 鍖归厤鍓╀綑鎵€鏈夎矾锟?
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
     * 鏋勫缓缂撳瓨 key
     */
    private String buildKey(String method, String path) {
        return (method != null ? method : "*") + ":" + path;
    }

    /**
     * 娓呯悊鐩稿叧缂撳瓨
     */
    private void clearRelatedCaches() {
        try {
            // 娓呯悊鏉冮檺鐩稿叧鐨勬墍鏈夌紦锟?
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
     * 鑾峰彇鏉冮檺鐗堟湰锟?
     */
    public long getPermissionVersion() {
        return permissionVersion.get();
    }

    /**
     * 鑾峰彇缂撳瓨缁熻淇℃伅
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("version", permissionVersion.get());
        stats.put("cachedMappings", urlPermissionCache.size());
        stats.put("memorySize", estimateMemorySize());
        return stats;
    }

    /**
     * 浼扮畻鍐呭瓨鍗犵敤
     */
    private long estimateMemorySize() {
        long size = 0;
        for (Map.Entry<String, Set<String>> entry : urlPermissionCache.entrySet()) {
            size += entry.getKey().length() * 2L; // String 鍗犵敤
            size += entry.getValue().size() * 50L; // Set 鍏冪礌浼扮畻
        }
        return size;
    }

    /**
     * 鏉冮檺鍒锋柊浜嬩欢
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

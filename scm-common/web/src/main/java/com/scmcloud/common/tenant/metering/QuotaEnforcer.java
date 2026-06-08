package com.scmcloud.common.tenant.metering;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Enforces tenant API quota using Redis Lua scripts for atomic check-and-increment.
 *
 * <p>Quota limits are cached in-memory (refreshed on service restart).
 * The Redis counter uses INCR + EXPIRE for daily reset.</p>
 *
 * <p>Lua script ensures atomicity: check limit → increment → set expiry in a single round-trip.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuotaEnforcer {
    private final StringRedisTemplate redisTemplate;

    private static final String COUNTER_PREFIX = "metering:";
    private static final long DEFAULT_DAILY_LIMIT = 100_000L;

    // In-memory quota cache: tenantId -> daily limit
    private final ConcurrentMap<UUID, Long> quotaLimits = new ConcurrentHashMap<>();

    /**
     * Lua script: atomic check-and-increment.
     * KEYS[1] = counter key
     * ARGV[1] = daily limit
     * ARGV[2] = TTL in seconds (until end of day)
     * Returns: 1 if allowed, 0 if quota exceeded.
     */
    private static final String QUOTA_SCRIPT = """
        local current = redis.call('GET', KEYS[1])
        local count = current and tonumber(current) or 0
        if count >= tonumber(ARGV[1]) then
            return 0
        end
        count = redis.call('INCR', KEYS[1])
        if count == 1 then
            redis.call('EXPIRE', KEYS[1], ARGV[2])
        end
        return 1
        """;

    /**
     * Check if the tenant has quota remaining and increment the counter atomically.
     *
     * @return true if allowed, false if quota exceeded
     */
    public boolean checkAndIncrement(UUID tenantId) {
        String key = counterKey(tenantId);
        long limit = quotaLimits.getOrDefault(tenantId, DEFAULT_DAILY_LIMIT);
        long ttlSeconds = secondsUntilEndOfDay();

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(QUOTA_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script, Collections.singletonList(key),
                String.valueOf(limit), String.valueOf(ttlSeconds));

        boolean allowed = result != null && result == 1L;
        if (!allowed) {
            log.warn("API quota exceeded for tenant {}: limit={}", tenantId, limit);
        }
        return allowed;
    }

    /**
     * Set the daily API quota for a tenant.
     */
    public void setQuota(UUID tenantId, long dailyLimit) {
        quotaLimits.put(tenantId, dailyLimit);
        log.info("Set API quota for tenant {}: {}", tenantId, dailyLimit);
    }

    /**
     * Get the current usage count for a tenant today.
     */
    public long getCurrentUsage(UUID tenantId) {
        String key = counterKey(tenantId);
        String value = redisTemplate.opsForValue().get(key);
        return value == null ? 0 : Long.parseLong(value);
    }

    /**
     * Get the quota limit for a tenant.
     */
    public long getQuotaLimit(UUID tenantId) {
        return quotaLimits.getOrDefault(tenantId, DEFAULT_DAILY_LIMIT);
    }

    private String counterKey(UUID tenantId) {
        String date = LocalDate.now(ZoneOffset.UTC).toString();
        return COUNTER_PREFIX + tenantId + ":" + date;
    }

    private long secondsUntilEndOfDay() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        long nowEpoch = java.time.Instant.now().getEpochSecond();
        long endOfDayEpoch = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        return Math.max(1, endOfDayEpoch - nowEpoch);
    }
}

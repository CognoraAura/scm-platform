package com.scmcloud.common.data.rw.health;

import com.scmcloud.common.data.rw.config.ReadWriteProperties;
import com.scmcloud.common.data.rw.routing.ReadWriteRoutingDataSource;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 从库健康检查器
 * <p>
 * 功能�?
 * - 定期检查从库连�?
 * - 检测复制延�?
 * - 自动摘除/恢复不可用节�?
 *
 * @author Deng
 * @since 2025-12-16
 */
@Slf4j
public class SlaveHealthChecker {
    private final Map<String, ReadWriteRoutingDataSource> routingDataSources;
    private final Map<String, Map<String, DataSource>> slaveDataSources;
    private final ReadWriteProperties properties;

    /**
     * 连续失败计数
     */
    private final Map<String, AtomicInteger> failureCounters = new ConcurrentHashMap<>();

    /**
     * 复制延迟（毫秒）
     */
    private final Map<String, Long> replicationLags = new ConcurrentHashMap<>();

    public SlaveHealthChecker(Map<String, ReadWriteRoutingDataSource> routingDataSources,
                               Map<String, Map<String, DataSource>> slaveDataSources,
                               ReadWriteProperties properties,
                               MeterRegistry meterRegistry) {
        this.routingDataSources = routingDataSources;
        this.slaveDataSources = slaveDataSources;
        this.properties = properties;

        // 注册指标
        if (meterRegistry != null) {
            registerMetrics(meterRegistry);
        }
    }

    private void registerMetrics(MeterRegistry meterRegistry) {
        // 复制延迟指标
        for (String groupName : slaveDataSources.keySet()) {
            for (String slaveName : slaveDataSources.get(groupName).keySet()) {
                String fullName = groupName + "." + slaveName;

                Gauge.builder("datasource.rw.replication.lag", replicationLags,
                                map -> map.getOrDefault(fullName, 0L).doubleValue())
                        .tag("group", groupName)
                        .tag("slave", slaveName)
                        .description("Replication lag in milliseconds")
                        .register(meterRegistry);
            }
        }
    }

    /**
     * 定期健康检�?
     */
    @Scheduled(fixedDelayString = "${spring.datasource.rw.health-check-interval:30000}")
    public void healthCheck() {
        if (!properties.isHealthCheckEnabled()) {
            return;
        }

        log.debug("[Health] Starting slave health check...");

        for (Map.Entry<String, Map<String, DataSource>> groupEntry : slaveDataSources.entrySet()) {
            String groupName = groupEntry.getKey();
            Map<String, DataSource> slaves = groupEntry.getValue();

            for (Map.Entry<String, DataSource> slaveEntry : slaves.entrySet()) {
                String slaveName = slaveEntry.getKey();
                DataSource dataSource = slaveEntry.getValue();

                checkSlave(groupName, slaveName, dataSource);
            }
        }
    }

    private void checkSlave(String groupName, String slaveName, DataSource dataSource) {
        String fullName = groupName + "." + slaveName;
        AtomicInteger failureCounter = failureCounters.computeIfAbsent(fullName,
                k -> new AtomicInteger(0));

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            // 1. 连接检�?
            if (!connection.isValid(5)) {
                handleFailure(groupName, slaveName, failureCounter, "Connection invalid");
                return;
            }

            // 2. 复制延迟检查（PostgreSQL�?
            Long lagMs = checkReplicationLag(statement);
            replicationLags.put(fullName, lagMs != null ? lagMs : 0L);

            if (lagMs != null && lagMs > properties.getReplicationLagTolerance().toMillis()) {
                log.warn("[Health] Slave [{}] replication lag too high: {}ms (threshold: {}ms)",
                        fullName, lagMs, properties.getReplicationLagTolerance().toMillis());
                handleFailure(groupName, slaveName, failureCounter, "Replication lag too high");
                return;
            }

            // 健康检查通过，重置计数器
            if (failureCounter.get() > 0) {
                failureCounter.set(0);
                markSlaveAvailable(groupName, slaveName);
            }

            log.debug("[Health] Slave [{}] is healthy, lag: {}ms", fullName, lagMs);

        } catch (Exception e) {
            handleFailure(groupName, slaveName, failureCounter, e.getMessage());
        }
    }

    /**
     * 检查复制延迟（PostgreSQL�?
     */
    private Long checkReplicationLag(Statement statement) {
        try {
            // PostgreSQL 复制延迟查询
            ResultSet rs = statement.executeQuery("""
                    SELECT CASE
                        WHEN pg_last_wal_receive_lsn() = pg_last_wal_replay_lsn() THEN 0
                        ELSE EXTRACT(EPOCH FROM now() - pg_last_xact_replay_timestamp())::bigint * 1000
                    END AS lag_ms
                    """);

            if (rs.next()) {
                return rs.getLong("lag_ms");
            }
        } catch (Exception e) {
            // 可能不是从库，或者版本不支持
            log.trace("[Health] Could not check replication lag: {}", e.getMessage());
        }
        return null;
    }

    private void handleFailure(String groupName, String slaveName,
                                AtomicInteger failureCounter, String reason) {
        int failures = failureCounter.incrementAndGet();
        String fullName = groupName + "." + slaveName;

        log.warn("[Health] Slave [{}] health check failed ({}): {}",
                fullName, failures, reason);

        if (failures >= properties.getFailureThreshold()) {
            markSlaveUnavailable(groupName, slaveName);
        }
    }

    private void markSlaveUnavailable(String groupName, String slaveName) {
        ReadWriteRoutingDataSource routingDataSource = routingDataSources.get(groupName);
        if (routingDataSource != null) {
            routingDataSource.markSlaveUnavailable(slaveName);
            log.error("[Health] Slave [{}] marked as UNAVAILABLE after {} consecutive failures",
                    groupName + "." + slaveName, properties.getFailureThreshold());
        }
    }

    private void markSlaveAvailable(String groupName, String slaveName) {
        ReadWriteRoutingDataSource routingDataSource = routingDataSources.get(groupName);
        if (routingDataSource != null) {
            routingDataSource.markSlaveAvailable(slaveName);
            log.info("[Health] Slave [{}] recovered and marked as AVAILABLE",
                    groupName + "." + slaveName);
        }
    }

    /**
     * 获取所有从库的健康状�?
     */
    public Map<String, HealthStatus> getAllHealthStatus() {
        Map<String, HealthStatus> result = new ConcurrentHashMap<>();

        for (Map.Entry<String, ReadWriteRoutingDataSource> entry : routingDataSources.entrySet()) {
            String groupName = entry.getKey();
            Map<String, Boolean> availability = entry.getValue().getSlaveAvailability();

            for (Map.Entry<String, Boolean> slaveEntry : availability.entrySet()) {
                String fullName = groupName + "." + slaveEntry.getKey();
                Long lagMs = replicationLags.getOrDefault(fullName, 0L);

                result.put(fullName, new HealthStatus(
                        slaveEntry.getValue(),
                        lagMs,
                        failureCounters.getOrDefault(fullName, new AtomicInteger(0)).get()
                ));
            }
        }

        return result;
    }

    /**
     * 健康状�?
     */
    public record HealthStatus(
            boolean available,
            long replicationLagMs,
            int consecutiveFailures
    ) {}
}

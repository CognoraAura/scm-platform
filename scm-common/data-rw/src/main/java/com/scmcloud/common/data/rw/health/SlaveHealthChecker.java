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
 * 浠庡簱鍋ュ悍妫€鏌ュ櫒
 * <p>
 * 鍔熻兘锟?
 * - 瀹氭湡妫€鏌ヤ粠搴撹繛锟?
 * - 妫€娴嬪鍒跺欢锟?
 * - 鑷姩鎽橀櫎/鎭㈠涓嶅彲鐢ㄨ妭锟?
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
     * 杩炵画澶辫触璁℃暟
     */
    private final Map<String, AtomicInteger> failureCounters = new ConcurrentHashMap<>();

    /**
     * 澶嶅埗寤惰繜锛堟绉掞級
     */
    private final Map<String, Long> replicationLags = new ConcurrentHashMap<>();

    public SlaveHealthChecker(Map<String, ReadWriteRoutingDataSource> routingDataSources,
                               Map<String, Map<String, DataSource>> slaveDataSources,
                               ReadWriteProperties properties,
                               MeterRegistry meterRegistry) {
        this.routingDataSources = routingDataSources;
        this.slaveDataSources = slaveDataSources;
        this.properties = properties;

        // 娉ㄥ唽鎸囨爣
        if (meterRegistry != null) {
            registerMetrics(meterRegistry);
        }
    }

    private void registerMetrics(MeterRegistry meterRegistry) {
        // 澶嶅埗寤惰繜鎸囨爣
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
     * 瀹氭湡鍋ュ悍妫€锟?
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

            // 1. 杩炴帴妫€锟?
            if (!connection.isValid(5)) {
                handleFailure(groupName, slaveName, failureCounter, "Connection invalid");
                return;
            }

            // 2. 澶嶅埗寤惰繜妫€鏌ワ紙PostgreSQL锟?
            Long lagMs = checkReplicationLag(statement);
            replicationLags.put(fullName, lagMs != null ? lagMs : 0L);

            if (lagMs != null && lagMs > properties.getReplicationLagTolerance().toMillis()) {
                log.warn("[Health] Slave [{}] replication lag too high: {}ms (threshold: {}ms)",
                        fullName, lagMs, properties.getReplicationLagTolerance().toMillis());
                handleFailure(groupName, slaveName, failureCounter, "Replication lag too high");
                return;
            }

            // 鍋ュ悍妫€鏌ラ€氳繃锛岄噸缃鏁板櫒
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
     * 妫€鏌ュ鍒跺欢杩燂紙PostgreSQL锟?
     */
    private Long checkReplicationLag(Statement statement) {
        try {
            // PostgreSQL 澶嶅埗寤惰繜鏌ヨ
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
            // 鍙兘涓嶆槸浠庡簱锛屾垨鑰呯増鏈笉鏀寔
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
     * 鑾峰彇鎵€鏈変粠搴撶殑鍋ュ悍鐘讹拷
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
     * 鍋ュ悍鐘讹拷
     */
    public record HealthStatus(
            boolean available,
            long replicationLagMs,
            int consecutiveFailures
    ) {}
}

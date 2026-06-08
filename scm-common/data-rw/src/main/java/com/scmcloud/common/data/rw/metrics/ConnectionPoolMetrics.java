package com.scmcloud.common.data.rw.metrics;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 杩炴帴姹犳寚鏍囬噰闆嗗櫒
 * <p>
 * 鍙傝€冿細
 * - 闃块噷 TDDL DataSourceMonitor
 * - 缇庡洟 Zebra DataSourceMetrics
 * - HikariCP Metrics
 * <p>
 * 閲囬泦鎸囨爣锟?
 * - 娲昏穬杩炴帴锟?
 * - 绌洪棽杩炴帴锟?
 * - 绛夊緟绾跨▼锟?
 * - 杩炴帴鑾峰彇鑰楁椂
 * - 杩炴帴浣跨敤鑰楁椂
 * - 杩炴帴鍒涘缓鑰楁椂
 *
 * @author Deng
 * @since 2025-12-16
 */
@Slf4j
public class ConnectionPoolMetrics {
    private static final String METRIC_PREFIX = "datasource.rw.pool";

    private final MeterRegistry meterRegistry;
    private final Map<String, Timer> connectionAcquireTimers = new ConcurrentHashMap<>();
    private final Map<String, Timer> connectionUseTimers = new ConcurrentHashMap<>();

    public ConnectionPoolMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * 娉ㄥ唽鏁版嵁婧愭寚锟?
     */
    public void registerDataSource(String groupName, String dsName, DataSource dataSource) {
        if (!(dataSource instanceof HikariDataSource hikariDs)) {
            log.warn("[Pool-Metrics] DataSource [{}] is not HikariDataSource, skipping metrics",
                    dsName);
            return;
        }

        Tags tags = Tags.of("group", groupName, "name", dsName);
        String fullName = groupName + "." + dsName;

        // 灏濊瘯鑾峰彇 MXBean
        HikariPoolMXBean poolMXBean = hikariDs.getHikariPoolMXBean();
        if (poolMXBean == null) {
            log.warn("[Pool-Metrics] HikariPoolMXBean not available for [{}], metrics will be limited",
                    fullName);
            registerBasicMetrics(hikariDs, tags, fullName);
            return;
        }

        // 娲昏穬杩炴帴锟?
        Gauge.builder(METRIC_PREFIX + ".active", poolMXBean, HikariPoolMXBean::getActiveConnections)
                .tags(tags)
                .description("Number of active connections")
                .register(meterRegistry);

        // 绌洪棽杩炴帴锟?
        Gauge.builder(METRIC_PREFIX + ".idle", poolMXBean, HikariPoolMXBean::getIdleConnections)
                .tags(tags)
                .description("Number of idle connections")
                .register(meterRegistry);

        // 鎬昏繛鎺ユ暟
        Gauge.builder(METRIC_PREFIX + ".total", poolMXBean, HikariPoolMXBean::getTotalConnections)
                .tags(tags)
                .description("Total number of connections")
                .register(meterRegistry);

        // 绛夊緟绾跨▼锟?
        Gauge.builder(METRIC_PREFIX + ".pending", poolMXBean, HikariPoolMXBean::getThreadsAwaitingConnection)
                .tags(tags)
                .description("Number of threads waiting for a connection")
                .register(meterRegistry);

        // 鏈€澶ц繛鎺ユ暟
        Gauge.builder(METRIC_PREFIX + ".max", hikariDs, HikariDataSource::getMaximumPoolSize)
                .tags(tags)
                .description("Maximum pool size")
                .register(meterRegistry);

        // 鏈€灏忕┖闂茶繛鎺ユ暟
        Gauge.builder(METRIC_PREFIX + ".min", hikariDs, HikariDataSource::getMinimumIdle)
                .tags(tags)
                .description("Minimum idle connections")
                .register(meterRegistry);

        // 杩炴帴鑾峰彇鑰楁椂
        Timer acquireTimer = Timer.builder(METRIC_PREFIX + ".acquire")
                .tags(tags)
                .description("Connection acquire time")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
        connectionAcquireTimers.put(fullName, acquireTimer);

        // 杩炴帴浣跨敤鑰楁椂
        Timer useTimer = Timer.builder(METRIC_PREFIX + ".use")
                .tags(tags)
                .description("Connection use time")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
        connectionUseTimers.put(fullName, useTimer);

        // 杩炴帴姹犲埄鐢ㄧ巼
        Gauge.builder(METRIC_PREFIX + ".utilization", poolMXBean,
                        bean -> {
                            int total = bean.getTotalConnections();
                            if (total == 0) return 0.0;
                            return (double) bean.getActiveConnections() / total;
                        })
                .tags(tags)
                .description("Connection pool utilization (0.0 - 1.0)")
                .register(meterRegistry);

        log.info("[Pool-Metrics] Registered metrics for datasource [{}]", fullName);
    }

    /**
     * 娉ㄥ唽鍩虹鎸囨爣锛堝綋 MXBean 涓嶅彲鐢ㄦ椂锟?
     */
    private void registerBasicMetrics(HikariDataSource hikariDs, Tags tags, String fullName) {
        Gauge.builder(METRIC_PREFIX + ".max", hikariDs, HikariDataSource::getMaximumPoolSize)
                .tags(tags)
                .description("Maximum pool size")
                .register(meterRegistry);

        Gauge.builder(METRIC_PREFIX + ".min", hikariDs, HikariDataSource::getMinimumIdle)
                .tags(tags)
                .description("Minimum idle connections")
                .register(meterRegistry);

        log.info("[Pool-Metrics] Registered basic metrics for datasource [{}]", fullName);
    }

    /**
     * 璁板綍杩炴帴鑾峰彇鑰楁椂
     */
    public void recordAcquireTime(String groupName, String dsName, long nanos) {
        String fullName = groupName + "." + dsName;
        Timer timer = connectionAcquireTimers.get(fullName);
        if (timer != null) {
            timer.record(nanos, TimeUnit.NANOSECONDS);
        }
    }

    /**
     * 璁板綍杩炴帴浣跨敤鑰楁椂
     */
    public void recordUseTime(String groupName, String dsName, long nanos) {
        String fullName = groupName + "." + dsName;
        Timer timer = connectionUseTimers.get(fullName);
        if (timer != null) {
            timer.record(nanos, TimeUnit.NANOSECONDS);
        }
    }

    /**
     * 鑾峰彇杩炴帴姹犵姸鎬佸揩锟?
     */
    public Map<String, Object> getPoolSnapshot(String groupName, String dsName,
                                                DataSource dataSource) {
        if (!(dataSource instanceof HikariDataSource hikariDs)) {
            return Map.of("error", "Not a HikariDataSource");
        }

        HikariPoolMXBean poolMXBean = hikariDs.getHikariPoolMXBean();
        if (poolMXBean == null) {
            return Map.of(
                    "maxPoolSize", hikariDs.getMaximumPoolSize(),
                    "minIdle", hikariDs.getMinimumIdle(),
                    "status", "Pool not yet initialized"
            );
        }

        return Map.of(
                "activeConnections", poolMXBean.getActiveConnections(),
                "idleConnections", poolMXBean.getIdleConnections(),
                "totalConnections", poolMXBean.getTotalConnections(),
                "threadsAwaitingConnection", poolMXBean.getThreadsAwaitingConnection(),
                "maxPoolSize", hikariDs.getMaximumPoolSize(),
                "minIdle", hikariDs.getMinimumIdle(),
                "connectionTimeout", hikariDs.getConnectionTimeout(),
                "idleTimeout", hikariDs.getIdleTimeout(),
                "maxLifetime", hikariDs.getMaxLifetime()
        );
    }
}

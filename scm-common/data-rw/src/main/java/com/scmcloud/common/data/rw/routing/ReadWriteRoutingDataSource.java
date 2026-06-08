package com.scmcloud.common.data.rw.routing;

import com.scmcloud.common.data.rw.config.ReadWriteProperties;
import com.scmcloud.common.data.rw.loadbalance.SlaveLoadBalancer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 璇诲啓鍒嗙璺敱鏁版嵁锟?
 * <p>
 * 鍩轰簬 Spring AbstractRoutingDataSource 瀹炵幇锛屾敮鎸侊細
 * - 涓讳粠鑷姩璺敱
 * - 璐熻浇鍧囪　
 * - 鍋ュ悍妫€锟?
 * - 璇诲啓涓€鑷存€т繚锟?
 *
 * @author Deng
 * @since 2025-12-16
 */
@Slf4j
public class ReadWriteRoutingDataSource extends AbstractRoutingDataSource {
    private static final String MASTER_KEY = "master";

    private final String groupName;
    private final ReadWriteProperties properties;
    private final SlaveLoadBalancer loadBalancer;

    @Setter
    private List<SlaveLoadBalancer.SlaveInfo> slaveInfos;

    /**
     * 浠庡簱鍙敤鎬х姸锟?
     */
    private final Map<String, Boolean> slaveAvailability = new ConcurrentHashMap<>();

    // Metrics
    private Counter masterRouteCounter;
    private Counter slaveRouteCounter;
    private Counter fallbackCounter;

    public ReadWriteRoutingDataSource(String groupName,
                                       DataSource masterDataSource,
                                       Map<String, DataSource> slaveDataSources,
                                       ReadWriteProperties properties,
                                       SlaveLoadBalancer loadBalancer,
                                       MeterRegistry meterRegistry) {
        this.groupName = groupName;
        this.properties = properties;
        this.loadBalancer = loadBalancer;

        // 璁剧疆鏁版嵁锟?
        Map<Object, Object> targetDataSources = new ConcurrentHashMap<>();
        targetDataSources.put(MASTER_KEY, masterDataSource);
        targetDataSources.putAll(slaveDataSources);

        setTargetDataSources(targetDataSources);
        setDefaultTargetDataSource(masterDataSource);

        // 鍒濆鍖栦粠搴撳彲鐢拷
        slaveDataSources.keySet().forEach(name -> slaveAvailability.put(name, true));

        // 鍒濆鍖栨寚锟?
        if (meterRegistry != null) {
            initMetrics(meterRegistry);
        }
    }

    private void initMetrics(MeterRegistry meterRegistry) {
        this.masterRouteCounter = Counter.builder("datasource.rw.route")
                .tag("group", groupName)
                .tag("target", "master")
                .description("Number of routes to master")
                .register(meterRegistry);

        this.slaveRouteCounter = Counter.builder("datasource.rw.route")
                .tag("group", groupName)
                .tag("target", "slave")
                .description("Number of routes to slave")
                .register(meterRegistry);

        this.fallbackCounter = Counter.builder("datasource.rw.fallback")
                .tag("group", groupName)
                .description("Number of fallbacks to master")
                .register(meterRegistry);
    }

    @Override
    protected Object determineCurrentLookupKey() {
        // 1. 妫€鏌ユ槸鍚﹀簲璇ヤ娇鐢ㄤ富锟?
        long readMasterAfterWriteMs = properties.getReadMasterAfterWrite().toMillis();
        if (ReadWriteRoutingContext.shouldUseMaster(readMasterAfterWriteMs)) {
            log.debug("[RW-Routing] Group [{}] routing to MASTER", groupName);
            incrementMasterCounter();
            return MASTER_KEY;
        }

        // 2. 妫€鏌ヨ矾鐢辩被锟?
        ReadWriteRoutingContext.RoutingType routingType = ReadWriteRoutingContext.current();

        if (routingType == ReadWriteRoutingContext.RoutingType.MASTER) {
            log.debug("[RW-Routing] Group [{}] routing to MASTER (explicit)", groupName);
            incrementMasterCounter();
            return MASTER_KEY;
        }

        // 3. 灏濊瘯璺敱鍒颁粠锟?
        if (routingType == ReadWriteRoutingContext.RoutingType.SLAVE ||
                routingType == ReadWriteRoutingContext.RoutingType.AUTO) {

            // 妫€鏌ユ槸鍚︽寚瀹氫簡鐗瑰畾浠庡簱
            String specifiedSlave = ReadWriteRoutingContext.getSpecifiedSlave();
            if (specifiedSlave != null && slaveAvailability.getOrDefault(specifiedSlave, false)) {
                log.debug("[RW-Routing] Group [{}] routing to SLAVE [{}] (specified)",
                        groupName, specifiedSlave);
                incrementSlaveCounter();
                return specifiedSlave;
            }

            // 浣跨敤璐熻浇鍧囪　閫夋嫨浠庡簱
            String selectedSlave = selectSlave();
            if (selectedSlave != null) {
                log.debug("[RW-Routing] Group [{}] routing to SLAVE [{}]",
                        groupName, selectedSlave);
                incrementSlaveCounter();
                return selectedSlave;
            }

            // 浠庡簱涓嶅彲鐢紝闄嶇骇鍒颁富锟?
            log.warn("[RW-Routing] Group [{}] no available slave, fallback to MASTER", groupName);
            incrementFallbackCounter();
        }

        incrementMasterCounter();
        return MASTER_KEY;
    }

    private String selectSlave() {
        if (slaveInfos == null || slaveInfos.isEmpty()) {
            return null;
        }

        // 杩囨护鍙敤鐨勪粠锟?
        List<SlaveLoadBalancer.SlaveInfo> availableSlaves = slaveInfos.stream()
                .filter(s -> slaveAvailability.getOrDefault(s.name(), false))
                .toList();

        if (availableSlaves.isEmpty()) {
            return null;
        }

        return loadBalancer.select(availableSlaves);
    }

    /**
     * 鏍囪浠庡簱涓嶅彲锟?
     */
    public void markSlaveUnavailable(String slaveName) {
        slaveAvailability.put(slaveName, false);
        log.warn("[RW-Routing] Group [{}] slave [{}] marked as UNAVAILABLE",
                groupName, slaveName);
    }

    /**
     * 鏍囪浠庡簱鍙敤
     */
    public void markSlaveAvailable(String slaveName) {
        slaveAvailability.put(slaveName, true);
        log.info("[RW-Routing] Group [{}] slave [{}] marked as AVAILABLE",
                groupName, slaveName);
    }

    /**
     * 鑾峰彇浠庡簱鍙敤鎬х姸锟?
     */
    public Map<String, Boolean> getSlaveAvailability() {
        return Map.copyOf(slaveAvailability);
    }

    private void incrementMasterCounter() {
        if (masterRouteCounter != null) {
            masterRouteCounter.increment();
        }
    }

    private void incrementSlaveCounter() {
        if (slaveRouteCounter != null) {
            slaveRouteCounter.increment();
        }
    }

    private void incrementFallbackCounter() {
        if (fallbackCounter != null) {
            fallbackCounter.increment();
        }
    }
}

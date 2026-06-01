package com.scmcloud.common.data.rw.circuitbreaker;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.degrade.circuitbreaker.CircuitBreakerStrategy;
import com.scmcloud.common.data.rw.routing.ReadWriteRoutingDataSource;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 从库熔断�?
 * <p>
 * 基于 Sentinel 实现从库熔断�?
 * - 慢调用比例熔�?
 * - 异常比例熔断
 * - 异常数熔�?
 *
 * @author Deng
 * @since 2025-12-16
 */
@Slf4j
public class SlaveCircuitBreaker {
    private static final String RESOURCE_PREFIX = "slave:";

    private final Map<String, ReadWriteRoutingDataSource> routingDataSources;
    private final Map<String, CircuitBreakerState> circuitStates = new ConcurrentHashMap<>();

    /**
     * 熔断器状�?
     */
    public enum CircuitBreakerState {
        CLOSED,      // 正常
        OPEN,        // 熔断
        HALF_OPEN    // 半开（探测）
    }

    public SlaveCircuitBreaker(Map<String, ReadWriteRoutingDataSource> routingDataSources) {
        this.routingDataSources = routingDataSources;
        initDegradeRules();
    }

    /**
     * 初始化降级规�?
     */
    private void initDegradeRules() {
        List<DegradeRule> rules = new ArrayList<>();

        for (String groupName : routingDataSources.keySet()) {
            ReadWriteRoutingDataSource ds = routingDataSources.get(groupName);
            for (String slaveName : ds.getSlaveAvailability().keySet()) {
                String resource = RESOURCE_PREFIX + groupName + "." + slaveName;

                // 慢调用比例熔�?
                DegradeRule slowRule = new DegradeRule(resource)
                        .setGrade(CircuitBreakerStrategy.SLOW_REQUEST_RATIO.getType())
                        .setCount(0.5)           // 慢调用比例阈�?50%
                        .setSlowRatioThreshold(0.5)
                        .setTimeWindow(30)       // 熔断时长 30s
                        .setMinRequestAmount(10) // 最小请求数
                        .setStatIntervalMs(10000); // 统计时长 10s

                // 异常比例熔断
                DegradeRule exceptionRule = new DegradeRule(resource)
                        .setGrade(CircuitBreakerStrategy.ERROR_RATIO.getType())
                        .setCount(0.5)           // 异常比例阈�?50%
                        .setTimeWindow(30)       // 熔断时长 30s
                        .setMinRequestAmount(10)
                        .setStatIntervalMs(10000);

                rules.add(slowRule);
                rules.add(exceptionRule);

                circuitStates.put(resource, CircuitBreakerState.CLOSED);
                log.debug("[Circuit-Breaker] Registered rules for slave: {}", resource);
            }
        }

        DegradeRuleManager.loadRules(rules);
        log.info("[Circuit-Breaker] Loaded {} degrade rules for {} slaves",
                rules.size(), circuitStates.size());
    }

    /**
     * 执行带熔断保护的操作
     *
     * @param groupName 数据源组�?
     * @param slaveName 从库�?
     * @param operation 操作
     * @param fallback  降级操作
     * @param <T>       返回类型
     * @return 执行结果
     */
    public <T> T executeWithCircuitBreaker(String groupName, String slaveName, SlaveOperation<T> operation,
                                           SlaveFallback<T> fallback) throws Exception {
        String resource = RESOURCE_PREFIX + groupName + "." + slaveName;

        // 尝试获取 Entry，BlockException 表示熔断触发
        Entry entry;
        try {
            entry = SphU.entry(resource);
        } catch (BlockException e) {
            // 熔断触发
            log.warn("[Circuit-Breaker] Slave [{}] is blocked, circuit is OPEN", resource);
            circuitStates.put(resource, CircuitBreakerState.OPEN);
            markSlaveUnavailable(groupName, slaveName);
            return fallback.fallback(e);
        }

        // Entry 获取成功，使�?try-with-resources 执行操作
        try (entry) {
            T result = operation.execute();
            circuitStates.put(resource, CircuitBreakerState.CLOSED);
            return result;
        } catch (Exception e) {
            // 业务异常，上报给 Sentinel 统计
            Tracer.trace(e);
            throw e;
        }
    }

    /**
     * 标记从库不可�?
     */
    private void markSlaveUnavailable(String groupName, String slaveName) {
        ReadWriteRoutingDataSource ds = routingDataSources.get(groupName);
        if (ds != null) {
            ds.markSlaveUnavailable(slaveName);
            log.warn("[Circuit-Breaker] Marked slave [{}] in group [{}] as UNAVAILABLE",
                    slaveName, groupName);
        }
    }

    /**
     * 获取熔断器状�?
     */
    public CircuitBreakerState getState(String groupName, String slaveName) {
        String resource = RESOURCE_PREFIX + groupName + "." + slaveName;
        return circuitStates.getOrDefault(resource, CircuitBreakerState.CLOSED);
    }

    /**
     * 获取所有熔断器状�?
     */
    public Map<String, CircuitBreakerState> getAllStates() {
        return Map.copyOf(circuitStates);
    }

    /**
     * 从库操作接口
     */
    @FunctionalInterface
    public interface SlaveOperation<T> {
        T execute() throws Exception;
    }

    /**
     * 降级操作接口
     */
    @FunctionalInterface
    public interface SlaveFallback<T> {
        T fallback(BlockException e);
    }
}

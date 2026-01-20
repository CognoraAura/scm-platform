package com.frog.common.rest.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.RandomLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.RoundRobinLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Spring Cloud LoadBalancer 负载均衡策略配置
 *
 * <p>支持的负载均衡策略：
 * <ul>
 *   <li><b>round-robin</b>（默认）: 轮询策略，依次选择实例</li>
 *   <li><b>random</b>: 随机策略，随机选择实例</li>
 *   <li><b>weighted-round-robin</b>: 加权轮询，根据实例权重分配请求</li>
 * </ul>
 *
 * <p>配置方式（application.yml）:
 * <pre>
 * spring:
 *   cloud:
 *     loadbalancer:
 *       # 负载均衡策略: round-robin（默认）, random, weighted-round-robin
 *       strategy: round-robin
 *
 *       # Nacos 权重配置（仅 weighted-round-robin 策略生效）
 *       nacos:
 *         enabled: true
 * </pre>
 *
 * <p>Nacos 实例权重配置示例：
 * <pre>
 * # 在 Nacos 控制台配置实例 metadata
 * weight: 80  # 权重值 0-100，默认 100
 * </pre>
 *
 * @author Claude
 * @since 2025-12-29
 */
@Slf4j
@Configuration
public class LoadBalancerConfiguration {

    /**
     * Round Robin 负载均衡器（默认策略）
     * <p>轮询选择实例，适用于实例性能相近的场景</p>
     */
    @Bean
    @ConditionalOnProperty(
        name = "spring.cloud.loadbalancer.strategy",
        havingValue = "round-robin",
        matchIfMissing = true
    )
    public ReactorLoadBalancer<ServiceInstance> roundRobinLoadBalancer(Environment environment,
            ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider) {

        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME, "default");
        log.info("Using RoundRobinLoadBalancer for service: {}", name);

        return new RoundRobinLoadBalancer(serviceInstanceListSupplierProvider, name);
    }

    /**
     * Random 负载均衡器
     * <p>随机选择实例，适用于快速分散请求的场景</p>
     */
    @Bean
    @ConditionalOnProperty(
        name = "spring.cloud.loadbalancer.strategy",
        havingValue = "random"
    )
    public ReactorLoadBalancer<ServiceInstance> randomLoadBalancer(Environment environment,
            ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider) {

        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME, "default");
        log.info("Using RandomLoadBalancer for service: {}", name);

        return new RandomLoadBalancer(serviceInstanceListSupplierProvider, name);
    }

    /**
     * Weighted Round Robin 负载均衡器（基于 Nacos 权重）
     * <p>根据实例权重分配请求，适用于实例性能差异较大的场景</p>
     *
     * <p>权重配置：
     * <ul>
     *   <li>在 Nacos 控制台设置实例 metadata: {@code weight=80}</li>
     *   <li>权重范围: 0-100，默认 100</li>
     *   <li>权重为 0 的实例不接收请求</li>
     * </ul>
     *
     * <p>注意：Spring Cloud LoadBalancer 2023.0.0+ 原生支持 Nacos 权重，
     * 只需配置 {@code spring.cloud.loadbalancer.nacos.enabled=true} 即可。
     * 本 Bean 使用 RoundRobinLoadBalancer 配合 Nacos 权重特性实现。
     */
    @Bean
    @ConditionalOnProperty(
        name = "spring.cloud.loadbalancer.strategy",
        havingValue = "weighted-round-robin"
    )
    public ReactorLoadBalancer<ServiceInstance> weightedRoundRobinLoadBalancer(Environment environment,
            ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider) {

        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME, "default");
        log.info("Using WeightedRoundRobinLoadBalancer (Nacos-based) for service: {}", name);

        // Spring Cloud Alibaba 的 NacosServiceInstanceListSupplier 会自动
        // 根据 Nacos 实例的 weight 字段过滤和加权实例列表
        return new RoundRobinLoadBalancer(serviceInstanceListSupplierProvider, name);
    }
}

package com.frog.common.rest.config;

import com.frog.common.rest.client.SysAuthServiceClient;
import com.frog.common.rest.client.SysPermissionServiceClient;
import com.frog.common.rest.client.SysUserServiceClient;
import com.frog.common.rest.interceptor.RestClientRequestSignatureInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.net.URI;

/**
 * RestClient + HttpExchange 核心配置
 * <p>替代 OpenFeign 配置</p>
 *
 * <p>功能：
 * <ul>
 *   <li>创建 RestClient Bean（包含 mTLS + 签名拦截器）</li>
 *   <li>创建 HttpServiceProxyFactory（用于 {@code @HttpExchange} 代理）</li>
 *   <li>集成 Nacos 服务发现（动态解析服务地址）</li>
 *   <li>注册 3 个客户端 Bean（UserServiceClient、AuthServiceClient、PermissionServiceClient）</li>
 * </ul>
 *
 * @author Claude
 * @since 2025-12-29
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RestClientHttpExchangeConfig {
    private final ClientHttpRequestFactory clientHttpRequestFactory;
    private final RestClientRequestSignatureInterceptor signatureInterceptor;
    private final LoadBalancerClient loadBalancerClient;

    /**
     * 创建基础 RestClient Bean
     * <p>包含 mTLS + 签名拦截器</p>
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder()
            .requestFactory(clientHttpRequestFactory)
            .requestInterceptor(signatureInterceptor);
    }

    /**
     * 创建 SysUserServiceClient Bean
     */
    @Bean
    public SysUserServiceClient sysUserServiceClient(RestClient.Builder restClientBuilder) {
        String baseUrl = resolveServiceUrl("user-service");
        log.info("Creating SysUserServiceClient with baseUrl: {}", baseUrl);

        RestClient restClient = restClientBuilder
            .baseUrl(baseUrl)
            .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build();

        return factory.createClient(SysUserServiceClient.class);
    }

    /**
     * 创建 SysAuthServiceClient Bean
     */
    @Bean
    public SysAuthServiceClient sysAuthServiceClient(RestClient.Builder restClientBuilder) {
        String baseUrl = resolveServiceUrl("auth-service");
        log.info("Creating SysAuthServiceClient with baseUrl: {}", baseUrl);

        RestClient restClient = restClientBuilder
            .baseUrl(baseUrl)
            .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build();

        return factory.createClient(SysAuthServiceClient.class);
    }

    /**
     * 创建 SysPermissionServiceClient Bean
     */
    @Bean
    public SysPermissionServiceClient sysPermissionServiceClient(RestClient.Builder restClientBuilder) {
        String baseUrl = resolveServiceUrl("permission-service");
        log.info("Creating SysPermissionServiceClient with baseUrl: {}", baseUrl);

        RestClient restClient = restClientBuilder
            .baseUrl(baseUrl)
            .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build();

        return factory.createClient(SysPermissionServiceClient.class);
    }

    /**
     * 从 Nacos 服务发现解析服务 URL（支持负载均衡）
     *
     * <p>解析策略：
     * <ol>
     *   <li>使用 Spring Cloud LoadBalancer 选择服务实例</li>
     *   <li>支持多种负载均衡策略（Round Robin、Random、Weighted 等）</li>
     *   <li>根据实例的 secure 标志选择 https/http</li>
     *   <li>如果解析失败，使用默认 URL（{@code http://serviceName}）</li>
     * </ol>
     *
     * <p>负载均衡策略由 Spring Cloud LoadBalancer 配置决定，默认为 Round Robin。
     * 可通过配置类自定义策略（如 RandomLoadBalancer、WeightedServiceInstanceListSupplier 等）。
     *
     * @param serviceName 服务名称（如 "user-service"）
     * @return 服务基础 URL（如 {@code https://192.168.1.100:8081}）
     */
    private String resolveServiceUrl(String serviceName) {
        try {
            // 使用 LoadBalancerClient 选择实例（自动负载均衡）
            // Note: 虽然当前版本不返回 null，但保留检查作为防御性编程
            ServiceInstance instance = loadBalancerClient.choose(serviceName);

            if (instance == null) {
                log.warn("No available instances for service '{}', using default URL", serviceName);
                return "http://" + serviceName;
            }

            URI uri = instance.getUri();
            log.debug("Resolved service '{}' to URL: {} (host: {}, metadata: {})",
                     serviceName, uri, instance.getHost(), instance.getMetadata());
            return uri.toString();

        } catch (Exception e) {
            log.error("Failed to resolve service '{}' via LoadBalancer: {}, using default URL",
                      serviceName, e.getMessage());
            return "http://" + serviceName;
        }
    }
}

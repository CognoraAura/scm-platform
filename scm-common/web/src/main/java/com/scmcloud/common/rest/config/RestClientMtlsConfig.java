package com.scmcloud.common.rest.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleKey;
import org.springframework.boot.ssl.jks.JksSslStoreDetails;
import org.springframework.boot.ssl.jks.JksSslStoreBundle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;

import javax.net.ssl.*;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * RestClient mTLS 配置 - 使用 Spring Boot 3.1+ SSL Bundles
 * 替代 OpenFeign �?FeignMtlsConfig
 *
 * <p>特性：
 * <ul>
 *   <li>基于 Spring Boot 4.0+ SSL Bundle 机制</li>
 *   <li>支持 mTLS 双向认证</li>
 *   <li>支持证书有效期监控和告警</li>
 *   <li>零停机证书热更新（通过 CertificateReloaderRestClient�?/li>
 * </ul>
 *
 * @author Claude
 * @since 2025-12-29
 */
@Slf4j
@Configuration
@Getter
public class RestClientMtlsConfig {

    @Value("${security.mtls.keystore-path:classpath:certificates/keystore.p12}")
    private Resource keystoreResource;

    @Value("${security.mtls.keystore-password:${KEYSTORE_PASSWORD:changeit}}")
    private String keystorePassword;

    @Value("${security.mtls.truststore-path:classpath:certificates/truststore.p12}")
    private Resource truststoreResource;

    @Value("${security.mtls.truststore-password:${TRUSTSTORE_PASSWORD:changeit}}")
    private String truststorePassword;

    @Value("${security.feign.app-id:${API_SECRET_INTERNAL_SERVICE:internal-service}}")
    private String appId;

    @Value("${security.feign.secret-key:${API_SECRET_INTERNAL_SECRET:your-internal-secret-key}}")
    private String secretKey;

    @Value("${spring.http.client.connect-timeout:10000}")
    private int connectTimeout;

    @Value("${spring.http.client.read-timeout:30000}")
    private int readTimeout;

    /**
     * 配置 SSL Bundle（推荐方式）
     * <p>使用 Spring Boot 4.0 �?SSL Bundle 机制，支持证书热更新</p>
     */
    @Bean
    public SslBundle sslBundle() throws Exception {
        log.info("Initializing SSL Bundle for mTLS...");

        // 加载 KeyStore �?TrustStore
        JksSslStoreDetails keystoreDetails = createStoreDetails(keystoreResource, keystorePassword);
        JksSslStoreDetails truststoreDetails = createStoreDetails(truststoreResource, truststorePassword);

        // 创建 SSL Store Bundle
        JksSslStoreBundle storeBundle = new JksSslStoreBundle(keystoreDetails, truststoreDetails);

        // 创建 SSL Bundle Key
        SslBundleKey key = SslBundleKey.of(keystorePassword, null);

        // 构建 SSL Bundle
        SslBundle bundle = SslBundle.of(storeBundle, key);

        log.info("SSL Bundle initialized successfully");
        return bundle;
    }

    /**
     * 创建 JKS Store Details
     */
    private JksSslStoreDetails createStoreDetails(Resource resource, String password) throws Exception {
        JksSslStoreDetails details = JksSslStoreDetails.forLocation(resource.getURL().toString());
        return new JksSslStoreDetails(
            details.type(),
            details.provider(),
            resource.getURL().toString(),
            password
        );
    }

    /**
     * 配置 ClientHttpRequestFactory（RestClient 使用�?
     * <p>使用 JDK HttpClient (Java 21+) 配合 SSL Bundle</p>
     */
    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory(SslBundle sslBundle) {
        log.info("Creating ClientHttpRequestFactory with mTLS support...");

        // �?SSL Bundle 创建 SSLContext
        SSLContext sslContext = sslBundle.createSslContext();

        // 使用 JDK 21 �?HttpClient（支持虚拟线程）
        HttpClient httpClient = HttpClient.newBuilder()
            .sslContext(sslContext)
            .connectTimeout(Duration.ofMillis(connectTimeout))
            .build();

        // 创建 JdkClientHttpRequestFactory
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(readTimeout));

        log.info("ClientHttpRequestFactory created: connectTimeout={}ms, readTimeout={}ms",
                 connectTimeout, readTimeout);
        return factory;
    }

    /**
     * 定时检查证书有效期（每天凌�?2 点）
     * <p>SECURITY: 提前 30 天告警证书即将过�?/p>
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void checkCertificateExpiry() {
        try {
            log.debug("Checking certificate expiry...");

            // 加载 KeyStore
            KeyStore keyStore = loadKeyStore(keystoreResource, keystorePassword);

            // 检查所有证�?
            var aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (keyStore.isCertificateEntry(alias) || keyStore.isKeyEntry(alias)) {
                    X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
                    if (cert != null) {
                        checkSingleCertificate(alias, cert);
                    }
                }
            }

            log.debug("Certificate expiry check completed");
        } catch (Exception e) {
            log.error("Failed to check certificate expiry: {}", e.getMessage(), e);
        }
    }

    /**
     * 检查单个证书有效期
     */
    private void checkSingleCertificate(String alias, X509Certificate cert) {
        Date notAfter = cert.getNotAfter();
        Instant expiryInstant = notAfter.toInstant();
        Instant now = Instant.now();
        Instant warningThreshold = now.plus(Duration.ofDays(30));

        if (expiryInstant.isBefore(now)) {
            log.error("SECURITY ALERT: Certificate '{}' has EXPIRED on {}", alias, notAfter);
        } else if (expiryInstant.isBefore(warningThreshold)) {
            long daysUntilExpiry = Duration.between(now, expiryInstant).toDays();
            log.warn("SECURITY WARNING: Certificate '{}' will expire in {} days ({})",
                     alias, daysUntilExpiry, notAfter);
        } else {
            long daysUntilExpiry = Duration.between(now, expiryInstant).toDays();
            log.info("Certificate '{}' is valid for {} more days (expires: {})",
                     alias, daysUntilExpiry, notAfter);
        }
    }

    /**
     * 加载 KeyStore
     */
    private KeyStore loadKeyStore(Resource resource, String password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream is = resource.getInputStream()) {
            keyStore.load(is, password.toCharArray());
        }
        return keyStore;
    }
}

package com.frog.gateway.config;

import com.frog.gateway.properties.ApiSignatureProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 用于 API 签名设置的配置绑定，并包含安全验证功能。
 * 遵循快速失败原则：如果缺少关键密钥，应用程序将无法启动。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(ApiSignatureProperties.class)
public class ApiSignatureConfiguration {
    private final ApiSignatureProperties properties;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @PostConstruct
    public void validateConfiguration() {
        if (!properties.isEnabled()) {
            log.warn("API signature verification is DISABLED - this should only happen in development!");
            return;
        }

        boolean isProduction = isProductionEnvironment();
        List<String> issues = new ArrayList<>();

        // Validate clock skew
        Duration skew = properties.getAllowedClockSkew();
        if (skew.toMillis() < 1000) {
            log.warn("allowedClockSkew is too small ({}ms), using default 1 minute", skew.toMillis());
            properties.setAllowedClockSkew(Duration.ofMinutes(1));
        }

        // Validate app secrets
        Map<String, String> appSecrets = properties.getAppSecrets();
        if (appSecrets == null || appSecrets.isEmpty()) {
            issues.add("No app secrets configured (security.signature.app-secrets)");
        } else {
            for (Map.Entry<String, String> entry : appSecrets.entrySet()) {
                String appId = entry.getKey();
                String secret = entry.getValue();

                if (!StringUtils.hasText(secret)) {
                    issues.add(String.format("Empty secret for app '%s'", appId));
                } else if (secret.length() < 32) {
                    issues.add(String.format("Secret for app '%s' is too weak (minimum 32 characters)", appId));
                }
            }
        }

        if (!issues.isEmpty()) {
            String message = """
                API Signature Configuration Issues:
                  - %s

                REQUIRED: Set secrets via environment variables:
                  export API_SECRET_WEB_APP='<secure-random-string-min-32-chars>'
                  export API_SECRET_INTERNAL_SERVICE='<secure-random-string-min-32-chars>'

                Generate secure secrets:
                  openssl rand -base64 48
                  or use password manager
                """.formatted(String.join("\n  - ", issues));

            if (isProduction) {
                log.error("CRITICAL: {}", message);
                throw new IllegalStateException("Invalid API signature configuration in production");
            } else {
                log.warn("\n{}\n", message);
            }
        } else {
            log.info("✓ API signature configuration validated ({} app(s) configured)",
                     appSecrets.size());
        }
    }

    private boolean isProductionEnvironment() {
        return activeProfile != null && (activeProfile.contains("prod") || activeProfile.contains("production") ||
                activeProfile.contains("staging"));
    }
}

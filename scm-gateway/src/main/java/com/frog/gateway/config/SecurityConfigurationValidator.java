package com.frog.gateway.config;

import com.frog.gateway.properties.ApiSignatureProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 安全配置验证器
 * 确保在应用程序启动前所有关键安全配置都已正确设置。
 * 遵循 Google/Netflix 的最佳实践：如果安全配置缺失，则快速失败。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityConfigurationValidator {
    private final ApiSignatureProperties signatureProperties;

    @Value("${security.signature.app-secrets.web-app:}")
    private String webAppSecret;

    @Value("${security.signature.app-secrets.internal-service:}")
    private String internalServiceSecret;

    @Value("${security.identity.signature-secret:}")
    private String identitySignatureSecret;

    @Value("${security.mtls.keystore-password:}")
    private String keystorePassword;

    @Value("${security.mtls.truststore-password:}")
    private String truststorePassword;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * 在启动时验证所有关键安全配置。
     * 如果在生产环境中缺少任何必需的配置，应用程序将无法启动。
     */
    @PostConstruct
    public void validateSecurityConfiguration() {
        List<String> missingConfigs = new ArrayList<>();

        boolean isProductionLike = isProductionLikeEnvironment();

        log.info("Validating security configuration (profile: {}, strict mode: {})",
                activeProfile, isProductionLike);

        if (!StringUtils.hasText(webAppSecret)) {
            String error = "security.signature.app-secrets.web-app (env: API_SECRET_WEB_APP)";
            missingConfigs.add(error);
        }

        if (!StringUtils.hasText(internalServiceSecret)) {
            String error = "security.signature.app-secrets.internal-service (env: API_SECRET_INTERNAL_SERVICE)";
            missingConfigs.add(error);
        }

        if (!StringUtils.hasText(identitySignatureSecret)) {
            String error = "security.identity.signature-secret (env: IDENTITY_SIGNATURE_SECRET)";
            missingConfigs.add(error);
        }

        if (!StringUtils.hasText(keystorePassword)) {
            String error = "security.mtls.keystore-password (env: KEYSTORE_PASSWORD)";
            missingConfigs.add(error);
        }

        if (!StringUtils.hasText(truststorePassword)) {
            String error = "security.mtls.truststore-password (env: TRUSTSTORE_PASSWORD)";
            missingConfigs.add(error);
        }

        if (!missingConfigs.isEmpty()) {
            String errorMessage = buildErrorMessage(missingConfigs);

            if (isProductionLike) {
                log.error("CRITICAL SECURITY ERROR: {}", errorMessage);
                throw new IllegalStateException(errorMessage);
            } else {
                String warningMessage = """

                    ================================================================================
                      SECURITY WARNING: Missing configuration (acceptable in dev mode)
                      {}
                      These MUST be set via environment variables in production!
                    ================================================================================
                    """;
                log.warn(warningMessage, errorMessage);
            }
        } else {
            log.info("✓ All critical security configurations are properly set");

            if (isProductionLike) {
                validateSecretStrength();
            }
        }

        // Validate clock skew configuration consistency
        validateClockSkewConfiguration();
    }

    /**
     * 验证密码强度是否符合要求。
     */
    private void validateSecretStrength() {
        List<String> weakSecrets = new ArrayList<>();

        if (webAppSecret.length() < 32) {
            weakSecrets.add("web-app secret is too short (minimum 32 characters)");
        }

        if (internalServiceSecret.length() < 32) {
            weakSecrets.add("internal-service secret is too short (minimum 32 characters)");
        }

        if (identitySignatureSecret.length() < 64) {
            weakSecrets.add("identity signature secret is too short (minimum 64 characters for HMAC-SHA256)");
        }

        if (!weakSecrets.isEmpty()) {
            log.warn("SECURITY WARNING: Weak secrets detected:\n  - {}",
                    String.join("\n  - ", weakSecrets));
        }
    }

    /**
     * 确定当前环境是否需要严格的安全验证。
     */
    private boolean isProductionLikeEnvironment() {
        return activeProfile != null &&
                (activeProfile.contains("prod") ||
                        activeProfile.contains("production") ||
                        activeProfile.contains("staging") ||
                        activeProfile.contains("uat"));
    }

    /**
     * 验证时钟偏移配置的一致性。
     * nonceTtl 必须 >= allowedClockSkew，否则可能导致重放攻击。
     */
    private void validateClockSkewConfiguration() {
        Duration nonceTtl = signatureProperties.getNonceTtl();
        Duration allowedClockSkew = signatureProperties.getAllowedClockSkew();

        if (nonceTtl.compareTo(allowedClockSkew) < 0) {
            String errorMessage = String.format(
                "CRITICAL CONFIGURATION ERROR: nonceTtl (%s) must be >= allowedClockSkew (%s) " +
                "to prevent replay attacks. Current configuration allows requests within %s window " +
                "but only protects against replay for %s.",
                nonceTtl, allowedClockSkew, allowedClockSkew, nonceTtl
            );

            if (isProductionLikeEnvironment()) {
                log.error(errorMessage);
                throw new IllegalStateException(errorMessage);
            } else {
                log.warn("⚠️  {}", errorMessage);
            }
        } else {
            log.info("✓ Clock skew configuration validated: nonceTtl={}, allowedClockSkew={}",
                    nonceTtl, allowedClockSkew);
        }
    }

    /**
     * 生成针对配置缺失的详细错误消息。
     */
    private String buildErrorMessage(List<String> missingConfigs) {
        return """
            Missing %d required security configuration(s):
              - %s

            SOLUTION: Set these via environment variables:
              export API_SECRET_WEB_APP='your-secret-here'
              export API_SECRET_INTERNAL_SERVICE='your-secret-here'
              export IDENTITY_SIGNATURE_SECRET='your-secret-here'
              export KEYSTORE_PASSWORD='your-password-here'
              export TRUSTSTORE_PASSWORD='your-password-here'

            For production deployment, use HashiCorp Vault, AWS Secrets Manager, or equivalent.
            """.formatted(missingConfigs.size(), String.join("\n  - ", missingConfigs));
    }
}

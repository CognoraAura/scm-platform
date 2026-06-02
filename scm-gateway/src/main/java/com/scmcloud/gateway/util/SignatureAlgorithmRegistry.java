package com.scmcloud.gateway.util;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 签名算法版本控制
 *
 * @author Deng
 * createData 2025/11/11 9:18
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SignatureAlgorithmRegistry {
    private final List<SignatureAlgorithm> algorithmList;
    private final Map<String, SignatureAlgorithm> algorithms = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 自动注册所有实现类
        for (SignatureAlgorithm algorithm : algorithmList) {
            algorithms.put(algorithm.version(), algorithm);
            log.info("Registered signature algorithm: {}", algorithm.version());
        }

        if (algorithms.isEmpty()) {
            throw new IllegalStateException("No signature algorithms registered");
        }

        // 验证默认算法是否存在（启动时快速失败）
        if (!algorithms.containsKey("HMAC-SHA256-V1")) {
            throw new IllegalStateException(
                "Default signature algorithm 'HMAC-SHA256-V1' not registered. " +
                "Available algorithms: " + algorithms.keySet()
            );
        }

        log.info("�Default signature algorithm 'HMAC-SHA256-V1' validated successfully");
    }

    public SignatureAlgorithm getAlgorithm(String version) {
        SignatureAlgorithm algorithm = algorithms.get(version);
        if (algorithm != null) {
            return algorithm;
        }

        // 回退到默认版�
        SignatureAlgorithm defaultAlgorithm = algorithms.get("HMAC-SHA256-V1");
        if (defaultAlgorithm == null) {
            throw new IllegalStateException("Default signature algorithm 'HMAC-SHA256-V1' not registered");
        }
        log.warn("Unknown signature version '{}', falling back to HMAC-SHA256-V1", version);

        return defaultAlgorithm;
    }
}

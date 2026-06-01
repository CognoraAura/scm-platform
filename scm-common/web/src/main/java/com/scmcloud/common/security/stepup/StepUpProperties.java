package com.scmcloud.common.security.stepup;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "security.stepup")
@Data
public class StepUpProperties {
    private boolean enabled = true;
    // 工作时间窗口（含�?
    private int businessStartHour = 9;   // 09:00
    private int businessEndHour = 18;    // 18:00
    // 是否启用新设备触�?
    private boolean newDeviceTrigger = true;
    // 策略文件路径（可选）：优先该路径，其次classpath:security/stepup-policy.yaml，最后docs/security/stepup-policy.yaml
    private String policyPath;
    // 策略缓存刷新秒数（TTL�?
    private int refreshSeconds = 60;
    // Step-up 白名单与旁路配置
    private List<String> whitelistPaths = new ArrayList<>();
    private List<String> bypassPaths = new ArrayList<>();
    private List<String> bypassRoles = new ArrayList<>();
    private List<String> bypassPermissions = new ArrayList<>();
    private List<String> bypassUsers = new ArrayList<>();
    private CircuitBreaker circuitBreaker = new CircuitBreaker();

    @Data
    public static class CircuitBreaker {
        private boolean enabled = true;
        private int failureThreshold = 3;
        private int openSeconds = 60;
        private boolean bypassOnOpen = true;
        private boolean forceOpen = false;
    }
}

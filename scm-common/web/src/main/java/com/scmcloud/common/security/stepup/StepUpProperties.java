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
    // 宸ヤ綔鏃堕棿绐楀彛锛堝惈锟?
    private int businessStartHour = 9;   // 09:00
    private int businessEndHour = 18;    // 18:00
    // 鏄惁鍚敤鏂拌澶囪Е锟?
    private boolean newDeviceTrigger = true;
    // 绛栫暐鏂囦欢璺緞锛堝彲閫夛級锛氫紭鍏堣璺緞锛屽叾娆lasspath:security/stepup-policy.yaml锛屾渶鍚巇ocs/security/stepup-policy.yaml
    private String policyPath;
    // 绛栫暐缂撳瓨鍒锋柊绉掓暟锛圱TL锟?
    private int refreshSeconds = 60;
    // Step-up 鐧藉悕鍗曚笌鏃佽矾閰嶇疆
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

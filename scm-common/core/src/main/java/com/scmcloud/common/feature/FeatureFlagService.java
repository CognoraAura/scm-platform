package com.scmcloud.common.feature;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class FeatureFlagService {

    private final Map<String, Boolean> flags = new ConcurrentHashMap<>();

    @Value("${feature-flags.default:false}")
    private boolean defaultFlag;

    public FeatureFlagService(@Value("${feature-flags:}") Map<String, Boolean> configFlags) {
        if (configFlags != null) {
            this.flags.putAll(configFlags);
        }
        log.info("Feature flags initialized: {}", flags);
    }

    public boolean isEnabled(String flagName) {
        return flags.getOrDefault(flagName, defaultFlag);
    }

    public void setFlag(String flagName, boolean enabled) {
        flags.put(flagName, enabled);
    }
}
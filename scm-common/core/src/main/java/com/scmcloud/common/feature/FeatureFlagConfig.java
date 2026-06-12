package com.scmcloud.common.feature;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class FeatureFlagConfig {

    private final FeatureFlagService featureFlagService;

    public FeatureFlagConfig(FeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
    }

    @Around("@annotation(featureFlag)")
    public Object around(ProceedingJoinPoint joinPoint, FeatureFlag featureFlag) throws Throwable {
        boolean enabled = featureFlagService.isEnabled(featureFlag.value());
        if (featureFlag.negate()) {
            enabled = !enabled;
        }
        if (!enabled) {
            throw new FeatureDisabledException("Feature '" + featureFlag.value() + "' is disabled");
        }
        return joinPoint.proceed();
    }

    public static class FeatureDisabledException extends RuntimeException {
        public FeatureDisabledException(String message) {
            super(message);
        }
    }
}
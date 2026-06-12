package com.scmcloud.common.health;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class CompositeHealthIndicator implements HealthIndicator {

    private final HealthIndicator dbHealthIndicator;
    private final HealthIndicator redisHealthIndicator;

    public CompositeHealthIndicator(
            @Qualifier("dataSourceHealthIndicator") HealthIndicator dbHealthIndicator,
            @Qualifier("redisHealthIndicator") HealthIndicator redisHealthIndicator) {
        this.dbHealthIndicator = dbHealthIndicator;
        this.redisHealthIndicator = redisHealthIndicator;
    }

    @Override
    public Health health() {
        Health dbHealth = dbHealthIndicator.health();
        Health redisHealth = redisHealthIndicator.health();

        if (dbHealth.getStatus().equals(org.springframework.boot.actuate.health.Status.UP)
                && redisHealth.getStatus().equals(org.springframework.boot.actuate.health.Status.UP)) {
            return Health.up()
                    .withDetail("database", dbHealth.getDetails())
                    .withDetail("redis", redisHealth.getDetails())
                    .build();
        }

        return Health.down()
                .withDetail("database", dbHealth.getDetails())
                .withDetail("redis", redisHealth.getDetails())
                .build();
    }
}

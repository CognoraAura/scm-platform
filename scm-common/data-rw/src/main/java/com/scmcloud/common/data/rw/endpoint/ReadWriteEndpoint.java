package com.scmcloud.common.data.rw.endpoint;

import com.scmcloud.common.data.rw.config.ReadWriteAutoConfiguration;
import com.scmcloud.common.data.rw.health.SlaveHealthChecker;
import com.scmcloud.common.data.rw.routing.ReadWriteRoutingDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 璇诲啓鍒嗙绠＄悊绔偣
 * <p>
 * 鎻愪緵杩愯鏃剁鐞嗚兘鍔涳細
 * - 鏌ョ湅浠庡簱鐘讹拷
 * - 鎵嬪姩鎽橀櫎/鎭㈠浠庡簱
 * - 鏌ョ湅鍋ュ悍淇℃伅
 * <p>
 * 璁块棶璺緞锟絘ctuator/readwrite
 * <p>
 * 娉ㄦ剰锛氶渶锟絪pring-boot-starter-actuator 渚濊禆
 *
 * @author Deng
 * @since 2025-12-16
 */
@Slf4j
@Endpoint(id = "readwrite")
@RequiredArgsConstructor
public class ReadWriteEndpoint {
    private final ReadWriteAutoConfiguration.ReadWriteDataSourceProvider dataSourceProvider;
    private final SlaveHealthChecker healthChecker;

    /**
     * 鑾峰彇鎵€鏈夎鍐欏垎绂荤姸锟?
     * <p>
     * GET /actuator/readwrite
     */
    @ReadOperation
    public Map<String, Object> status() {
        Map<String, Object> result = new HashMap<>();

        Set<String> groups = dataSourceProvider.getGroupNames();
        result.put("groups", groups);

        Map<String, Object> groupDetails = new HashMap<>();
        for (String groupName : groups) {
            groupDetails.put(groupName, buildGroupInfo(groupName));
        }

        result.put("details", groupDetails);
        return result;
    }

    /**
     * 鑾峰彇鎸囧畾缁勭殑鐘讹拷
     * <p>
     * GET /actuator/readwrite/{groupName}
     */
    @ReadOperation
    public Map<String, Object> statusByGroup(@Selector String groupName) {
        Map<String, Object> result = new HashMap<>();

        try {
            result.put("group", groupName);
            result.putAll(buildGroupInfo(groupName));
        } catch (IllegalArgumentException e) {
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * 鎵嬪姩鎽橀櫎浠庡簱
     * <p>
     * POST /actuator/readwrite with {"groupName": "xxx", "slaveName": "xxx", "action": "markUnavailable"}
     */
    @WriteOperation
    public Map<String, Object> operate(String groupName, String slaveName, String action) {
        if ("markAvailable".equalsIgnoreCase(action)) {
            return doMarkAvailable(groupName, slaveName);
        } else {
            return doMarkUnavailable(groupName, slaveName);
        }
    }

    /**
     * 鏋勫缓缁勪俊锟?
     */
    private Map<String, Object> buildGroupInfo(String groupName) {
        Map<String, Object> groupInfo = new HashMap<>();

        ReadWriteRoutingDataSource ds = dataSourceProvider.getDataSource(groupName);
        groupInfo.put("slaveAvailability", ds.getSlaveAvailability());
        groupInfo.put("healthStatus", buildHealthDetails(groupName));

        return groupInfo;
    }

    /**
     * 鏋勫缓鍋ュ悍璇︽儏
     */
    private Map<String, Object> buildHealthDetails(String groupName) {
        Map<String, Object> healthDetails = new HashMap<>();
        Map<String, SlaveHealthChecker.HealthStatus> allStatus = healthChecker.getAllHealthStatus();

        String prefix = groupName + ".";
        for (Map.Entry<String, SlaveHealthChecker.HealthStatus> entry : allStatus.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                String slaveName = entry.getKey().substring(prefix.length());
                SlaveHealthChecker.HealthStatus status = entry.getValue();
                healthDetails.put(slaveName, Map.of(
                        "available", status.available(),
                        "replicationLagMs", status.replicationLagMs(),
                        "consecutiveFailures", status.consecutiveFailures()
                ));
            }
        }

        return healthDetails;
    }

    private Map<String, Object> doMarkUnavailable(String groupName, String slaveName) {
        Map<String, Object> result = new HashMap<>();

        try {
            ReadWriteRoutingDataSource ds = dataSourceProvider.getDataSource(groupName);
            ds.markSlaveUnavailable(slaveName);

            log.warn("[RW-Endpoint] Manually marked slave [{}] in group [{}] as UNAVAILABLE",
                    slaveName, groupName);

            result.put("success", true);
            result.put("message", String.format("Slave [%s.%s] marked as unavailable", groupName, slaveName));
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    private Map<String, Object> doMarkAvailable(String groupName, String slaveName) {
        Map<String, Object> result = new HashMap<>();

        try {
            ReadWriteRoutingDataSource ds = dataSourceProvider.getDataSource(groupName);
            ds.markSlaveAvailable(slaveName);

            log.info("[RW-Endpoint] Manually marked slave [{}] in group [{}] as AVAILABLE",
                    slaveName, groupName);

            result.put("success", true);
            result.put("message", String.format("Slave [%s.%s] marked as available", groupName, slaveName));
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }
}

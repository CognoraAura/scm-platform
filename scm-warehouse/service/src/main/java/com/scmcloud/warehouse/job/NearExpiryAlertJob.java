package com.scmcloud.warehouse.job;

import com.scmcloud.warehouse.service.InventoryBatchService;
import com.scmcloud.warehouse.service.NotificationService;
import com.scmcloud.warehouse.vo.NearExpiryProductVO;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 临期库存告警定时任务
 *
 * 执行时间：每日上�?09:00（cron: 0 0 9 * * ?�?
 *
 * 功能�?
 * 1. 扫描所有租户的临期库存（基�?v_near_expiry_inventory 视图�?
 * 2. 临期标准�?
 *    - 距离过期 <= 30天：一级告警（严重�?
 *    - 距离过期 31-60天：二级告警（警告）
 *    - 距离过期 61-90天：三级告警（提示）
 * 3. 发送告警通知�?
 *    - 站内消息通知
 *    - 邮件通知（可选）
 *    - 企业微信/钉钉通知（可选）
 * 4. 记录告警历史，避免重复发�?
 *
 * XXL-Job 配置示例�?
 * - 执行器：scm-warehouse-executor
 * - JobHandler：nearExpiryAlertJob
 * - Cron�? 0 9 * * ?
 * - 运行模式：BEAN
 * - 阻塞处理策略：单机串�?
 * - 路由策略：轮�?
 *
 * @author Claude Code
 * @since 2025-01-24
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NearExpiryAlertJob {

    private final InventoryBatchService inventoryBatchService;
    private final NotificationService notificationService;

    /**
     * 执行临期库存告警
     *
     * 任务参数（可选）�?
     * - tenantId: 指定租户ID（UUID格式），不传则扫描所有租�?
     * - alertLevel: 告警级别（CRITICAL, WARNING, INFO），不传则发送所有级�?
     *
     * 示例�?
     * - 扫描所有租户：不传参数
     * - 扫描单个租户：传�?"123e4567-e89b-12d3-a456-426614174000"
     * - 只发送严重告警：传参 "alertLevel=CRITICAL"
     */
    @XxlJob("nearExpiryAlertJob")
    public void execute() {
        long startTime = System.currentTimeMillis();
        String param = XxlJobHelper.getJobParam();

        try {
            // 解析参数
            UUID tenantId = null;
            AlertLevel alertLevel = null;

            if (param != null && !param.trim().isEmpty()) {
                String[] parts = param.split(",");
                for (String part : parts) {
                    part = part.trim();
                    if (part.startsWith("alertLevel=")) {
                        try {
                            alertLevel = AlertLevel.valueOf(part.substring(11).toUpperCase());
                        } catch (IllegalArgumentException e) {
                            log.warn("无效的告警级别参�? {}", part);
                        }
                    } else {
                        // 尝试解析为租户ID
                        try {
                            tenantId = UUID.fromString(part);
                        } catch (IllegalArgumentException e) {
                            log.warn("无法解析租户ID: {}", part);
                        }
                    }
                }
            }

            String scope = tenantId == null ? "所有租�? : "租户 " + tenantId;
            log.info("开始扫描临期库存，范围: {}, 告警级别: {}", scope, alertLevel == null ? "ALL" : alertLevel);

            // 查询临期库存
            List<NearExpiryProductVO> nearExpiryProducts = inventoryBatchService.getNearExpiryProducts(
                tenantId,
                alertLevel
            );

            if (nearExpiryProducts.isEmpty()) {
                String msg = String.format("未发现临期库存，范围: %s", scope);
                log.info(msg);
                XxlJobHelper.handleSuccess(msg);
                return;
            }

            // 按租户分�?
            Map<UUID, List<NearExpiryProductVO>> groupedByTenant = nearExpiryProducts.stream()
                .collect(Collectors.groupingBy(NearExpiryProductVO::getTenantId));

            int totalAlerts = 0;
            int successCount = 0;
            int failCount = 0;

            // 按租户发送告�?
            for (Map.Entry<UUID, List<NearExpiryProductVO>> entry : groupedByTenant.entrySet()) {
                UUID currentTenantId = entry.getKey();
                List<NearExpiryProductVO> products = entry.getValue();

                try {
                    // 按告警级别统�?
                    Map<AlertLevel, Long> levelCounts = products.stream()
                        .collect(Collectors.groupingBy(
                            p -> AlertLevel.valueOf(p.getAlertLevel()),
                            Collectors.counting()
                        ));

                    // 发送告警通知
                    notificationService.sendNearExpiryAlert(currentTenantId, products, levelCounts);

                    log.info("租户 {} 临期告警已发送，�?{} 条（严重: {}, 警告: {}, 提示: {}�?,
                        currentTenantId,
                        products.size(),
                        levelCounts.getOrDefault(AlertLevel.CRITICAL, 0L),
                        levelCounts.getOrDefault(AlertLevel.WARNING, 0L),
                        levelCounts.getOrDefault(AlertLevel.INFO, 0L)
                    );

                    totalAlerts += products.size();
                    successCount++;

                } catch (Exception e) {
                    log.error("租户 {} 临期告警发送失�?, currentTenantId, e);
                    failCount++;
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            String successMsg = String.format(
                "临期库存告警完成，范�? %s, 总告警数: %d, 成功租户: %d, 失败租户: %d, 耗时: %d ms",
                scope,
                totalAlerts,
                successCount,
                failCount,
                duration
            );

            log.info(successMsg);

            if (failCount > 0) {
                XxlJobHelper.handleFail(successMsg);
            } else {
                XxlJobHelper.handleSuccess(successMsg);
            }

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            String errorMsg = String.format("临期库存告警失败，耗时: %d ms", duration);
            log.error(errorMsg, e);
            XxlJobHelper.handleFail(errorMsg + ": " + e.getMessage());
        }
    }

    /**
     * 告警级别枚举
     */
    public enum AlertLevel {
        /**
         * 严重告警�?= 30天）
         */
        CRITICAL,

        /**
         * 警告�?1-60天）
         */
        WARNING,

        /**
         * 提示�?1-90天）
         */
        INFO
    }
}
package com.scmcloud.common.partition;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库分区管理定时任�?

 * 执行时间：每�?1 日凌�?01:00（cron: 0 0 1 1 * ?�?

 * 功能�?
 * 1. 为下个月创建新的分区表（提前创建，避免月末插入失败）
 * 2. 清理过期分区（保留近 24 个月，超过则 DETACH 归档�?
 * 3. 支持的分区表�?
 *    - ord_order (订单表，�?order_time 分区)
 *    - ord_payment (支付记录，按 payment_time 分区)
 *    - ord_refund (退款记录，�?refund_time 分区)
 *    - inv_reservation (库存预留，按 create_time 分区)
 *    - inv_log (库存日志，按 create_time 分区)
 *    - inv_batch_flow (批次流水，按 create_time 分区)
 *    - sup_purchase_order (采购订单，按 order_time 分区)
 *    - tenant_operation_log (租户操作日志，按 create_time 分区)
 *    - payment_record (财务支付记录，按 payment_time 分区)

 * XXL-Job 配置示例�?
 * - 执行器：scm-common-executor
 * - JobHandler：partitionManagementJob
 * - Cron�? 0 1 1 * ?
 * - 运行模式：BEAN
 * - 阻塞处理策略：单机串�?
 * - 路由策略：第一�?
 *
 * @author Claude Code
 * @since 2025-01-24
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PartitionManagementJob {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 需要管理的分区表配�?
     */
    private static final List<PartitionTable> PARTITION_TABLES = List.of(
        new PartitionTable("ord_order", "order_time"),
        new PartitionTable("ord_payment", "payment_time"),
        new PartitionTable("ord_refund", "refund_time"),
        new PartitionTable("inv_reservation", "create_time"),
        new PartitionTable("inv_log", "create_time"),
        new PartitionTable("inv_batch_flow", "create_time"),
        new PartitionTable("sup_purchase_order", "order_time"),
        new PartitionTable("tenant_operation_log", "create_time"),
        new PartitionTable("payment_record", "payment_time")
    );

    /**
     * 保留分区的月数（24个月 = 2年）
     */
    private static final int RETENTION_MONTHS = 24;

    /**
     * 执行分区管理任务
     */
    @XxlJob("partitionManagementJob")
    public void execute() {
        long startTime = System.currentTimeMillis();
        List<String> results = new ArrayList<>();

        try {
            log.info("开始执行分区管理任�?);

            // 1. 为下个月创建新分�?
            YearMonth nextMonth = YearMonth.now().plusMonths(1);
            int createdCount = createPartitionsForMonth(nextMonth);
            results.add(String.format("创建下月分区: %d �?, createdCount));

            // 2. 清理过期分区
            YearMonth cutoffMonth = YearMonth.now().minusMonths(RETENTION_MONTHS);
            int detachedCount = detachExpiredPartitions(cutoffMonth);
            results.add(String.format("归档过期分区: %d �?, detachedCount));

            long duration = System.currentTimeMillis() - startTime;
            String successMsg = String.format("分区管理完成�?s，耗时: %d ms",
                String.join(", ", results), duration);

            log.info(successMsg);
            XxlJobHelper.handleSuccess(successMsg);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            String errorMsg = String.format("分区管理失败，耗时: %d ms", duration);
            log.error(errorMsg, e);
            XxlJobHelper.handleFail(errorMsg + ": " + e.getMessage());
        }
    }

    /**
     * 为指定月份创建分�?
     */
    private int createPartitionsForMonth(YearMonth yearMonth) {
        int count = 0;
        String partitionSuffix = yearMonth.format(DateTimeFormatter.ofPattern("yyyyMM"));

        for (PartitionTable table : PARTITION_TABLES) {
            try {
                String partitionName = table.tableName + "_" + partitionSuffix;

                // 检查分区是否已存在
                String checkSql = """
                    SELECT COUNT(*) FROM pg_tables
                    WHERE schemaname = 'public' AND tablename = ?
                    """;
                Integer exists = jdbcTemplate.queryForObject(checkSql, Integer.class, partitionName);

                if (exists != null && exists > 0) {
                    log.debug("分区 {} 已存在，跳过创建", partitionName);
                    continue;
                }

                // 计算分区范围
                LocalDate startDate = yearMonth.atDay(1);
                LocalDate endDate = yearMonth.atEndOfMonth().plusDays(1);

                // 创建分区�?
                String createSql = String.format("""
                    CREATE TABLE IF NOT EXISTS %s PARTITION OF %s
                    FOR VALUES FROM ('%s') TO ('%s')
                    """,
                    partitionName,
                    table.tableName,
                    startDate,
                    endDate
                );

                jdbcTemplate.execute(createSql);
                log.info("成功创建分区: {}", partitionName);
                count++;

            } catch (Exception e) {
                log.error("创建分区失败: {}.{}", table.tableName, partitionSuffix, e);
            }
        }

        return count;
    }

    /**
     * 分离过期分区（归档）
     */
    private int detachExpiredPartitions(YearMonth cutoffMonth) {
        int count = 0;

        for (PartitionTable table : PARTITION_TABLES) {
            try {
                // 查询所有分�?
                String querySql = """
                    SELECT tablename FROM pg_tables
                    WHERE schemaname = 'public'
                      AND tablename LIKE ?
                    ORDER BY tablename
                    """;
                List<String> partitions = jdbcTemplate.queryForList(
                    querySql,
                    String.class,
                    table.tableName + "_%"
                );

                for (String partition : partitions) {
                    // 从分区名提取年月 (例如: ord_order_202401 -> 202401)
                    String suffix = partition.substring(table.tableName.length() + 1);
                    try {
                        YearMonth partitionMonth = YearMonth.parse(suffix, DateTimeFormatter.ofPattern("yyyyMM"));

                        if (partitionMonth.isBefore(cutoffMonth)) {
                            // DETACH 分区（不删除数据，只是从主表分离�?
                            String detachSql = String.format(
                                "ALTER TABLE %s DETACH PARTITION %s",
                                table.tableName,
                                partition
                            );
                            jdbcTemplate.execute(detachSql);
                            log.info("成功归档分区: {}", partition);
                            count++;
                        }
                    } catch (Exception e) {
                        log.warn("无法解析分区�? {}", partition);
                    }
                }

            } catch (Exception e) {
                log.error("归档分区失败: {}", table.tableName, e);
            }
        }

        return count;
    }

    /**
     * 分区表配�?
     */
    private record PartitionTable(String tableName, String partitionColumn) {
    }
}
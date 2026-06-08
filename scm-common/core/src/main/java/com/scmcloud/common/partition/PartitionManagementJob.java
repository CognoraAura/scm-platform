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
 * 鏁版嵁搴撳垎鍖虹鐞嗗畾鏃朵换锟?

 * 鎵ц鏃堕棿锛氭瘡锟? 鏃ュ噷锟?1:00锛坈ron: 0 0 1 1 * ?锟?

 * 鍔熻兘锟?
 * 1. 涓轰笅涓湀鍒涘缓鏂扮殑鍒嗗尯琛紙鎻愬墠鍒涘缓锛岄伩鍏嶆湀鏈彃鍏ュけ璐ワ級
 * 2. 娓呯悊杩囨湡鍒嗗尯锛堜繚鐣欒繎 24 涓湀锛岃秴杩囧垯 DETACH 褰掓。锟?
 * 3. 鏀寔鐨勫垎鍖鸿〃锟?
 *    - ord_order (璁㈠崟琛紝锟給rder_time 鍒嗗尯)
 *    - ord_payment (鏀粯璁板綍锛屾寜 payment_time 鍒嗗尯)
 *    - ord_refund (閫€娆捐褰曪紝锟絩efund_time 鍒嗗尯)
 *    - inv_reservation (搴撳瓨棰勭暀锛屾寜 create_time 鍒嗗尯)
 *    - inv_log (搴撳瓨鏃ュ織锛屾寜 create_time 鍒嗗尯)
 *    - inv_batch_flow (鎵规娴佹按锛屾寜 create_time 鍒嗗尯)
 *    - sup_purchase_order (閲囪喘璁㈠崟锛屾寜 order_time 鍒嗗尯)
 *    - tenant_operation_log (绉熸埛鎿嶄綔鏃ュ織锛屾寜 create_time 鍒嗗尯)
 *    - payment_record (璐㈠姟鏀粯璁板綍锛屾寜 payment_time 鍒嗗尯)

 * XXL-Job 閰嶇疆绀轰緥锟?
 * - 鎵ц鍣細scm-common-executor
 * - JobHandler锛歱artitionManagementJob
 * - Cron锟?0 1 1 * ?
 * - 杩愯妯″紡锛欱EAN
 * - 闃诲澶勭悊绛栫暐锛氬崟鏈轰覆锟?
 * - 璺敱绛栫暐锛氱涓€锟?
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
     * 闇€瑕佺鐞嗙殑鍒嗗尯琛ㄩ厤锟?
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
     * 淇濈暀鍒嗗尯鐨勬湀鏁帮紙24涓湀 = 2骞达級
     */
    private static final int RETENTION_MONTHS = 24;

    /**
     * 鎵ц鍒嗗尯绠＄悊浠诲姟
     */
    @XxlJob("partitionManagementJob")
    public void execute() {
        long startTime = System.currentTimeMillis();
        List<String> results = new ArrayList<>();

        try {
            log.info("寮€濮嬫墽琛屽垎鍖虹鐞嗕换鍔?);

            // 1. 涓轰笅涓湀鍒涘缓鏂板垎锟?
            YearMonth nextMonth = YearMonth.now().plusMonths(1);
            int createdCount = createPartitionsForMonth(nextMonth);
            results.add(String.format("鍒涘缓涓嬫湀鍒嗗尯: %d 涓?, createdCount));

            // 2. 娓呯悊杩囨湡鍒嗗尯
            YearMonth cutoffMonth = YearMonth.now().minusMonths(RETENTION_MONTHS);
            int detachedCount = detachExpiredPartitions(cutoffMonth);
            results.add(String.format("褰掓。杩囨湡鍒嗗尯: %d 涓?, detachedCount));

            long duration = System.currentTimeMillis() - startTime;
            String successMsg = String.format("鍒嗗尯绠＄悊瀹屾垚锟絪锛岃€楁椂: %d ms",
                String.join(", ", results), duration);

            log.info(successMsg);
            XxlJobHelper.handleSuccess(successMsg);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            String errorMsg = String.format("鍒嗗尯绠＄悊澶辫触锛岃€楁椂: %d ms", duration);
            log.error(errorMsg, e);
            XxlJobHelper.handleFail(errorMsg + ": " + e.getMessage());
        }
    }

    /**
     * 涓烘寚瀹氭湀浠藉垱寤哄垎锟?
     */
    private int createPartitionsForMonth(YearMonth yearMonth) {
        int count = 0;
        String partitionSuffix = yearMonth.format(DateTimeFormatter.ofPattern("yyyyMM"));

        for (PartitionTable table : PARTITION_TABLES) {
            try {
                String partitionName = table.tableName + "_" + partitionSuffix;

                // 妫€鏌ュ垎鍖烘槸鍚﹀凡瀛樺湪
                String checkSql = """
                    SELECT COUNT(*) FROM pg_tables
                    WHERE schemaname = 'public' AND tablename = ?
                    """;
                Integer exists = jdbcTemplate.queryForObject(checkSql, Integer.class, partitionName);

                if (exists != null && exists > 0) {
                    log.debug("鍒嗗尯 {} 宸插瓨鍦紝璺宠繃鍒涘缓", partitionName);
                    continue;
                }

                // 璁＄畻鍒嗗尯鑼冨洿
                LocalDate startDate = yearMonth.atDay(1);
                LocalDate endDate = yearMonth.atEndOfMonth().plusDays(1);

                // 鍒涘缓鍒嗗尯锟?
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
                log.info("鎴愬姛鍒涘缓鍒嗗尯: {}", partitionName);
                count++;

            } catch (Exception e) {
                log.error("鍒涘缓鍒嗗尯澶辫触: {}.{}", table.tableName, partitionSuffix, e);
            }
        }

        return count;
    }

    /**
     * 鍒嗙杩囨湡鍒嗗尯锛堝綊妗ｏ級
     */
    private int detachExpiredPartitions(YearMonth cutoffMonth) {
        int count = 0;

        for (PartitionTable table : PARTITION_TABLES) {
            try {
                // 鏌ヨ鎵€鏈夊垎锟?
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
                    // 浠庡垎鍖哄悕鎻愬彇骞存湀 (渚嬪: ord_order_202401 -> 202401)
                    String suffix = partition.substring(table.tableName.length() + 1);
                    try {
                        YearMonth partitionMonth = YearMonth.parse(suffix, DateTimeFormatter.ofPattern("yyyyMM"));

                        if (partitionMonth.isBefore(cutoffMonth)) {
                            // DETACH 鍒嗗尯锛堜笉鍒犻櫎鏁版嵁锛屽彧鏄粠涓昏〃鍒嗙锟?
                            String detachSql = String.format(
                                "ALTER TABLE %s DETACH PARTITION %s",
                                table.tableName,
                                partition
                            );
                            jdbcTemplate.execute(detachSql);
                            log.info("鎴愬姛褰掓。鍒嗗尯: {}", partition);
                            count++;
                        }
                    } catch (Exception e) {
                        log.warn("鏃犳硶瑙ｆ瀽鍒嗗尯锟?{}", partition);
                    }
                }

            } catch (Exception e) {
                log.error("褰掓。鍒嗗尯澶辫触: {}", table.tableName, e);
            }
        }

        return count;
    }

    /**
     * 鍒嗗尯琛ㄩ厤锟?
     */
    private record PartitionTable(String tableName, String partitionColumn) {
    }
}
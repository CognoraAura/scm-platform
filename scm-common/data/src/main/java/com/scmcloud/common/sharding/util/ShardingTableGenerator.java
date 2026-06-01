package com.scmcloud.common.sharding.util;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * 分表建表脚本生成工具
 *
 * @author Deng
 * createData 2025/11/11 15:14
 * @version 1.0
 */
public class ShardingTableGenerator {

    public static void main(String[] args) {
        YearMonth start = YearMonth.of(2025, 1);
        YearMonth end = YearMonth.of(2030, 12);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMM");

        StringBuilder ddl = new StringBuilder("-- 自动生成 sys_audit_log 分表DDL\n\n");

        for (YearMonth ym = start; !ym.isAfter(end); ym = ym.plusMonths(1)) {
            String suffix = ym.format(fmt);
            String nextMonth = ym.plusMonths(1).toString();

            ddl.append(String.format("""
                    CREATE TABLE IF NOT EXISTS sys_audit_log_%s LIKE sys_audit_log;
                    -- 可�? 添加索引或修改表属�?
                    -- ALTER TABLE sys_audit_log_%s ADD INDEX idx_user_id(user_id);
                    """, suffix, suffix));
        }

        System.out.println(ddl);
    }
}

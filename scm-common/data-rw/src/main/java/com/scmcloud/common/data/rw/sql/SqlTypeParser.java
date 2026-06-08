package com.scmcloud.common.data.rw.sql;

import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * SQL 绫诲瀷瑙ｆ瀽锟?
 * <p>
 * 閫氳繃 SQL 璇彞鍒ゆ柇鏄鎿嶄綔杩樻槸鍐欐搷锟?
 *
 * @author Deng
 * @since 2025-12-16
 */
@Slf4j
public class SqlTypeParser {
    /**
     * 鍐欐搷浣滃叧閿瓧
     */
    private static final Set<String> WRITE_KEYWORDS = Set.of(
            "INSERT", "UPDATE", "DELETE", "REPLACE",
            "CREATE", "ALTER", "DROP", "TRUNCATE",
            "GRANT", "REVOKE", "LOCK", "UNLOCK",
            "CALL", "MERGE", "UPSERT"
    );

    /**
     * SELECT ... FOR UPDATE 妯″紡
     */
    private static final Pattern FOR_UPDATE_PATTERN =
            Pattern.compile(".*\\bFOR\\s+UPDATE\\b.*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * SELECT ... LOCK IN SHARE MODE 妯″紡
     */
    private static final Pattern LOCK_IN_SHARE_MODE_PATTERN =
            Pattern.compile(".*\\bLOCK\\s+IN\\s+SHARE\\s+MODE\\b.*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * Hint: 寮哄埗涓诲簱
     */
    private static final Pattern MASTER_HINT_PATTERN =
            Pattern.compile("/\\*\\s*MASTER\\s*\\*/", Pattern.CASE_INSENSITIVE);

    /**
     * Hint: 寮哄埗浠庡簱
     */
    private static final Pattern SLAVE_HINT_PATTERN =
            Pattern.compile("/\\*\\s*SLAVE\\s*\\*/", Pattern.CASE_INSENSITIVE);

    /**
     * Hint: 鎸囧畾浠庡簱鍚嶇О
     */
    private static final Pattern SLAVE_NAME_HINT_PATTERN =
            Pattern.compile("/\\*\\s*SLAVE\\s*\\(\\s*(\\w+)\\s*\\)\\s*\\*/", Pattern.CASE_INSENSITIVE);

    /**
     * SQL 绫诲瀷
     */
    public enum SqlType {
        READ,
        WRITE,
        UNKNOWN
    }

    /**
     * 璺敱 Hint
     */
    public record RoutingHint(
            HintType type,
            String slaveName
    ) {
        public enum HintType {
            NONE,
            MASTER,
            SLAVE
        }
    }

    /**
     * 瑙ｆ瀽 SQL 绫诲瀷
     *
     * @param sql SQL 璇彞
     * @return SQL 绫诲瀷
     */
    public static SqlType parse(String sql) {
        if (sql == null || sql.isBlank()) {
            return SqlType.UNKNOWN;
        }

        String trimmedSql = sql.trim();

        // 1. 妫€鏌ユ槸鍚︽湁 FOR UPDATE / LOCK IN SHARE MODE锛堥渶瑕佽蛋涓诲簱锟?
        if (FOR_UPDATE_PATTERN.matcher(trimmedSql).matches() ||
                LOCK_IN_SHARE_MODE_PATTERN.matcher(trimmedSql).matches()) {
            log.trace("[SQL-Parser] Detected locking SQL, type: WRITE");
            return SqlType.WRITE;
        }

        // 2. 鑾峰彇绗竴涓叧閿瓧
        String firstKeyword = getFirstKeyword(trimmedSql);

        // 3. 鍒ゆ柇鏄惁鏄啓鎿嶄綔
        if (WRITE_KEYWORDS.contains(firstKeyword)) {
            log.trace("[SQL-Parser] Detected write keyword [{}], type: WRITE", firstKeyword);
            return SqlType.WRITE;
        }

        // 4. SELECT 寮€澶磋涓鸿鎿嶄綔
        if ("SELECT".equals(firstKeyword) || "SHOW".equals(firstKeyword) ||
                "DESCRIBE".equals(firstKeyword) || "EXPLAIN".equals(firstKeyword)) {
            log.trace("[SQL-Parser] Detected read keyword [{}], type: READ", firstKeyword);
            return SqlType.READ;
        }

        log.trace("[SQL-Parser] Unknown SQL type for keyword [{}]", firstKeyword);
        return SqlType.UNKNOWN;
    }

    /**
     * 瑙ｆ瀽璺敱 Hint
     * <p>
     * 鏀寔鏍煎紡锟?
     * - /*MASTER* / SELECT ... 锟藉己鍒朵富搴?
     * - /*SLAVE* / SELECT ... 锟藉己鍒朵粠搴擄紙璐熻浇鍧囪　閫夋嫨锟?
     * - /*SLAVE(slave1)* / SELECT ... 锟芥寚瀹氫粠搴?
     *
     * @param sql SQL 璇彞
     * @return 璺敱 Hint
     */
    public static RoutingHint parseHint(String sql) {
        if (sql == null || sql.isBlank()) {
            return new RoutingHint(RoutingHint.HintType.NONE, null);
        }

        // 妫€锟組ASTER Hint
        if (MASTER_HINT_PATTERN.matcher(sql).find()) {
            log.trace("[SQL-Parser] Found MASTER hint");
            return new RoutingHint(RoutingHint.HintType.MASTER, null);
        }

        // 妫€鏌ュ甫鍚嶇О锟絊LAVE Hint
        var slaveNameMatcher = SLAVE_NAME_HINT_PATTERN.matcher(sql);
        if (slaveNameMatcher.find()) {
            String slaveName = slaveNameMatcher.group(1);
            log.trace("[SQL-Parser] Found SLAVE hint with name: {}", slaveName);
            return new RoutingHint(RoutingHint.HintType.SLAVE, slaveName);
        }

        // 妫€锟絊LAVE Hint
        if (SLAVE_HINT_PATTERN.matcher(sql).find()) {
            log.trace("[SQL-Parser] Found SLAVE hint");
            return new RoutingHint(RoutingHint.HintType.SLAVE, null);
        }

        return new RoutingHint(RoutingHint.HintType.NONE, null);
    }

    /**
     * 绉婚櫎 SQL 涓殑 Hint 娉ㄩ噴
     */
    public static String removeHint(String sql) {
        if (sql == null) {
            return null;
        }
        return sql
                .replaceAll("/\\*\\s*MASTER\\s*\\*/", "")
                .replaceAll("/\\*\\s*SLAVE\\s*(\\(\\s*\\w+\\s*\\))?\\s*\\*/", "")
                .trim();
    }

    /**
     * 鑾峰彇 SQL 鐨勭涓€涓叧閿瓧
     */
    private static String getFirstKeyword(String sql) {
        // 璺宠繃娉ㄩ噴鍜岀┖锟?
        String cleanSql = sql
                .replaceAll("/\\*.*?\\*/", "")  // 绉婚櫎鍧楁敞锟?
                .replaceAll("--.*$", "")         // 绉婚櫎琛屾敞锟?
                .replaceAll("^\\s+", "");        // 绉婚櫎寮€澶寸┖锟?

        // 鑾峰彇绗竴涓瘝
        int spaceIndex = cleanSql.indexOf(' ');
        int newlineIndex = cleanSql.indexOf('\n');
        int tabIndex = cleanSql.indexOf('\t');

        int endIndex = cleanSql.length();
        if (spaceIndex > 0) endIndex = Math.min(endIndex, spaceIndex);
        if (newlineIndex > 0) endIndex = Math.min(endIndex, newlineIndex);
        if (tabIndex > 0) endIndex = Math.min(endIndex, tabIndex);

        return cleanSql.substring(0, endIndex).toUpperCase();
    }

    /**
     * 鍒ゆ柇鏄惁鏄簨鍔℃帶鍒惰锟?
     */
    public static boolean isTransactionStatement(String sql) {
        if (sql == null) {
            return false;
        }
        String upper = sql.trim().toUpperCase();
        return upper.startsWith("BEGIN") ||
                upper.startsWith("START TRANSACTION") ||
                upper.startsWith("COMMIT") ||
                upper.startsWith("ROLLBACK") ||
                upper.startsWith("SAVEPOINT") ||
                upper.startsWith("SET AUTOCOMMIT");
    }
}

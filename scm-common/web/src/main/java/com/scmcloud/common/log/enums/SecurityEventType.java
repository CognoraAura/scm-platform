package com.scmcloud.common.log.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *
 *
 * @author Deng
 * createData 2025/10/24 16:14
 * @version 1.0
 */
@Getter
@AllArgsConstructor
public enum SecurityEventType {

    LOGIN_SUCCESS("鐧诲綍鎴愬姛", 1),
    LOGIN_FAILED("鐧诲綍澶辫触", 2),
    LOGOUT("鐧诲嚭", 1),

    PASSWORD_CHANGED("瀵嗙爜淇敼", 2),
    PASSWORD_RESET("瀵嗙爜閲嶇疆", 3),

    PERMISSION_GRANTED("鏉冮檺鎺堜簣", 3),
    PERMISSION_REVOKED("鏉冮檺鎾ら攢", 3),

    DATA_EXPORT("鏁版嵁瀵煎嚭", 4),
    DATA_DELETE("鏁版嵁鍒犻櫎", 4),

    API_ABUSE("API 婊ョ敤", 4),
    SQL_INJECTION_ATTEMPT("SQL 娉ㄥ叆灏濊瘯", 5),
    XSS_ATTEMPT("XSS 鏀诲嚮灏濊瘯", 5),
    UNAUTHORIZED_ACCESS("鏈巿鏉冭闂?, 4),

    IP_BLACKLISTED("IP 琚媺榛?, 3),
    ACCOUNT_LOCKED("璐︽埛閿佸畾", 3);

    private final String description;
    private final Integer riskLevel; // 1-锟?-锟?-锟?-涓ラ噸 5-绱э拷
}

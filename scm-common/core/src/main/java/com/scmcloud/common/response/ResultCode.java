package com.scmcloud.common.response;

import lombok.Getter;

/**
 * 缁熶竴鐘舵€佺爜鏋氫妇
 *
 * @author Deng
 * createData 2025/10/11 14:31
 * @version 1.0
 */
@Getter
public enum ResultCode {
    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    VALIDATION_FAILED(422, "Validation Failed"),
    SERVER_ERROR(500, "Internal Server Error"),

    // 瀹㈡埛绔敊锟?xx
    METHOD_NOT_ALLOWED(405, "璇锋眰鏂规硶涓嶆敮鎸?),
    CONFLICT(409, "璧勬簮鍐茬獊"),
    TOO_MANY_REQUESTS(429, "璇锋眰杩囦簬棰戠箒"),

    // 鏈嶅姟绔敊锟?xx
    INTERNAL_SERVER_ERROR(500, "鏈嶅姟鍣ㄥ唴閮ㄩ敊璇?),
    SERVICE_UNAVAILABLE(503, "鏈嶅姟鏆傛椂涓嶅彲鐢?),
    GATEWAY_TIMEOUT(504, "缃戝叧瓒呮椂"),

    // 涓氬姟閿欒 1xxx
    USER_NOT_FOUND(1001, "鐢ㄦ埛涓嶅瓨鍦?),
    USER_PASSWORD_ERROR(1002, "鐢ㄦ埛鍚嶆垨瀵嗙爜閿欒"),
    USER_LOCKED(1003, "璐︽埛宸茶閿佸畾"),
    USER_DISABLED(1004, "璐︽埛宸茶绂佺敤"),
    USER_NOT_ACTIVATED(1005, "鐢ㄦ埛鏈縺娲?),
    USER_EXIST(1006, "鐢ㄦ埛宸插瓨鍦?),
    USER_CANNOT_DELETE_ADMIN(1007, "涓嶈兘鍒犻櫎绠＄悊鍛樼敤鎴?),
    USER_CANNOT_DELETE_SELF(1008, "涓嶈兘鍒犻櫎鑷繁"),
    USER_NEED_LOGIN(1009, "闇€瑕佺櫥褰?),


    TOKEN_INVALID(1101, "Token 鏃犳晥"),
    TOKEN_EXPIRED(1102, "Token 宸茶繃鏈?),
    TOKEN_BLACKLISTED(1103, "Token 宸插け鏁?),

    PERMISSION_DENIED(1201, "鏉冮檺涓嶈冻"),
    ROLE_NOT_FOUND(1202, "瑙掕壊涓嶅瓨鍦?),
    ROLE_REQUIRED(1203, "闇€瑕佹寚瀹氳鑹?),
    ROLE_ASSIGNMENT_DENIED(1204, "鏃犳潈鍒嗛厤璇ヨ鑹?),
    DATA_ACCESS_DENIED(1205, "鏃犳潈璁块棶璇ユ暟鎹?),

    // 澶氱鎴烽敊锟?3xx
    TENANT_CONTEXT_MISSING(1301, "绉熸埛涓婁笅鏂囨湭璁剧疆锛岃姹傝鎷掔粷"),
    DATA_TENANT_MISSING(1302, "鏁版嵁鏈叧鑱旂鎴?),
    TENANT_DATA_ACCESS_DENIED(1303, "鏃犳潈璁块棶鍏朵粬绉熸埛鐨勬暟鎹?),
    TENANT_ROLE_ACCESS_DENIED(1304, "鏃犳潈璁块棶鍏朵粬绉熸埛鐨勮鑹?),
    TENANT_PERMISSION_ACCESS_DENIED(1305, "鏃犳潈璁块棶鍏朵粬绉熸埛鐨勬潈闄?),
    PLATFORM_RESOURCE_ACCESS_DENIED(1306, "鍙湁骞冲彴绠＄悊鍛樺彲浠ュ垱寤哄钩鍙扮骇璧勬簮"),
    DEPT_TENANT_MISSING(1307, "閮ㄩ棬鏈叧鑱旂鎴?),
    TENANT_DEPT_ACCESS_DENIED(1308, "鏃犳潈璁块棶鍏朵粬绉熸埛鐨勯儴闂?),

    // 鏈嶅姟闂磋皟鐢ㄩ敊锟?xxx
    FEIGN_ERROR(2001, "鏈嶅姟璋冪敤澶辫触"),
    FEIGN_TIMEOUT(2002, "鏈嶅姟璋冪敤瓒呮椂");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
package com.scmcloud.common.response;

import lombok.Getter;

/**
 * 统一状态码枚举
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

    // 客户端错�?4xx
    METHOD_NOT_ALLOWED(405, "请求方法不支�?),
    CONFLICT(409, "资源冲突"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),

    // 服务端错�?5xx
    INTERNAL_SERVER_ERROR(500, "服务器内部错�?),
    SERVICE_UNAVAILABLE(503, "服务暂时不可�?),
    GATEWAY_TIMEOUT(504, "网关超时"),

    // 业务错误 1xxx
    USER_NOT_FOUND(1001, "用户不存�?),
    USER_PASSWORD_ERROR(1002, "用户名或密码错误"),
    USER_LOCKED(1003, "账户已被锁定"),
    USER_DISABLED(1004, "账户已被禁用"),
    USER_NOT_ACTIVATED(1005, "用户未激�?),
    USER_EXIST(1006, "用户已存�?),
    USER_CANNOT_DELETE_ADMIN(1007, "不能删除管理员用�?),
    USER_CANNOT_DELETE_SELF(1008, "不能删除自己"),
    USER_NEED_LOGIN(1009, "需要登�?),


    TOKEN_INVALID(1101, "Token 无效"),
    TOKEN_EXPIRED(1102, "Token 已过�?),
    TOKEN_BLACKLISTED(1103, "Token 已失�?),

    PERMISSION_DENIED(1201, "权限不足"),
    ROLE_NOT_FOUND(1202, "角色不存�?),
    ROLE_REQUIRED(1203, "需要指定角�?),
    ROLE_ASSIGNMENT_DENIED(1204, "无权分配该角�?),
    DATA_ACCESS_DENIED(1205, "无权访问该数�?),

    // 多租户错�?13xx
    TENANT_CONTEXT_MISSING(1301, "租户上下文未设置，请求被拒绝"),
    DATA_TENANT_MISSING(1302, "数据未关联租�?),
    TENANT_DATA_ACCESS_DENIED(1303, "无权访问其他租户的数�?),
    TENANT_ROLE_ACCESS_DENIED(1304, "无权访问其他租户的角�?),
    TENANT_PERMISSION_ACCESS_DENIED(1305, "无权访问其他租户的权�?),
    PLATFORM_RESOURCE_ACCESS_DENIED(1306, "只有平台管理员可以创建平台级资源"),
    DEPT_TENANT_MISSING(1307, "部门未关联租�?),
    TENANT_DEPT_ACCESS_DENIED(1308, "无权访问其他租户的部�?),

    // 服务间调用错�?2xxx
    FEIGN_ERROR(2001, "服务调用失败"),
    FEIGN_TIMEOUT(2002, "服务调用超时");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
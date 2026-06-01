package com.scmcloud.common.log.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scmcloud.common.log.entity.SysAuditLog;

import java.util.UUID;

/**
 * <p>
 * 操作审计日志�?服务�?
 * </p>
 *
 * @author author
 * @since 2025-10-14
 */
public interface ISysAuditLogService extends IService<SysAuditLog> {

    /**
     * 记录用户登录操作日志
     *
     * @param userId 用户唯一标识�?
     * @param username 用户�?
     * @param ipAddress 登录 IP地址
     * @param success 登录是否成功
     * @param remark 备注信息
     */
    void recordLogin(UUID userId, String username, String ipAddress, boolean success, String remark);

    /**
     * 记录用户登录失败操作日志
     *
     * @param username 用户�?
     * @param ipAddress 登录 IP地址
     * @param reason 登录失败原因
     */
    void recordLoginFailure(String username, String ipAddress, String reason);

    /**
     * 记录用户登出操作日志
     *
     * @param userId 用户唯一标识�?
     * @param remark 备注信息
     */
    void recordLogout(UUID userId, String remark);

    /**
     * 记录安全事件操作日志
     *
     * @param eventType 事件类型
     * @param riskLevel 风险等级
     * @param userId 用户唯一标识�?
     * @param username 用户�?
     * @param ipAddress 操作 IP地址
     * @param resource 操作资源
     * @param success 操作是否成功
     * @param details 详细信息
     */
    void recordSecurityEvent(String eventType, Integer riskLevel, UUID userId, String username, String ipAddress,
                             String resource, boolean success, String details);
}

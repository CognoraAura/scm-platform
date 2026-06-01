package com.scmcloud.system.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.system.domain.entity.SysSensitiveOperationLog;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 敏感操作日志 Mapper 接口
 *
 * @author Deng
 * @since 2025-12-15
 */
@Mapper
@DS("audit")
public interface SysSensitiveOperationLogMapper extends BaseMapper<SysSensitiveOperationLog> {

    /**
     * 根据用户 ID查询敏感操作日志
     */
    @Select("""
            SELECT * FROM sys_sensitive_operation_log
            WHERE user_id = #{userId}
            ORDER BY create_time DESC
            """)
    List<SysSensitiveOperationLog> findByUserId(@Param("userId") UUID userId);

    /**
     * 根据操作类型查询日志
     */
    @Select("""
            SELECT * FROM sys_sensitive_operation_log
            WHERE operation_type = #{operationType}
            ORDER BY create_time DESC
            """)
    List<SysSensitiveOperationLog> findByOperationType(@Param("operationType") String operationType);

    /**
     * 查询高风险操作日志（风险评分>=7�?
     */
    @Select("""
            SELECT * FROM sys_sensitive_operation_log
            WHERE risk_score >= 7
            ORDER BY create_time DESC
            """)
    List<SysSensitiveOperationLog> findHighRiskOperations();

    /**
     * 按时间范围查询日�?
     */
    @Select("""
            SELECT * FROM sys_sensitive_operation_log
            WHERE create_time BETWEEN #{startTime} AND #{endTime}
            ORDER BY create_time DESC
            """)
    List<SysSensitiveOperationLog> findByTimeRange(@Param("startTime") LocalDateTime startTime,
                                                    @Param("endTime") LocalDateTime endTime);

    /**
     * 根据数据指纹查询日志
     */
    @Select("""
            SELECT * FROM sys_sensitive_operation_log
            WHERE data_fingerprint = #{fingerprint}
            ORDER BY create_time DESC
            """)
    List<SysSensitiveOperationLog> findByDataFingerprint(@Param("fingerprint") String fingerprint);

    /**
     * 查询需要审批的操作日志
     */
    @Select("""
            SELECT * FROM sys_sensitive_operation_log
            WHERE approval_required = true AND approval_id IS NULL
            ORDER BY create_time DESC
            """)
    List<SysSensitiveOperationLog> findPendingApprovalOperations();

    /**
     * 统计用户在指定时间范围内的敏感操作次�?
     */
    @Select("""
            SELECT COUNT(*) FROM sys_sensitive_operation_log
            WHERE user_id = #{userId}
              AND create_time BETWEEN #{startTime} AND #{endTime}
            """)
    int countUserOperations(@Param("userId") UUID userId,
                            @Param("startTime") LocalDateTime startTime,
                            @Param("endTime") LocalDateTime endTime);

    /**
     * 统计指定敏感数据类型的操作次�?
     */
    @Select("""
            SELECT COUNT(*) FROM sys_sensitive_operation_log
            WHERE sensitive_data_type = #{dataType}
              AND create_time BETWEEN #{startTime} AND #{endTime}
            """)
    int countByDataType(@Param("dataType") String dataType,
                        @Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime);
}

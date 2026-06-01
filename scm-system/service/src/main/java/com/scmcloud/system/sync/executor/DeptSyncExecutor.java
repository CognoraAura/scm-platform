package com.scmcloud.system.sync.executor;

import com.baomidou.dynamic.datasource.annotation.DS;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 部门同步执行�?
 * <p>
 * 独立�?Bean，用于执行跨库事务操作�?
 * 避免 @Transactional 自调用问题（Spring AOP 代理不拦截同类方法调用）
 *
 * @author Deng
 * @since 2025-12-16
 */
@Slf4j
@Component
public class DeptSyncExecutor {

    /**
     * 同步部门信息�?audit �?
     *
     * @param deptId   部门 ID
     * @param deptName 部门名称
     */
    @DS("audit")
    @Transactional(rollbackFor = Exception.class)
    public void syncToAuditDb(UUID deptId, String deptName) {
        // 更新审计日志中的部门名称
        // 注意：审计日志通常不更新历史记录，这里只是示例
        log.debug("[DeptSync] Would update audit logs for dept: {}, name: {}", deptId, deptName);
    }

    /**
     * 同步部门信息�?approval �?
     *
     * @param deptId   部门 ID
     * @param deptName 部门名称
     */
    @DS("approval")
    @Transactional(rollbackFor = Exception.class)
    public void syncToApprovalDb(UUID deptId, String deptName) {
        // 更新审批记录中的部门名称
        log.debug("[DeptSync] Would update approval records for dept: {}, name: {}", deptId, deptName);
    }
}
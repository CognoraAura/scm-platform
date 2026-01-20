package com.frog.common.rest.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Sentinel RestClient 集成配置
 * <p>替代 OpenFeign 的 Sentinel 集成</p>
 *
 * <p>功能：
 * <ul>
 *   <li>配置 @SentinelResource 限流规则</li>
 *   <li>配置熔断降级规则</li>
 *   <li>不依赖 Feign，使用通用的 @SentinelResource 注解</li>
 * </ul>
 *
 * <p>注意：生产环境建议从 Nacos 动态加载规则，本配置仅作为默认规则</p>
 *
 * @author Claude
 * @since 2025-12-29
 */
@Slf4j
@Configuration
public class SentinelRestClientConfiguration {

    /**
     * 初始化 Sentinel 规则
     */
    @PostConstruct
    public void initSentinelRules() {
        initFlowRules();
        initDegradeRules();
        log.info("Sentinel rules initialized for HTTP Exchange clients");
    }

    /**
     * 初始化限流规则
     * <p>为每个 HTTP Exchange 资源配置 QPS 限流</p>
     */
    private void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();

        // 用户服务 - 更新登录信息（每秒最多 1000 次）
        FlowRule userLoginRule = new FlowRule();
        userLoginRule.setResource("user-service:updateLastLogin");
        userLoginRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        userLoginRule.setCount(1000);
        userLoginRule.setLimitApp("default");
        rules.add(userLoginRule);

        // 认证服务 - 强制登出（每秒最多 100 次）
        FlowRule authLogoutRule = new FlowRule();
        authLogoutRule.setResource("auth-service:forceLogout");
        authLogoutRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        authLogoutRule.setCount(100);
        authLogoutRule.setLimitApp("default");
        rules.add(authLogoutRule);

        // 权限服务 - 通过 URL 查找权限（每秒最多 500 次）
        FlowRule permByUrlRule = new FlowRule();
        permByUrlRule.setResource("permission-service:findByUrl");
        permByUrlRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        permByUrlRule.setCount(500);
        permByUrlRule.setLimitApp("default");
        rules.add(permByUrlRule);

        // 权限服务 - 查询用户权限（每秒最多 1000 次）
        FlowRule permByUserRule = new FlowRule();
        permByUserRule.setResource("permission-service:getUserPermissions");
        permByUserRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        permByUserRule.setCount(1000);
        permByUserRule.setLimitApp("default");
        rules.add(permByUserRule);

        // 权限服务 - 查询权限树（每秒最多 200 次）
        FlowRule permTreeRule = new FlowRule();
        permTreeRule.setResource("permission-service:getTree");
        permTreeRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        permTreeRule.setCount(200);
        permTreeRule.setLimitApp("default");
        rules.add(permTreeRule);

        // 权限服务 - 查询 API 权限（每秒最多 200 次）
        FlowRule permApiRule = new FlowRule();
        permApiRule.setResource("permission-service:getApiPermissions");
        permApiRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        permApiRule.setCount(200);
        permApiRule.setLimitApp("default");
        rules.add(permApiRule);

        FlowRuleManager.loadRules(rules);
        log.info("Sentinel flow rules loaded: {} rules", rules.size());
    }

    /**
     * 初始化熔断降级规则
     * <p>基于异常比例进行熔断</p>
     */
    private void initDegradeRules() {
        List<DegradeRule> rules = new ArrayList<>();

        // 权限服务 - 通过 URL 查找权限（异常比例 50% 触发熔断）
        DegradeRule permByUrlDegrade = new DegradeRule();
        permByUrlDegrade.setResource("permission-service:findByUrl");
        permByUrlDegrade.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
        permByUrlDegrade.setCount(0.5);  // 50% 异常率
        permByUrlDegrade.setTimeWindow(10);  // 熔断时长 10 秒
        permByUrlDegrade.setMinRequestAmount(5);  // 最小请求数 5
        permByUrlDegrade.setStatIntervalMs(1000);  // 统计时长 1 秒
        rules.add(permByUrlDegrade);

        // 权限服务 - 查询用户权限（异常比例 50% 触发熔断）
        DegradeRule permByUserDegrade = new DegradeRule();
        permByUserDegrade.setResource("permission-service:getUserPermissions");
        permByUserDegrade.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
        permByUserDegrade.setCount(0.5);  // 50% 异常率
        permByUserDegrade.setTimeWindow(10);  // 熔断时长 10 秒
        permByUserDegrade.setMinRequestAmount(5);  // 最小请求数 5
        permByUserDegrade.setStatIntervalMs(1000);  // 统计时长 1 秒
        rules.add(permByUserDegrade);

        // 用户服务 - 更新登录信息（异常比例 60% 触发熔断）
        DegradeRule userLoginDegrade = new DegradeRule();
        userLoginDegrade.setResource("user-service:updateLastLogin");
        userLoginDegrade.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
        userLoginDegrade.setCount(0.6);  // 60% 异常率
        userLoginDegrade.setTimeWindow(15);  // 熔断时长 15 秒
        userLoginDegrade.setMinRequestAmount(10);  // 最小请求数 10
        userLoginDegrade.setStatIntervalMs(1000);  // 统计时长 1 秒
        rules.add(userLoginDegrade);

        // 认证服务 - 强制登出（异常比例 50% 触发熔断）
        DegradeRule authLogoutDegrade = new DegradeRule();
        authLogoutDegrade.setResource("auth-service:forceLogout");
        authLogoutDegrade.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
        authLogoutDegrade.setCount(0.5);  // 50% 异常率
        authLogoutDegrade.setTimeWindow(10);  // 熔断时长 10 秒
        authLogoutDegrade.setMinRequestAmount(5);  // 最小请求数 5
        authLogoutDegrade.setStatIntervalMs(1000);  // 统计时长 1 秒
        rules.add(authLogoutDegrade);

        DegradeRuleManager.loadRules(rules);
        log.info("Sentinel degrade rules loaded: {} rules", rules.size());
    }
}

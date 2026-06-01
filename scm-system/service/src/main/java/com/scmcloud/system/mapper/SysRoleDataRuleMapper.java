package com.scmcloud.system.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.system.domain.entity.SysRoleDataRule;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.UUID;

/**
 * 角色数据权限规则关联 Mapper 接口
 *
 * @author Deng
 * @since 2025-12-15
 */
@Mapper
@DS("permission")
public interface SysRoleDataRuleMapper extends BaseMapper<SysRoleDataRule> {

    /**
     * 根据角色 ID查询规则ID列表
     */
    @Select("""
            SELECT rule_id FROM sys_role_data_rule
            WHERE role_id = #{roleId}
            """)
    List<UUID> findRuleIdsByRoleId(@Param("roleId") UUID roleId);

    /**
     * 删除角色的所有规则关�?
     */
    @Delete("""
            DELETE FROM sys_role_data_rule
            WHERE role_id = #{roleId}
            """)
    int deleteByRoleId(@Param("roleId") UUID roleId);

    /**
     * 删除规则的所有角色关�?
     */
    @Delete("""
            DELETE FROM sys_role_data_rule
            WHERE rule_id = #{ruleId}
            """)
    int deleteByRuleId(@Param("ruleId") UUID ruleId);

    /**
     * 批量插入角色规则关联
     */
    @Insert("""
            <script>
            INSERT INTO sys_role_data_rule (id, role_id, rule_id, create_by, create_time) VALUES
            <foreach collection='ruleIds' item='ruleId' separator=','>
            (gen_random_uuid(), #{roleId}, #{ruleId}, #{createBy}, NOW())
            </foreach>
            </script>
            """)
    int batchInsert(@Param("roleId") UUID roleId,
                    @Param("ruleIds") List<UUID> ruleIds,
                    @Param("createBy") UUID createBy);

    /**
     * 检查角色是否已关联规则
     */
    @Select("""
            SELECT COUNT(*) > 0 FROM sys_role_data_rule
            WHERE role_id = #{roleId} AND rule_id = #{ruleId}
            """)
    boolean exists(@Param("roleId") UUID roleId, @Param("ruleId") UUID ruleId);

    /**
     * 删除角色数据权限规则关联
     * <p>
     * 用于删除角色时清理数据权限规则关�?
     */
    @Delete("""
            DELETE FROM sys_role_data_rule
            WHERE role_id = #{roleId}
            """)
    int deleteRoleDataRules(@Param("roleId") UUID roleId);
}

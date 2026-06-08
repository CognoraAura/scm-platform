package com.scmcloud.system.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.system.domain.entity.SysDataPermissionRule;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.UUID;

/**
 * 鏁版嵁鏉冮檺瑙勫垯 Mapper 鎺ュ彛
 *
 * @author Deng
 * @since 2025-12-15
 */
@Mapper
@DS("permission")
public interface SysDataPermissionRuleMapper extends BaseMapper<SysDataPermissionRule> {

    /**
     * 鏍规嵁瑙勫垯缂栫爜鏌ヨ
     */
    @Select("""
            SELECT * FROM sys_data_permission_rule
            WHERE rule_code = #{ruleCode} AND NOT deleted
            """)
    SysDataPermissionRule findByRuleCode(@Param("ruleCode") String ruleCode);

    /**
     * 鏍规嵁璧勬簮绫诲瀷鏌ヨ鍚敤鐨勮锟?
     */
    @Select("""
            SELECT * FROM sys_data_permission_rule
            WHERE resource_type = #{resourceType} AND status = 1 AND NOT deleted
            ORDER BY priority DESC
            """)
    List<SysDataPermissionRule> findByResourceType(@Param("resourceType") String resourceType);

    /**
     * 鏍规嵁瑙掕壊 ID鏌ヨ鍏宠仈鐨勮锟?
     */
    @Select("""
            SELECT r.* FROM sys_data_permission_rule r
            JOIN sys_role_data_rule rdr ON r.id = rdr.rule_id
            WHERE rdr.role_id = #{roleId} AND r.status = 1 AND NOT r.deleted
            ORDER BY r.priority DESC
            """)
    List<SysDataPermissionRule> findByRoleId(@Param("roleId") UUID roleId);

    /**
     * 鏍规嵁鐢ㄦ埛ID鏌ヨ鍏宠仈鐨勮鍒欙紙閫氳繃鐢ㄦ埛瑙掕壊锟?
     */
    @Select("""
            SELECT DISTINCT r.* FROM sys_data_permission_rule r
            JOIN sys_role_data_rule rdr ON r.id = rdr.rule_id
            JOIN sys_user_role ur ON rdr.role_id = ur.role_id
            WHERE ur.user_id = #{userId}
              AND ur.approval_status = 2
              AND (ur.expire_time IS NULL OR ur.expire_time > NOW())
              AND r.status = 1
              AND NOT r.deleted
            ORDER BY r.priority DESC
            """)
    List<SysDataPermissionRule> findByUserId(@Param("userId") UUID userId);

    /**
     * 妫€鏌ヨ鍒欑紪鐮佹槸鍚﹀瓨锟?
     */
    @Select("""
            SELECT COUNT(*) > 0 FROM sys_data_permission_rule
            WHERE rule_code = #{ruleCode} AND NOT deleted
            """)
    boolean existsByRuleCode(@Param("ruleCode") String ruleCode);
}

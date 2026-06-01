package com.scmcloud.system.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.scmcloud.system.domain.entity.SysDept;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 部门�?Mapper 接口
 * <p>
 * 注意：此 Mapper 只处�?db_org 库中�?sys_dept �?
 * 需要获取部门负责人信息时，请在 Service 层聚合查�?
 *
 * @author author
 * @since 2025-11-07
 */
@Mapper
@DS("org")
public interface SysDeptMapper extends BaseMapper<SysDept> {

    /**
     * 查询所有部门列表（不包含负责人信息�?
     * 负责人信息需要在 Service 层通过 SysUserMapper 聚合
     */
    @Select("""
            SELECT * FROM sys_dept
            WHERE NOT deleted
            ORDER BY sort_order, create_time
            """)
    List<SysDept> selectDeptList();

    /**
     * 递归查询部门及其所有子部门 ID
     */
    @Select("""
            WITH RECURSIVE dept_tree AS (
                SELECT id FROM sys_dept
                WHERE id = #{deptId} AND NOT deleted
                UNION ALL
                SELECT d.id FROM sys_dept d
                INNER JOIN dept_tree dt ON d.parent_id = dt.id
                WHERE NOT d.deleted
            )
            SELECT id FROM dept_tree
            """)
    List<UUID> selectDeptAndChildren(@Param("deptId") UUID deptId);

    /**
     * 递归查询多个部门及其所有子部门 ID
     */
    @Select("""
            <script>
            WITH RECURSIVE dept_tree AS (
                SELECT id FROM sys_dept
                WHERE id IN
                <foreach collection='deptIds' item='deptId' open='(' close=')' separator=','>
                    #{deptId}
                </foreach>
                AND NOT deleted
                UNION ALL
                SELECT d.id FROM sys_dept d
                INNER JOIN dept_tree dt ON d.parent_id = dt.id
                WHERE NOT d.deleted
            )
            SELECT DISTINCT id FROM dept_tree
            </script>
            """)
    List<UUID> selectDeptsAndChildren(@Param("deptIds") List<UUID> deptIds);

    /**
     * 统计子部门数
     */
    @Select("""
            SELECT COUNT(*) FROM sys_dept
            WHERE parent_id = #{deptId} AND NOT deleted
            """)
    Integer countChildren(@Param("deptId") UUID deptId);

    /**
     * 批量统计多个部门的子部门数量
     * <p>
     * 用于优化 getDeptTree 等需要统计多个部门子部门数的场景，避�?N+1 查询
     */
    @Select("""
            <script>
            SELECT parent_id, COUNT(*) as child_count FROM sys_dept
            WHERE parent_id IN
            <foreach collection='deptIds' item='deptId' open='(' close=')' separator=','>
                #{deptId}
            </foreach>
            AND NOT deleted
            GROUP BY parent_id
            </script>
            """)
    @MapKey("parent_id")
    Map<UUID, Map<String, Object>> countChildrenByDeptIds(@Param("deptIds") List<UUID> deptIds);

    /**
     * 检查部门编码是否存�?
     */
    @Select("""
            <script>
            SELECT COUNT(*) > 0 FROM sys_dept
            WHERE dept_code = #{deptCode}
            AND NOT deleted
            <if test='excludeId != null'>
                AND id != #{excludeId}
            </if>
            </script>
            """)
    boolean existsByDeptCode(@Param("deptCode") String deptCode,
                             @Param("excludeId") UUID excludeId);

    default boolean existsByDeptCode(String deptCode) {
        return existsByDeptCode(deptCode, null);
    }

    /**
     * 查询部门负责�?ID
     */
    @Select("""
            SELECT leader_id FROM sys_dept
            WHERE id = #{deptId} AND NOT deleted
            """)
    UUID getLeaderId(@Param("deptId") UUID deptId);

    /**
     * 批量查询部门名称
     */
    @Select("""
            <script>
            SELECT id, dept_name FROM sys_dept
            WHERE id IN
            <foreach collection='deptIds' item='id' open='(' close=')' separator=','>
                #{id}
            </foreach>
            AND NOT deleted
            </script>
            """)
    List<Map<String, Object>> selectDeptNames(@Param("deptIds") List<UUID> deptIds);

    /**
     * 根据部门 ID查询部门信息
     */
    @Select("""
            SELECT * FROM sys_dept
            WHERE id = #{deptId} AND NOT deleted
            """)
    SysDept selectByDeptId(@Param("deptId") UUID deptId);

    /**
     * 查询顶级部门列表
     */
    @Select("""
            SELECT * FROM sys_dept
            WHERE parent_id IS NULL AND NOT deleted
            ORDER BY sort_order, create_time
            """)
    List<SysDept> selectTopDepts();

    /**
     * 查询指定部门的直接子部门
     */
    @Select("""
            SELECT * FROM sys_dept
            WHERE parent_id = #{parentId} AND NOT deleted
            ORDER BY sort_order, create_time
            """)
    List<SysDept> selectChildDepts(@Param("parentId") UUID parentId);

    /**
     * 批量查询部门负责�?ID 映射
     */
    @Select("""
            <script>
            SELECT id as dept_id, leader_id FROM sys_dept
            WHERE id IN
            <foreach collection='deptIds' item='id' open='(' close=')' separator=','>
                #{id}
            </foreach>
            AND NOT deleted AND leader_id IS NOT NULL
            </script>
            """)
    List<Map<String, Object>> selectLeaderIds(@Param("deptIds") List<UUID> deptIds);

    // ==================== 冗余字段同步（数据一致性） ====================

    /**
     * 更新负责人冗余信�?
     * �?db_user.sys_user 变更时调用（更新该用户作为负责人的所有部门）
     */
    @Update("""
            UPDATE sys_dept
            SET leader_name = #{leaderName},
                leader_phone = #{leaderPhone}
            WHERE leader_id = #{leaderId}
            """)
    int updateLeaderRedundancy(@Param("leaderId") UUID leaderId,
                               @Param("leaderName") String leaderName,
                               @Param("leaderPhone") String leaderPhone);

    /**
     * 查询部门树（包含冗余的负责人信息，无需跨库�?
     */
    @Select("""
            SELECT id, parent_id, dept_code, dept_name, dept_type,
                   leader_id, leader_name, leader_phone,
                   phone, email, isolation_level, sort_order, status
            FROM sys_dept
            WHERE NOT deleted
            ORDER BY sort_order, create_time
            """)
    List<SysDept> selectDeptTreeWithLeader();

    /**
     * 查询所有有负责人的部门（用于初始化同步�?
     */
    @Select("""
            SELECT id as dept_id, leader_id FROM sys_dept
            WHERE NOT deleted AND leader_id IS NOT NULL
            """)
    List<Map<String, Object>> selectAllLeaderIds();
}

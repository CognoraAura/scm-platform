package com.scmcloud.system.service;

import com.scmcloud.common.dto.dept.DeptDTO;
import com.scmcloud.system.domain.entity.SysDept;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.UUID;

/**
 * <p>
 * 部门�?服务�?
 * </p>
 *
 * @author author
 * @since 2025-11-07
 */
public interface ISysDeptService extends IService<SysDept> {

    /**
     * 查询部门树（树形结构，从根节点开始）�?
     *
     * @return 部门树列�?
     */
    List<DeptDTO> getDeptTree();

    /**
     * 查询指定部门及其所有子部门ID（包含自身）�?
     *
     * @param deptId 部门 ID
     * @return 部门ID列表（包含自身及所有子部门�?
     */
    List<UUID> getDeptAndChildren(UUID deptId);

    /**
     * 新增部门�?
     *
     * @param deptDTO 部门信息
     */
    void addDept(DeptDTO deptDTO);

    /**
     * 修改部门�?
     *
     * @param deptDTO 部门信息
     */
    void updateDept(DeptDTO deptDTO);

    /**
     * 删除部门�?
     *
     * @param id 部门 ID
     */
    void deleteDept(UUID id);

    /**
     * 检查部门下是否存在用户�?
     *
     * @param deptId 部门 ID
     * @return true 表示存在用户；false 表示不存�?
     */
    boolean hasUsers(UUID deptId);

    /**
     * 检查部门下是否存在子部门�?
     *
     * @param deptId 部门 ID
     * @return true 表示存在子部门；false 表示不存�?
     */
    boolean hasChildren(UUID deptId);
}

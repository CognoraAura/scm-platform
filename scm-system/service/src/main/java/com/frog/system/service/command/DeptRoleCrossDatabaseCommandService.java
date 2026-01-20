package com.frog.system.service.command;

import com.frog.common.data.rw.annotation.Master;
import com.frog.system.mapper.SysRoleDeptMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 部门角色跨库命令服务
 * <p>
 * 处理部门角色关联的写操作（db_permission）
 *
 * @author Deng
 * @since 2025-01-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeptRoleCrossDatabaseCommandService {
    private final SysRoleDeptMapper roleDeptMapper;

    /**
     * 删除部门的角色关联
     * <p>
     * 用于删除部门时清理 db_permission.sys_role_dept 中的关联数据
     * 跨库操作：db_org → db_permission
     *
     * @param deptId 部门 ID
     * @return 删除行数
     */
    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public int deleteRoleDeptsByDeptId(UUID deptId) {
        if (deptId == null) {
            return 0;
        }
        log.debug("Deleting role-dept associations for deptId={}", deptId);
        return roleDeptMapper.deleteByDeptId(deptId);
    }
}
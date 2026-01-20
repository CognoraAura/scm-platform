package com.frog.system.service.command;

import com.frog.common.data.rw.annotation.Master;
import com.frog.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 用户角色跨库命令服务
 * <p>
 * 处理用户角色关联的写操作（db_permission）
 *
 * @author Deng
 * @since 2025-01-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserRoleCrossDatabaseCommandService {
    private final SysUserRoleMapper userRoleMapper;

    /**
     * 批量插入用户角色关联（永久授权）
     * <p>
     * 替代 SysUserRoleMapper.batchInsert
     * 跨库操作：db_permission
     *
     * @param userId   用户 ID
     * @param roleIds  角色 ID 列表
     * @param createBy 创建人 ID
     * @return 插入行数
     */
    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public int batchInsertUserRoles(UUID userId, List<UUID> roleIds, UUID createBy) {
        if (userId == null || roleIds == null || roleIds.isEmpty()) {
            return 0;
        }
        log.debug("Batch inserting user roles: userId={}, roleCount={}", userId, roleIds.size());
        return userRoleMapper.batchInsert(userId, roleIds, createBy);
    }

    /**
     * 批量插入临时用户角色关联
     * <p>
     * 替代 SysUserRoleMapper.batchInsertTemporary
     * 跨库操作：db_permission
     *
     * @param userId        用户 ID
     * @param roleIds       角色 ID 列表
     * @param effectiveTime 生效时间
     * @param expireTime    过期时间
     * @param createBy      创建人 ID
     * @return 插入行数
     */
    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public int batchInsertTemporaryUserRoles(UUID userId, List<UUID> roleIds,
                                              LocalDateTime effectiveTime,
                                              LocalDateTime expireTime,
                                              UUID createBy) {
        if (userId == null || roleIds == null || roleIds.isEmpty()) {
            return 0;
        }
        log.debug("Batch inserting temporary user roles: userId={}, roleCount={}, expireTime={}",
                userId, roleIds.size(), expireTime);
        return userRoleMapper.batchInsertTemporary(userId, roleIds, effectiveTime, expireTime, createBy);
    }

    /**
     * 删除用户的所有角色关联
     * <p>
     * 替代 SysUserRoleMapper.deleteByUserId
     * 跨库操作：db_permission
     *
     * @param userId 用户 ID
     * @return 删除行数
     */
    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public int deleteUserRoles(UUID userId) {
        if (userId == null) {
            return 0;
        }
        log.debug("Deleting user roles: userId={}", userId);
        return userRoleMapper.deleteByUserId(userId);
    }

    /**
     * 延长临时角色有效期
     * <p>
     * 替代 SysUserRoleMapper.extendTemporaryRole
     * 跨库操作：db_permission
     *
     * @param userId        用户 ID
     * @param roleId        角色 ID
     * @param newExpireTime 新的过期时间
     * @return 更新行数
     */
    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public int extendTemporaryRole(UUID userId, UUID roleId, LocalDateTime newExpireTime) {
        if (userId == null || roleId == null || newExpireTime == null) {
            return 0;
        }
        log.debug("Extending temporary role: userId={}, roleId={}, newExpireTime={}",
                userId, roleId, newExpireTime);
        return userRoleMapper.extendTemporaryRole(userId, roleId, newExpireTime);
    }

    /**
     * 终止临时角色
     * <p>
     * 替代 SysUserRoleMapper.terminateTemporaryRole
     * 跨库操作：db_permission
     *
     * @param userId 用户 ID
     * @param roleId 角色 ID
     * @return 更新行数
     */
    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public int terminateTemporaryRole(UUID userId, UUID roleId) {
        if (userId == null || roleId == null) {
            return 0;
        }
        log.debug("Terminating temporary role: userId={}, roleId={}", userId, roleId);
        return userRoleMapper.terminateTemporaryRole(userId, roleId);
    }
}
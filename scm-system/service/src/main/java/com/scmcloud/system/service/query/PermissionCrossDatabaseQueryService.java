package com.scmcloud.system.service.query;

import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.common.dto.permission.PermissionDTO;
import com.scmcloud.system.mapper.SysPermissionMapper;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 权限跨库查询服务
 * <p>
 * 处理与权限相关的跨库查询操作（db_permission �?db_user�?
 *
 * @author Deng
 * @since 2025-01-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionCrossDatabaseQueryService {
    private final SysPermissionMapper permissionMapper;

    /**
     * 查询用户菜单�?
     * <p>
     * 替代 SysPermissionMapper.findMenuTreeByUserId
     * 跨库查询：db_permission
     *
     * @param userId 用户 ID
     * @return 菜单权限 DTO 列表
     */
    @Slave
    @Timed(value = "cross_db_query", extraTags = {"method", "findMenuTreeByUserId"})
    @Cacheable(value = "userMenuTree", key = "#userId", unless = "#result.isEmpty()")
    public List<PermissionDTO> findMenuTreeByUserId(UUID userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return permissionMapper.findMenuTreeByUserId(userId);
    }
}
package com.scmcloud.system.rpc;

import com.scmcloud.common.dto.user.UserDTO;
import com.scmcloud.common.dto.user.UserInfo;
import com.scmcloud.common.web.domain.SecurityUser;
import com.scmcloud.system.service.ISysPermissionService;
import com.scmcloud.system.service.ISysUserService;
import com.scmcloud.system.api.UserDubboService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * 用户Dubbo服务实现
 * 为用户相关操作提供高性能RPC服务
 *
 * @author Deng
 * @version 2.0
 */
@RequiredArgsConstructor
@DubboService
@Component
public class UserDubboServiceImpl implements UserDubboService {
    private final ISysUserService sysUserService;
    private final ISysPermissionService sysPermissionService;

    @Override
    public SecurityUser getUserByUsername(String username) {
        return sysUserService.getUserByUsername(username);
    }

    @Override
    public Set<String> getUserRoles(UUID userId) {
        return sysPermissionService.getUserRoles(userId);
    }

    @Override
    public Set<String> getUserPermissions(UUID userId) {
        return sysPermissionService.getUserPermissions(userId);
    }

    @Override
    public UserInfo getUserInfo(UUID userId) {
        return sysUserService.getUserInfo(userId);
    }

    @Override
    public void updateLastLogin(UUID userId, String ipAddress, LocalDateTime loginTime) {
        // 调用现有方法；如果不需要，则忽�loginTime 参数�
        sysUserService.updateLastLogin(userId, ipAddress);
    }

    @Override
    public UserDTO getUserById(UUID userId) {
        return sysUserService.getUserById(userId);
    }

    @Override
    public Set<String> findRolesByUserId(UUID userId) {
        return sysPermissionService.getUserRoles(userId);
    }

    @Override
    public Set<String> findPermissionsByUserId(UUID userId) {
        return sysPermissionService.getUserPermissions(userId);
    }
}


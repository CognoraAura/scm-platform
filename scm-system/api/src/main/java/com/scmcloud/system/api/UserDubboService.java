package com.scmcloud.system.api;

import com.scmcloud.common.dto.user.UserDTO;
import com.scmcloud.common.dto.user.UserInfo;
import com.scmcloud.common.web.domain.SecurityUser;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Dubbo API 用于服务间的高速内�?RPC 通信�?
 * 提供高性能的用户相关操作，包括身份验证和授权�?
 */
public interface UserDubboService {

    /**
     * 根据用户名获取用户进行身份验证�?
     * �?Spring Security �?UserDetailsService 使用�?
     *
     * @param username 要搜索的用户�?
     * @return 包含身份验证信息�?SecurityUser 对象，如果找不到则返�?null
     */
    SecurityUser getUserByUsername(String username);

    /**
     * Get user roles by userId.
     * Used for token refresh and authentication.
     *
     * @param userId the user ID
     * @return set of role codes
     */
    Set<String> getUserRoles(UUID userId);

    /**
     * Get user permissions by userId.
     * Used for token refresh and authentication.
     *
     * @param userId the user ID
     * @return set of permission codes
     */
    Set<String> getUserPermissions(UUID userId);

    /**
     * Fetch user info by userId.
     *
     * @param userId the user ID
     * @return UserInfo DTO
     */
    UserInfo getUserInfo(UUID userId);

    /**
     * Update last login info.
     *
     * @param userId the user ID
     * @param ipAddress the login IP address
     * @param loginTime the login timestamp
     */
    void updateLastLogin(UUID userId, String ipAddress, LocalDateTime loginTime);

    /**
     * Get username by userId.
     *
     * @param userId the user ID
     * @return UserDTO
     */
    UserDTO getUserById(UUID userId);

    /**
     * Get user roles by userId (alias for getUserRoles).
     * Used for token refresh and authentication.
     *
     * @param userId the user ID
     * @return set of role codes
     */
    Set<String> findRolesByUserId(UUID userId);

    /**
     * Get user permissions by userId (alias for getUserPermissions).
     * Used for token refresh and authentication.
     *
     * @param userId the user ID
     * @return set of permission codes
     */
    Set<String> findPermissionsByUserId(UUID userId);
}


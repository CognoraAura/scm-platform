package com.scmcloud.system.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.common.response.ResultCode;
import com.scmcloud.common.tenant.TenantValidationUtil;
import com.scmcloud.common.util.UUIDv7Util;

import com.scmcloud.common.exception.BusinessException;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scmcloud.common.dto.permission.PermissionDTO;
import com.scmcloud.common.dto.user.UserDTO;
import com.scmcloud.common.dto.user.UserInfo;
import com.scmcloud.common.web.domain.SecurityUser;
import com.scmcloud.common.web.util.SecurityUtils;
import com.scmcloud.system.domain.entity.SysUser;
import com.scmcloud.system.event.DataSyncEventPublisher;
import com.scmcloud.system.mapper.SysUserMapper;
import com.scmcloud.system.service.ISysUserService;
import com.scmcloud.system.service.command.UserRoleCrossDatabaseCommandService;
import com.scmcloud.system.service.query.DeptCrossDatabaseQueryService;
import com.scmcloud.system.service.query.PermissionCrossDatabaseQueryService;
import com.scmcloud.system.service.query.RoleCrossDatabaseQueryService;
import com.scmcloud.system.service.query.UserCrossDatabaseQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户�?服务实现�?
 * </p>
 *
 * @author author
 * @since 2025-10-14
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {
    private final SysUserMapper userMapper;
    private final UserCrossDatabaseQueryService userQueryService;
    private final RoleCrossDatabaseQueryService roleQueryService;
    private final DeptCrossDatabaseQueryService deptQueryService;
    private final PermissionCrossDatabaseQueryService permissionQueryService;
    private final UserRoleCrossDatabaseCommandService userRoleCommandService;
    private final PasswordEncoder passwordEncoder;
    private final DataSyncEventPublisher dataSyncEventPublisher;
    private final com.frog.common.security.PermissionChecker permissionChecker;

    @Value("${spring.security.default-password}")
    private String defaultPassword;

    /**
     * 分页查询用户列表
     * <p>
     * 使用只读事务，自动路由到从库，自动应用数据权限过�?
     */
    @Transactional(readOnly = true)
    public Page<UserDTO> listUsers(Integer pageNum, Integer pageSize,
                                   String username, Integer status) {
        // 1. 验证租户上下�?
        UUID tenantId = TenantValidationUtil.getRequiredTenantId();

        // 2. 获取当前用户的数据权限范�?
        UUID operatorId = SecurityUtils.getCurrentUserUuid().orElse(null);
        String dataScope = permissionChecker.getUserDataScope(operatorId);

        // 3. 构建查询条件
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();

        // 基本过滤条件
        wrapper.like(username != null && !username.isEmpty(), SysUser::getUsername, username)
                .eq(status != null, SysUser::getStatus, status);

        // 4. 应用数据权限过滤（通过 TenantInterceptor 自动过滤 tenant_id�?
        if (!"ALL".equals(dataScope)) {
            List<UUID> accessibleDeptIds = permissionChecker.getAccessibleDepartmentIds(operatorId, tenantId);

            if ("SELF".equals(dataScope)) {
                // 只能查看自己创建的用�?
                wrapper.eq(SysUser::getCreateBy, operatorId);
            } else if (!accessibleDeptIds.isEmpty()) {
                // DEPT, DEPT_AND_SUB, CUSTOM - 根据部门过滤
                wrapper.in(SysUser::getDeptId, accessibleDeptIds);
            } else {
                // 没有可访问的部门，返回空结果
                return new Page<>(pageNum, pageSize, 0);
            }
        }

        wrapper.orderByDesc(SysUser::getCreateTime);

        // 5. 执行查询
        Page<SysUser> userPage = userMapper.selectPage(page, wrapper);

        // 6. 转换�?DTO
        Page<UserDTO> userDTOPage = new Page<>(pageNum, pageSize, userPage.getTotal());
        List<UserDTO> userDTOs = userPage.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        userDTOPage.setRecords(userDTOs);

        return userDTOPage;
    }

    /**
     * 根据 ID查询用户
     */
    @Slave
    @Cacheable(
            value = "user",
            key = "#id"
    )
    public UserDTO getUserById(UUID id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND.getCode(), ResultCode.USER_NOT_FOUND.getMessage());
        }

        UserDTO userDTO = convertToDTO(user);

        // 通过 CrossDatabaseQueryService 跨库查询 db_permission
        List<Map<String, Object>> roles = userQueryService.findUserRolesWithNames(id);

        if (!roles.isEmpty()) {
            List<UUID> roleIds = roles.stream()
                    .map(role -> (UUID) role.get("id"))
                    .toList();
            List<String> roleNames = roles.stream()
                    .map(role -> (String) role.get("name"))
                    .toList();

            userDTO.setRoleIds(roleIds);
            userDTO.setRoleNames(roleNames);
        }

        return userDTO;
    }

    /**
     * 根据用户名获取用户（用于 Spring Security 认证�?
     * <p>
     * 查询走从库，返回包含密码、角色、权限的完整认证信息
     */
    @Override
    @Slave
    @Cacheable(
            value = "userDetails",
            key = "#username"
    )
    public SecurityUser getUserByUsername(String username) {
        SysUser user = userMapper.findByUsername(username);
        if (user == null) {
            return null;
        }

        // 跨库查询角色和权�?
        Set<String> roles = userQueryService.findRoleCodesByUserId(user.getId());
        Set<String> permissions = userQueryService.findPermissionCodesByUserId(user.getId());

        return SecurityUser.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .realName(user.getRealName())
                .deptId(user.getDeptId())
                .status(user.getStatus())
                .accountType(user.getAccountType())
                .userLevel(user.getUserLevel())
                .roles(roles != null ? roles : Collections.emptySet())
                .permissions(permissions != null ? permissions : Collections.emptySet())
                .twoFactorSecret(user.getTwoFactorSecret())
                .twoFactorEnabled(user.getTwoFactorEnabled())
                .passwordExpireTime(user.getPasswordExpireTime())
                .forceChangePassword(user.getForceChangePassword())
                .build();
    }

    /**
     * 获取用户详细信息（包含权限和菜单�?
     * <p>
     * 查询走从�?
     */
    @Slave
    @Cacheable(
            value = "userInfo",
            key = "#userId"
    )
    public UserInfo getUserInfo(UUID userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND.getCode(), ResultCode.USER_NOT_FOUND.getMessage());
        }

        UserInfo userInfo = UserInfo.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .avatar(user.getAvatar())
                .email(user.getEmail())
                .phone(user.getPhone())
                .deptId(user.getDeptId())
                .userLevel(user.getUserLevel())
                .build();

        // 通过 CrossDatabaseQueryService 跨库查询 db_permission
        Set<String> roles = userQueryService.findRoleCodesByUserId(userId);
        Set<String> permissions = userQueryService.findPermissionCodesByUserId(userId);

        userInfo.setRoles(roles);
        userInfo.setPermissions(permissions);

        // 构建菜单树（只返回菜单类型的权限�?
        List<PermissionDTO> menuTree = permissionQueryService.findMenuTreeByUserId(userId);
        userInfo.setMenuTree(new HashSet<>(menuTree));

        return userInfo;
    }

    /**
     * 新增用户
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(
            value = {"user", "userDetails", "userInfo"},
            allEntries = true
    )
    public void addUser(UserDTO userDTO) {
        // 1. 验证租户上下�?
        UUID tenantId = TenantValidationUtil.getRequiredTenantId();

        // 2. 检查操作权�?
        UUID operatorId = SecurityUtils.getCurrentUserUuid().orElse(null);
        permissionChecker.requirePermission(operatorId, "user:add");

        // 3. 验证用户名唯一�?
        if (userMapper.existsByUsername(userDTO.getUsername())) {
            throw new BusinessException(ResultCode.USER_EXIST.getCode(), ResultCode.USER_EXIST.getMessage());
        }

        // 4. 密码编码
        String encodedPassword;
        if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
            encodedPassword = passwordEncoder.encode(userDTO.getPassword());
        } else {
            encodedPassword = passwordEncoder.encode(defaultPassword);
        }

        // 5. 准备实体（自动填�?tenant_id�?
        SysUser user = new SysUser();
        BeanUtils.copyProperties(userDTO, user);
        user.setPassword(encodedPassword);
        user.setId(UUIDv7Util.generate());
        user.setTenantId(tenantId); // 自动填充租户 ID
        user.setPasswordExpireTime(LocalDateTime.now().plusDays(90));
        user.setForceChangePassword(true);

        // 6. 数据库操�?
        userMapper.insert(user);

        // 7. 跨库操作：插入用户角色关联（db_permission�?
        if (userDTO.getRoleIds() != null && !userDTO.getRoleIds().isEmpty()) {
            int inserted = userRoleCommandService.batchInsertUserRoles(user.getId(), userDTO.getRoleIds(),
                    SecurityUtils.getCurrentUserUuid().orElse(null));
            log.debug("创建用户时分配角�? user={}, roleCount={}", user.getUsername(), inserted);
        }

        // 8. 发布同步事件
        dataSyncEventPublisher.publishUserCreated(user);

        // 9. 记录租户操作日志
        com.frog.common.tenant.TenantValidationUtil.logTenantOperation("CREATE", "USER", user.getId());

        log.info("用户创建成功: username={}, operator={}", user.getUsername(), SecurityUtils.getCurrentUsername());
    }

    /**
     * 修改用户
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(
            value = {"user", "userDetails", "userInfo"},
            key = "#userDTO.id"
    )
    public void updateUser(UserDTO userDTO) {
        // 1. 验证租户上下文（确保请求包含有效租户信息�?
        TenantValidationUtil.getRequiredTenantId();

        // 2. 检查操作权�?
        UUID operatorId = SecurityUtils.getCurrentUserUuid().orElse(null);
        permissionChecker.requirePermission(operatorId, "user:update");

        // 3. 查询数据
        SysUser existUser = userMapper.selectById(userDTO.getId());
        if (existUser == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND.getCode(), ResultCode.USER_NOT_FOUND.getMessage());
        }

        // 4. 验证数据归属（tenant_id�?
        TenantValidationUtil.validateDataOwnership(existUser.getTenantId());

        // 5. 检查数据权限（是否可操作该用户�?
        String dataScope = permissionChecker.getUserDataScope(operatorId);
        if (!permissionChecker.canOperateResource(operatorId, existUser.getCreateBy(),
                existUser.getDeptId(), dataScope)) {
            throw new BusinessException(ResultCode.DATA_ACCESS_DENIED.getCode(), "无权操作该用户数�?);
        }

        // 6. 执行业务逻辑
        SysUser user = new SysUser();
        BeanUtils.copyProperties(userDTO, user);
        user.setPassword(null); // 不允许通过此接口修改密�?
        userMapper.updateById(user);

        // 7. 跨库操作：更新用户角色关联（db_permission�?
        if (userDTO.getRoleIds() != null) {
            int deleted = userRoleCommandService.deleteUserRoles(user.getId());
            log.debug("更新用户时清除旧角色: user={}, deletedCount={}", user.getUsername(), deleted);

            if (!userDTO.getRoleIds().isEmpty()) {
                int inserted = userRoleCommandService.batchInsertUserRoles(user.getId(), userDTO.getRoleIds(),
                        SecurityUtils.getCurrentUserUuid().orElse(null));
                log.debug("更新用户时重新分配角�? user={}, newRoleCount={}", user.getUsername(), inserted);
            }
        }

        // 8. 发布同步事件
        SysUser updatedUser = userMapper.selectById(user.getId());
        dataSyncEventPublisher.publishUserUpdated(updatedUser);

        // 9. 记录日志
        TenantValidationUtil.logTenantOperation("UPDATE", "USER", userDTO.getId());

        log.info("用户更新成功: username={}, operator={}", user.getUsername(), SecurityUtils.getCurrentUsername());
    }

    /**
     * 删除用户（逻辑删除�?
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(
            value = {"user", "userDetails", "userInfo"},
            key = "#id"
    )
    public void deleteUser(UUID id) {
        // 1. 验证租户上下文（确保请求包含有效租户信息�?
        TenantValidationUtil.getRequiredTenantId();

        // 2. 检查操作权�?
        UUID operatorId = SecurityUtils.getCurrentUserUuid().orElse(null);
        permissionChecker.requirePermission(operatorId, "user:delete");

        // 3. 查询数据
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND.getCode(), ResultCode.USER_NOT_FOUND.getMessage());
        }

        // 4. 验证数据归属（tenant_id�?
        TenantValidationUtil.validateDataOwnership(user.getTenantId());

        // 5. 检查数据权�?
        String dataScope = permissionChecker.getUserDataScope(operatorId);
        if (!permissionChecker.canOperateResource(operatorId, user.getCreateBy(),
                user.getDeptId(), dataScope)) {
            throw new BusinessException(ResultCode.DATA_ACCESS_DENIED.getCode(), "无权删除该用户数�?);
        }

        // 6. 业务校验
        if (user.getId().equals(UUID.fromString("019a0aee-3b74-7bfc-b34f-48b5428d4875"))) {
            throw new BusinessException(ResultCode.USER_CANNOT_DELETE_ADMIN.getCode(),
                    ResultCode.USER_CANNOT_DELETE_ADMIN.getMessage());
        }

        if (user.getId().equals(SecurityUtils.getCurrentUserUuid().orElse(null))) {
            throw new BusinessException(ResultCode.USER_CANNOT_DELETE_SELF.getCode(),
                    ResultCode.USER_CANNOT_DELETE_SELF.getMessage());
        }

        // 7. 执行删除
        userMapper.deleteById(id);

        // 8. 发布同步事件
        dataSyncEventPublisher.publishUserDeleted(id);

        // 9. 记录日志
        TenantValidationUtil.logTenantOperation("DELETE", "USER", id);

        log.info("用户删除成功: username={}, operator={}", user.getUsername(), SecurityUtils.getCurrentUsername());
    }

    /**
     * 重置密码
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(
            value = {"user", "userDetails"},
            key = "#id"
    )
    public String resetPassword(UUID id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存�?);
        }

        String newPassword = generateRandomPassword();

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setForceChangePassword(true);

        userMapper.updateById(user);

        log.info("Password reset for user: {}, by: {}",
                user.getUsername(), SecurityUtils.getCurrentUsername());

        return newPassword;
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%";
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();

        password.append(chars.charAt(random.nextInt(26)));
        password.append(chars.charAt(26 + random.nextInt(26)));
        password.append(chars.charAt(52 + random.nextInt(10)));
        password.append(chars.charAt(62 + random.nextInt(4)));

        for (int i = 0; i < 8; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }

        return shufflePassword(password.toString(), random);
    }

    private String shufflePassword(String password, SecureRandom random) {
        List<Character> charList = password.chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.toList());
        Collections.shuffle(charList, random);

        return charList.stream()
                .map(String::valueOf)
                .collect(Collectors.joining());
    }

    /**
     * 修改密码
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(
            value = {"user", "userDetails"},
            key = "#userId"
    )
    public void changePassword(UUID userId, String oldPassword, String newPassword) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存�?);
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BusinessException("新密码不能与原密码相�?);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setForceChangePassword(false);
        user.setLastPasswordChangeTime(LocalDateTime.now());

        LocalDateTime passwordExpireTime = LocalDateTime.now().plusDays(90);
        user.setPasswordExpireTime(passwordExpireTime);

        userMapper.updateById(user);

        log.info("Password changed for user: {}", user.getUsername());
    }

    /**
     * 授权角色
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(
            value = {"user", "userDetails", "userInfo", "userRoles", "userPermissions"},
            key = "#userId"
    )
    public void grantRoles(UUID userId, List<UUID> roleIds) {
        // 1. 验证租户上下文（确保请求包含有效租户信息�?
        TenantValidationUtil.getRequiredTenantId();

        // 2. 检查操作权�?
        UUID operatorId = SecurityUtils.getCurrentUserUuid().orElse(null);
        permissionChecker.requirePermission(operatorId, "user:grant-role");

        // 3. 查询数据
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND.getCode(), ResultCode.USER_NOT_FOUND.getMessage());
        }

        // 4. 验证数据归属（tenant_id�?
        TenantValidationUtil.validateDataOwnership(user.getTenantId());

        // 5. 检查角色等级（只能分配不高于自己的角色�?
        if (roleIds != null && !roleIds.isEmpty()) {
            Integer operatorMaxRoleLevel = userQueryService.getUserMaxRoleLevel(operatorId);
            for (UUID roleId : roleIds) {
                Integer roleLevel = roleQueryService.getRoleLevel(roleId);
                permissionChecker.requireRoleAssignmentPermission(operatorId, operatorMaxRoleLevel, roleLevel);

                // 验证角色归属（只能分配本租户或平台角色）
                UUID roleTenantId = roleQueryService.getRoleTenantId(roleId);
                TenantValidationUtil.validateRoleAccess(roleTenantId);
            }
        }

        // 6. 执行业务逻辑：更新用户角色关联（跨库操作 db_permission�?
        int deleted = userRoleCommandService.deleteUserRoles(userId);
        log.debug("授权操作清除原有角色: user={}, deletedCount={}", user.getUsername(), deleted);

        if (roleIds != null && !roleIds.isEmpty()) {
            int inserted = userRoleCommandService.batchInsertUserRoles(userId, roleIds, SecurityUtils.getCurrentUserUuid().orElse(null));
            log.debug("授权操作分配新角�? user={}, grantedCount={}", user.getUsername(), inserted);
        }

        // 7. 记录日志
        TenantValidationUtil.logTenantOperation("GRANT_ROLES", "USER", userId);

        log.info("角色授予操作完成: user={}, roleIds={}, operator={}",
                user.getUsername(), roleIds, SecurityUtils.getCurrentUsername());
    }

    /**
     * 锁定/解锁用户
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(
            value = {"user", "userDetails"},
            key = "#id"
    )
    public void lockUser(UUID id, Boolean lock) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND.getCode(), ResultCode.USER_NOT_FOUND.getMessage());
        }

        if (lock) {
            user.setStatus(2);
            LocalDateTime lockedUntil = LocalDateTime.now().plusHours(24);
            user.setLockedUntil(lockedUntil);
        } else {
            user.setStatus(1);
            user.setLockedUntil(null);
            user.setLoginAttempts(0);
        }

        userMapper.updateById(user);

        log.info("User {} {}, by: {}",
                user.getUsername(), lock ? "locked" : "unlocked",
                SecurityUtils.getCurrentUsername());
    }

    /**
     * 授予临时角色
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(
            value = {"user", "userDetails", "userInfo", "userRoles", "userPermissions"},
            key = "#userId"
    )
    public void grantTemporaryRoles(UUID userId, List<UUID> roleIds,
                                    LocalDateTime effectiveTime, LocalDateTime expireTime) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND.getCode(), ResultCode.USER_NOT_FOUND.getMessage());
        }

        if (expireTime != null && expireTime.isBefore(LocalDateTime.now())) {
            throw new BusinessException("过期时间不能早于当前时间");
        }

        if (effectiveTime != null && expireTime != null && effectiveTime.isAfter(expireTime)) {
            throw new BusinessException("生效时间不能晚于过期时间");
        }

        // 通过 CrossDatabaseQueryService 跨库操作：插入临时用户角色关联（db_permission�?
        if (roleIds != null && !roleIds.isEmpty()) {
            int inserted = userRoleCommandService.batchInsertTemporaryUserRoles(
                    userId, roleIds,
                    effectiveTime != null ? effectiveTime : LocalDateTime.now(),
                    expireTime,
                    SecurityUtils.getCurrentUserUuid().orElse(null)
            );
            log.debug("为用�?{} 授予�?{} 个临时角�?, user.getUsername(), inserted);
        }

        log.info("Temporary roles granted to user: {}, roles: {}, expireTime: {}, by: {}",
                user.getUsername(), roleIds, expireTime, SecurityUtils.getCurrentUsername());
    }

    /**
     * 延长临时角色的有效期
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(
            value = {"user", "userDetails", "userInfo", "userRoles"},
            key = "#userId"
    )
    public void extendTemporaryRole(UUID userId, UUID roleId, LocalDateTime newExpireTime) {
        // 通过 CrossDatabaseQueryService 跨库查询 db_permission
        if (!userQueryService.hasTemporaryRole(userId, roleId)) {
            throw new BusinessException("用户不存在该临时角色或已过期");
        }

        if (newExpireTime.isBefore(LocalDateTime.now())) {
            throw new BusinessException("新的过期时间不能早于当前时间");
        }

        int updated = userRoleCommandService.extendTemporaryRole(userId, roleId, newExpireTime);
        if (updated == 0) {
            throw new BusinessException("延长临时角色失败");
        }

        log.info("Temporary role extended: userId={}, roleId={}, newExpireTime={}, by={}",
                userId, roleId, newExpireTime, SecurityUtils.getCurrentUsername());
    }

    /**
     * 提前终止临时角色
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(
            value = {"user", "userDetails", "userInfo", "userRoles", "userPermissions"},
            key = "#userId"
    )
    public void terminateTemporaryRole(UUID userId, UUID roleId) {
        // 通过 CrossDatabaseQueryService 跨库操作 db_permission
        int updated = userRoleCommandService.terminateTemporaryRole(userId, roleId);
        if (updated == 0) {
            throw new BusinessException("终止临时角色失败，可能该角色不存在或已过�?);
        }

        log.info("Temporary role terminated: userId={}, roleId={}, by={}",
                userId, roleId, SecurityUtils.getCurrentUsername());
    }

    /**
     * 查询用户的临时角色列�?
     */
    @Override
    @Slave
    @Cacheable(
            value = "userTemporaryRoles",
            key = "#userId"
    )
    public List<Map<String, Object>> getUserTemporaryRoles(UUID userId) {
        // 通过 CrossDatabaseQueryService 跨库查询 db_permission
        return userQueryService.findTemporaryRolesByUserId(userId);
    }

    /**
     * 检查用户是否有访问某个部门的权�?
     * <p>
     * 跨库查询：需要查�?db_permission �?db_org
     */
    @Override
    @Slave
    public boolean canAccessDept(UUID userId, UUID deptId) {
        return deptQueryService.hasAccessToDept(userId, deptId);
    }

    /**
     * 获取用户的数据权限范�?
     */
    @Override
    @Slave
    @Cacheable(
            value = "userDataScope",
            key = "#userId"
    )
    public Integer getUserDataScope(UUID userId) {
        // 通过 CrossDatabaseQueryService 跨库查询 db_permission
        return userQueryService.getUserDataScope(userId);
    }

    /**
     * 统计用户信息
     * <p>
     * 跨库查询 db_permission
     */
    @Override
    @Slave
    public Map<String, Object> getUserStatistics(UUID userId) {
        Map<String, Object> stats = new HashMap<>();

        Integer roleCount = userQueryService.countUserRoles(userId);
        stats.put("roleCount", roleCount);

        Integer tempRoleCount = userQueryService.countTemporaryRoles(userId);
        stats.put("temporaryRoleCount", tempRoleCount);

        Integer expiringCount = userQueryService.countExpiringRoles(userId, 7);
        stats.put("expiringRoleCount", expiringCount);

        Integer dataScope = userQueryService.getUserDataScope(userId);
        stats.put("dataScope", dataScope);

        BigDecimal maxApprovalAmount = userQueryService.getMaxApprovalAmount(userId);
        stats.put("maxApprovalAmount", maxApprovalAmount);

        return stats;
    }

    @Override
    public void updateLastLogin(UUID userId, String ipAddress) {
       LambdaUpdateWrapper<SysUser> updateWrapper = new LambdaUpdateWrapper<>();
       updateWrapper.eq(SysUser::getId, userId)
                   .set(SysUser::getLastLoginTime, LocalDateTime.now())
                   .set(SysUser::getLastLoginIp, ipAddress);
       userMapper.update(null, updateWrapper);
    }

    private UserDTO convertToDTO(SysUser user) {
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);

        return userDTO;
    }
}

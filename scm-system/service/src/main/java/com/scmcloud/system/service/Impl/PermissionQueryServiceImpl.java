package com.scmcloud.system.service.Impl;

import com.scmcloud.common.security.PermissionQueryService;
import com.scmcloud.system.domain.entity.SysDept;
import com.scmcloud.system.domain.entity.SysRole;
import com.scmcloud.system.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 鏉冮檺鏌ヨ鏈嶅姟瀹炵幇锟?

 * 鎻愪緵鏉冮檺銆佽鑹层€佹暟鎹潈闄愮瓑鏌ヨ鍔熻兘锛屾敮鎸佺紦锟?
 *
 * @author Claude Code
 * @since 2025-01-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionQueryServiceImpl implements PermissionQueryService {
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;
    private final SysRoleMapper roleMapper;

    /**
     * 鏌ヨ鐢ㄦ埛鐨勬墍鏈夋潈闄愮紪鐮侊紙甯︾紦瀛橈級
     */
    @Override
    @Cacheable(value = "userPermissions", key = "#userId")
    public Set<String> getUserPermissions(UUID userId) {
        if (userId == null) {
            return Collections.emptySet();
        }

        Set<String> permissions = userRoleMapper.findPermissionCodesByUserId(userId);
        return permissions != null ? permissions : Collections.emptySet();
    }

    /**
     * 鏌ヨ鐢ㄦ埛鐨勬墍鏈夎鑹茬紪鐮侊紙甯︾紦瀛橈級
     */
    @Override
    @Cacheable(value = "userRoles", key = "#userId")
    public Set<String> getUserRoles(UUID userId) {
        if (userId == null) {
            return Collections.emptySet();
        }

        Set<String> roles = userRoleMapper.findRoleCodesByUserId(userId);
        return roles != null ? roles : Collections.emptySet();
    }

    /**
     * 鑾峰彇鐢ㄦ埛鐨勬暟鎹潈闄愯寖鍥达紙甯︾紦瀛橈級

     * 鍙栫敤鎴锋墍鏈夎鑹蹭腑鏉冮檺鑼冨洿鏈€澶х殑锛堟暟鍊兼渶灏忕殑锟?
     * NULL = 1锛堝叏閮級, DEPT = 2锛堟湰閮ㄩ棬锟?DEPT_AND_SUB = 3锛堟湰閮ㄩ棬鍙婁笅绾э級, SELF = 4锛堜粎鏈汉锟?CUSTOM = 5锛堣嚜瀹氫箟锟?
     */
    @Override
    @Cacheable(value = "userDataScope", key = "#userId")
    public String getUserDataScope(UUID userId) {
        if (userId == null) {
            return "SELF"; // 榛樿浠呮湰锟?
        }

        // 鑾峰彇鐢ㄦ埛鎵€鏈夋湁鏁堣鑹茬殑data_scope锛屽彇鏈€灏忓€硷紙鏉冮檺鏈€澶э級
        Integer dataScopeValue = userRoleMapper.getUserDataScope(userId);

        if (dataScopeValue == null || dataScopeValue == 1) {
            return "ALL";
        } else if (dataScopeValue == 2) {
            return "DEPT";
        } else if (dataScopeValue == 3) {
            return "DEPT_AND_SUB";
        } else if (dataScopeValue == 4) {
            return "SELF";
        } else if (dataScopeValue == 5) {
            return "CUSTOM";
        }

        return "SELF"; // 榛樿浠呮湰锟?
    }

    /**
     * 鑾峰彇鐢ㄦ埛鐨勯儴闂↖D锛堝甫缂撳瓨锟?
     */
    @Override
    @Cacheable(value = "userDeptId", key = "#userId")
    public UUID getUserDeptId(UUID userId) {
        if (userId == null) {
            return null;
        }

        return userMapper.getUserDeptId(userId);
    }

    /**
     * 鑾峰彇閮ㄩ棬璺緞锛堝甫缂撳瓨锟?
     */
    @Override
    @Cacheable(value = "deptPath", key = "#deptId")
    public String getDeptPath(UUID deptId) {
        if (deptId == null) {
            return null;
        }

        SysDept dept = deptMapper.selectById(deptId);
        return dept != null ? dept.getDeptPath() : null;
    }

    /**
     * 鑾峰彇鐢ㄦ埛鍙闂殑閮ㄩ棬ID鍒楄〃锛堝甫缂撳瓨锟?
     */
    @Override
    @Cacheable(value = "accessibleDeptIds", key = "#userId + ':' + #tenantId + ':' + #dataScope")
    public List<UUID> getAccessibleDepartmentIds(UUID userId, UUID tenantId, String dataScope) {
        if (userId == null) {
            return Collections.emptyList();
        }

        // ALL - 杩斿洖绌哄垪琛ㄨ〃绀哄彲浠ヨ闂墍鏈夐儴闂紙涓嶉渶瑕佽繃婊わ級
        if ("ALL".equals(dataScope)) {
            return Collections.emptyList();
        }

        // SELF - 涓嶆秹鍙婇儴闂ㄨ繃锟?
        if ("SELF".equals(dataScope)) {
            return Collections.emptyList();
        }

        // 鑾峰彇鐢ㄦ埛閮ㄩ棬 ID
        UUID userDeptId = getUserDeptId(userId);
        if (userDeptId == null) {
            return Collections.emptyList();
        }

        // DEPT - 鍙兘璁块棶鏈儴锟?
        if ("DEPT".equals(dataScope)) {
            return List.of(userDeptId);
        }

        // DEPT_AND_SUB - 鍙互璁块棶鏈儴闂ㄥ強涓嬬骇閮ㄩ棬
        if ("DEPT_AND_SUB".equals(dataScope)) {
            List<UUID> deptIds = deptMapper.selectDeptAndChildren(userDeptId);
            return deptIds != null ? deptIds : List.of(userDeptId);
        }

        // CUSTOM - 鑷畾涔夎鍒欙紙鏌ヨ sys_role_dept 琛級
        if ("CUSTOM".equals(dataScope)) {
            // TODO: 瀹炵幇鑷畾涔夋暟鎹潈闄愯鍒欐煡锟?
            log.warn("CUSTOM data scope not implemented yet for userId: {}", userId);
            return List.of(userDeptId);
        }

        return Collections.emptyList();
    }

    /**
     * 鑾峰彇瑙掕壊绛夌骇锛堝甫缂撳瓨锟?
     */
    @Override
    @Cacheable(value = "roleLevel", key = "#roleId")
    public Integer getRoleLevel(UUID roleId) {
        if (roleId == null) {
            return null;
        }

        SysRole role = roleMapper.selectById(roleId);
        return role != null ? role.getRoleLevel() : null;
    }

    /**
     * 鑾峰彇鐢ㄦ埛鐨勬渶楂樿鑹茬瓑绾э紙甯︾紦瀛橈級
     */
    @Override
    @Cacheable(value = "userMaxRoleLevel", key = "#userId")
    public Integer getUserMaxRoleLevel(UUID userId) {
        if (userId == null) {
            return null;
        }

        // 鑾峰彇鐢ㄦ埛鎵€鏈夋湁鏁堣锟絀D
        List<UUID> roleIds = userRoleMapper.findEffectiveRoleIds(userId);
        if (roleIds == null || roleIds.isEmpty()) {
            return null;
        }

        // 鏌ヨ鎵€鏈夎鑹茬殑绛夌骇锛屽彇鏈€澶э拷
        Integer maxLevel = null;
        for (UUID roleId : roleIds) {
            Integer level = getRoleLevel(roleId);
            if (level != null && (maxLevel == null || level > maxLevel)) {
                maxLevel = level;
            }
        }

        return maxLevel;
    }
}
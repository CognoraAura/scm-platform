package com.scmcloud.common.security;

import com.scmcloud.common.exception.BusinessException;
import com.scmcloud.common.response.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 鏉冮檺妫€鏌ュ伐鍏风被
 *
 * 鎻愪緵鐢ㄦ埛鏉冮檺銆佽鑹叉潈闄愩€佹暟鎹潈闄愮瓑妫€鏌ュ姛锟?
 *
 * @author Claude Code
 * @since 2025-01-24
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionChecker {
    private final PermissionQueryService permissionQueryService;

    /**
     * 妫€鏌ョ敤鎴锋槸鍚︽湁鎸囧畾鏉冮檺
     *
     * @param userId 鐢ㄦ埛 ID
     * @param permissionCode 鏉冮檺缂栫爜
     * @return true=鏈夋潈锟?false=鏃犳潈锟?
     */
    public boolean hasPermission(UUID userId, String permissionCode) {
        if (userId == null || permissionCode == null || permissionCode.isEmpty()) {
            return false;
        }

        Set<String> permissions = permissionQueryService.getUserPermissions(userId);
        boolean hasPermission = permissions.contains(permissionCode);

        log.debug("妫€鏌ョ敤鎴锋潈锟?userId={}, permissionCode={}, result={}", userId, permissionCode, hasPermission);
        return hasPermission;
    }

    /**
     * 妫€鏌ョ敤鎴锋槸鍚︽湁鎸囧畾瑙掕壊
     *
     * @param userId 鐢ㄦ埛 ID
     * @param roleCode 瑙掕壊缂栫爜
     * @return true=鏈夎锟?false=鏃犺锟?
     */
    public boolean hasRole(UUID userId, String roleCode) {
        if (userId == null || roleCode == null || roleCode.isEmpty()) {
            return false;
        }

        Set<String> roles = permissionQueryService.getUserRoles(userId);
        boolean hasRole = roles.contains(roleCode);

        log.debug("妫€鏌ョ敤鎴疯锟?userId={}, roleCode={}, result={}", userId, roleCode, hasRole);
        return hasRole;
    }

    /**
     * 妫€鏌ョ敤鎴锋槸鍚︽湁浠讳竴鏉冮檺
     *
     * @param userId 鐢ㄦ埛 ID
     * @param permissionCodes 鏉冮檺缂栫爜鍒楄〃
     * @return true=鑷冲皯鏈変竴涓潈锟?false=鏃犱换浣曟潈锟?
     */
    public boolean hasAnyPermission(UUID userId, List<String> permissionCodes) {
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            return true;
        }

        Set<String> userPermissions = permissionQueryService.getUserPermissions(userId);
        return permissionCodes.stream().anyMatch(userPermissions::contains);
    }

    /**
     * 妫€鏌ョ敤鎴锋槸鍚︽湁鎵€鏈夋潈锟?
     *
     * @param userId 鐢ㄦ埛 ID
     * @param permissionCodes 鏉冮檺缂栫爜鍒楄〃
     * @return true=鏈夋墍鏈夋潈锟?false=缂哄皯鏌愪簺鏉冮檺
     */
    public boolean hasAllPermissions(UUID userId, List<String> permissionCodes) {
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            return true;
        }

        Set<String> userPermissions = permissionQueryService.getUserPermissions(userId);
        return userPermissions.containsAll(permissionCodes);
    }

    /**
     * 瑕佹眰鐢ㄦ埛蹇呴』鏈夋寚瀹氭潈闄愶紝鍚﹀垯鎶涘嚭寮傚父
     *
     * @param userId 鐢ㄦ埛 ID
     * @param permissionCode 鏉冮檺缂栫爜
     * @throws BusinessException 濡傛灉鐢ㄦ埛鏃犳潈锟?
     */
    public void requirePermission(UUID userId, String permissionCode) {
        if (!hasPermission(userId, permissionCode)) {
            log.warn("鏉冮檺妫€鏌ュけ锟?userId={}, permissionCode={}", userId, permissionCode);
            throw new BusinessException(ResultCode.PERMISSION_DENIED.getCode(), "鏉冮檺涓嶈冻" + permissionCode);
        }
    }

    /**
     * 瑕佹眰鐢ㄦ埛蹇呴』鏈夋寚瀹氳鑹诧紝鍚﹀垯鎶涘嚭寮傚父
     *
     * @param userId 鐢ㄦ埛 ID
     * @param roleCode 瑙掕壊缂栫爜
     * @throws BusinessException 濡傛灉鐢ㄦ埛鏃犺锟?
     */
    public void requireRole(UUID userId, String roleCode) {
        if (!hasRole(userId, roleCode)) {
            log.warn("瑙掕壊妫€鏌ュけ锟?userId={}, roleCode={}", userId, roleCode);
            throw new BusinessException(ResultCode.ROLE_REQUIRED.getCode(), "闇€瑕佽鑹诧細" + roleCode);
        }
    }

    /**
     * 妫€鏌ョ敤鎴锋槸鍚﹀彲浠ヨ闂寚瀹氶儴闂ㄧ殑鏁版嵁
     *
     * @param userId 鐢ㄦ埛 ID
     * @param userDeptId 鐢ㄦ埛鎵€灞為儴锟絀D
     * @param targetDeptId 鐩爣閮ㄩ棬 ID
     * @param dataScope 鏁版嵁鏉冮檺鑼冨洿
     * @param deptPath 閮ㄩ棬璺緞锛堢敤浜庡垽鏂笂涓嬬骇鍏崇郴锟?
     * @param targetDeptPath 鐩爣閮ㄩ棬璺緞
     * @return true=鍙互璁块棶, false=涓嶅彲璁块棶
     */
    public boolean canAccessDepartmentData(UUID userId, UUID userDeptId, UUID targetDeptId,
                                                   String dataScope, String deptPath, String targetDeptPath) {
        // ALL - 鍙互璁块棶鎵€鏈夐儴闂ㄦ暟锟?
        if ("ALL".equals(dataScope)) {
            return true;
        }

        // SELF - 鍙兘璁块棶鏈汉鍒涘缓鐨勬暟鎹紙涓嶆秹鍙婇儴闂級
        if ("SELF".equals(dataScope)) {
            return false; // 闇€瑕佸湪涓氬姟灞傚垽锟絚reate_by
        }

        // DEPT - 鍙兘璁块棶鏈儴闂ㄦ暟锟?
        if ("DEPT".equals(dataScope)) {
            return userDeptId != null && userDeptId.equals(targetDeptId);
        }

        // DEPT_AND_SUB - 鍙互璁块棶鏈儴闂ㄥ強涓嬬骇閮ㄩ棬鏁版嵁
        if ("DEPT_AND_SUB".equals(dataScope)) {
            if (userDeptId == null || deptPath == null || targetDeptPath == null) {
                return false;
            }

            // 鍒ゆ柇鐩爣閮ㄩ棬鏄惁鍦ㄥ綋鍓嶉儴闂ㄧ殑璺緞锟?
            return targetDeptPath.startsWith(deptPath);
        }

        // CUSTOM - 鑷畾涔夎鍒欙紙闇€瑕佹煡锟絪ys_data_permission_rule锟?
        if ("CUSTOM".equals(dataScope)) {
            // TODO: 鏌ヨ鑷畾涔夋暟鎹潈闄愯锟?
            log.warn("CUSTOM data scope not fully implemented yet for userId: {}", userId);
            return true; // 涓存椂杩斿洖 true
        }

        return false;
    }

    /**
     * 妫€鏌ョ敤鎴锋槸鍚﹀彲浠ユ搷浣滄寚瀹氳祫锟?
     *
     * @param userId 鐢ㄦ埛 ID
     * @param resourceOwnerId 璧勬簮鎵€鏈夛拷ID
     * @param resourceDeptId 璧勬簮鎵€灞為儴锟絀D
     * @param dataScope 鏁版嵁鏉冮檺鑼冨洿
     * @return true=鍙互鎿嶄綔, false=涓嶅彲鎿嶄綔
     */
    public boolean canOperateResource(UUID userId, UUID resourceOwnerId, UUID resourceDeptId, String dataScope) {
        // ALL - 鍙互鎿嶄綔鎵€鏈夎祫锟?
        if ("ALL".equals(dataScope)) {
            return true;
        }

        // SELF - 鍙兘鎿嶄綔鑷繁鍒涘缓鐨勮祫锟?
        if ("SELF".equals(dataScope)) {
            return userId.equals(resourceOwnerId);
        }

        // DEPT, DEPT_AND_SUB, CUSTOM - 闇€瑕佺粨鍚堥儴闂ㄤ俊鎭垽锟?
        if ("DEPT".equals(dataScope) || "DEPT_AND_SUB".equals(dataScope) || "CUSTOM".equals(dataScope)) {
            UUID userDeptId = permissionQueryService.getUserDeptId(userId);
            String deptPath = null;
            String targetDeptPath = null;

            if (userDeptId != null) {
                deptPath = permissionQueryService.getDeptPath(userDeptId);
            }

            if (resourceDeptId != null) {
                targetDeptPath = permissionQueryService.getDeptPath(resourceDeptId);
            }

            return canAccessDepartmentData(userId, userDeptId, resourceDeptId, dataScope, deptPath, targetDeptPath);
        }

        return false;
    }

    /**
     * 妫€鏌ユ寜閽潈锟?
     *
     * @param userId 鐢ㄦ埛 ID
     * @param buttonCode 鎸夐挳鏉冮檺缂栫爜
     * @return true=鍙, false=涓嶅彲锟?
     */
    public boolean hasButtonPermission(UUID userId, String buttonCode) {
        return hasPermission(userId, buttonCode);
    }

    /**
     * 鑾峰彇鐢ㄦ埛鐨勬暟鎹潈闄愯寖锟?
     *
     * @param userId 鐢ㄦ埛 ID
     * @return 鏁版嵁鏉冮檺鑼冨洿锛圓LL, DEPT, DEPT_AND_SUB, SELF, CUSTOM锟?
     */
    public String getUserDataScope(UUID userId) {
        return permissionQueryService.getUserDataScope(userId);
    }

    /**
     * 鑾峰彇鐢ㄦ埛鍙闂殑閮ㄩ棬 ID鍒楄〃
     *
     * @param userId 鐢ㄦ埛 ID
     * @param tenantId 绉熸埛 ID
     * @return 鍙闂殑閮ㄩ棬 ID鍒楄〃
     */
    public List<UUID> getAccessibleDepartmentIds(UUID userId, UUID tenantId) {
        String dataScope = getUserDataScope(userId);
        return permissionQueryService.getAccessibleDepartmentIds(userId, tenantId, dataScope);
    }

    /**
     * 妫€鏌ョ敤鎴锋槸鍚﹀彲浠ュ垎閰嶆寚瀹氳锟?
     *
     * @param operatorUserId 鎿嶄綔鑰呯敤锟絀D
     * @param operatorRoleLevel 鎿嶄綔鑰呰鑹茬瓑锟?
     * @param targetRoleLevel 鐩爣瑙掕壊绛夌骇
     * @return true=鍙互鍒嗛厤, false=涓嶅彲鍒嗛厤
     */
    public boolean canAssignRole(UUID operatorUserId, Integer operatorRoleLevel, Integer targetRoleLevel) {
        // 瑙掕壊绛夌骇瓒婂ぇ鏉冮檺瓒婇珮
        // 鍙兘鍒嗛厤绛夌骇涓嶉珮浜庤嚜宸辩殑瑙掕壊
        if (operatorRoleLevel == null || targetRoleLevel == null) {
            return false;
        }

        return operatorRoleLevel >= targetRoleLevel;
    }

    /**
     * 瑕佹眰蹇呴』鍙互鍒嗛厤鎸囧畾瑙掕壊锛屽惁鍒欐姏鍑哄紓锟?
     *
     * @param operatorUserId 鎿嶄綔鑰呯敤锟絀D
     * @param operatorRoleLevel 鎿嶄綔鑰呰鑹茬瓑锟?
     * @param targetRoleLevel 鐩爣瑙掕壊绛夌骇
     * @throws BusinessException 濡傛灉涓嶅彲鍒嗛厤
     */
    public void requireRoleAssignmentPermission(UUID operatorUserId, Integer operatorRoleLevel, Integer targetRoleLevel) {
        if (!canAssignRole(operatorUserId, operatorRoleLevel, targetRoleLevel)) {
            log.warn("瑙掕壊鍒嗛厤鏉冮檺涓嶈冻: operatorUserId={}, operatorLevel={}, targetLevel={}",
                operatorUserId, operatorRoleLevel, targetRoleLevel);
            throw new BusinessException(ResultCode.ROLE_ASSIGNMENT_DENIED.getCode(), ResultCode.ROLE_ASSIGNMENT_DENIED.getMessage());
        }
    }
}
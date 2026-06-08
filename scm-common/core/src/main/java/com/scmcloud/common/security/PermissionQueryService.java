package com.scmcloud.common.security;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 鏉冮檺鏌ヨ鏈嶅姟鎺ュ彛

 * 鎻愪緵鏉冮檺銆佽鑹层€佹暟鎹潈闄愮瓑鏌ヨ鍔熻兘
 *
 * @author Claude Code
 * @since 2025-01-24
 */
public interface PermissionQueryService {

    /**
     * 鏌ヨ鐢ㄦ埛鐨勬墍鏈夋潈闄愮紪锟?
     *
     * @param userId 鐢ㄦ埛 ID
     * @return 鏉冮檺缂栫爜闆嗗悎
     */
    Set<String> getUserPermissions(UUID userId);

    /**
     * 鏌ヨ鐢ㄦ埛鐨勬墍鏈夎鑹茬紪锟?
     *
     * @param userId 鐢ㄦ埛 ID
     * @return 瑙掕壊缂栫爜闆嗗悎
     */
    Set<String> getUserRoles(UUID userId);

    /**
     * 鑾峰彇鐢ㄦ埛鐨勬暟鎹潈闄愯寖锟?
     *
     * @param userId 鐢ㄦ埛 ID
     * @return 鏁版嵁鏉冮檺鑼冨洿瀛楃涓诧紙ALL, DEPT, DEPT_AND_SUB, SELF, CUSTOM锟?
     */
    String getUserDataScope(UUID userId);

    /**
     * 鑾峰彇鐢ㄦ埛鐨勯儴锟絀D
     *
     * @param userId 鐢ㄦ埛 ID
     * @return 閮ㄩ棬 ID
     */
    UUID getUserDeptId(UUID userId);

    /**
     * 鑾峰彇閮ㄩ棬璺緞
     *
     * @param deptId 閮ㄩ棬 ID
     * @return 閮ㄩ棬璺緞
     */
    String getDeptPath(UUID deptId);

    /**
     * 鑾峰彇鐢ㄦ埛鍙闂殑閮ㄩ棬 ID鍒楄〃
     *
     * @param userId 鐢ㄦ埛 ID
     * @param tenantId 绉熸埛 ID
     * @param dataScope 鏁版嵁鏉冮檺鑼冨洿
     * @return 鍙闂殑閮ㄩ棬 ID鍒楄〃
     */
    List<UUID> getAccessibleDepartmentIds(UUID userId, UUID tenantId, String dataScope);

    /**
     * 鑾峰彇瑙掕壊绛夌骇
     *
     * @param roleId 瑙掕壊 ID
     * @return 瑙掕壊绛夌骇
     */
    Integer getRoleLevel(UUID roleId);

    /**
     * 鑾峰彇鐢ㄦ埛鐨勬渶楂樿鑹茬瓑锟?
     *
     * @param userId 鐢ㄦ埛 ID
     * @return 鏈€楂樿鑹茬瓑锟?
     */
    Integer getUserMaxRoleLevel(UUID userId);
}
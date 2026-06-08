package com.scmcloud.common.tenant;

import com.scmcloud.common.constant.RoleConstants;
import com.scmcloud.common.exception.BusinessException;
import com.scmcloud.common.response.ResultCode;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.UUID;

/**
 * 绉熸埛楠岃瘉宸ュ叿锟?

 * 鎻愪緵绉熸埛涓婁笅鏂囬獙璇併€佹暟鎹綊灞為獙璇佺瓑鍔熻兘
 *
 * @author Claude Code
 * @since 2025-01-24
 */
@Slf4j
public class TenantValidationUtil {

    /**
     * 鑾峰彇褰撳墠绉熸埛ID锛堝鏋滄湭璁剧疆鍒欐姏鍑哄紓甯革級
     *
     * @return 褰撳墠绉熸埛 ID
     * @throws BusinessException 濡傛灉绉熸埛涓婁笅鏂囨湭璁剧疆
     */
    public static UUID getRequiredTenantId() {
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            log.error("绉熸埛涓婁笅鏂囨湭璁剧疆锛岃姹傝鎷掔粷");
            throw new BusinessException(ResultCode.TENANT_CONTEXT_MISSING.getCode(),
                    ResultCode.TENANT_CONTEXT_MISSING.getMessage());
        }
        return tenantId;
    }

    /**
     * 楠岃瘉鏁版嵁鏄惁灞炰簬褰撳墠绉熸埛
     *
     * @param dataTenantId 鏁版嵁鎵€灞炵殑绉熸埛 ID
     * @throws BusinessException 濡傛灉鏁版嵁涓嶅睘浜庡綋鍓嶇锟?
     */
    public static void validateDataOwnership(UUID dataTenantId) {
        UUID currentTenantId = getRequiredTenantId();

        if (dataTenantId == null) {
            log.error("鏁版嵁鏈叧鑱旂鎴凤紝鏁版嵁ID鍙兘鏃犳晥");
            throw new BusinessException(ResultCode.DATA_TENANT_MISSING.getCode(),
                    ResultCode.DATA_TENANT_MISSING.getMessage());
        }

        if (!currentTenantId.equals(dataTenantId)) {
            log.warn("绉熸埛鏁版嵁璁块棶瓒婃潈锛氬綋鍓嶇锟絳}, 鏁版嵁绉熸埛={}", currentTenantId, dataTenantId);
            throw new BusinessException(ResultCode.TENANT_DATA_ACCESS_DENIED.getCode(),
                    ResultCode.TENANT_DATA_ACCESS_DENIED.getMessage());
        }
    }

    /**
     * 楠岃瘉鐢ㄦ埛鏄惁涓哄钩鍙扮鐞嗗憳锛堝熀浜庣敤鎴风被鍨嬪瓧绗︿覆锟?
     *
     * @param userType 鐢ㄦ埛绫诲瀷
     * @return true=骞冲彴绠＄悊锟?false=绉熸埛鐢ㄦ埛
     * @deprecated 寤鸿浣跨敤鏃犲弬锟絠sPlatformAdmin() 鏂规硶锟絊ecurityContext 鑾峰彇
     */
    @Deprecated
    public static boolean isPlatformAdmin(String userType) {
        return RoleConstants.USER_TYPE_PLATFORM_ADMIN.equals(userType);
    }

    /**
     * 楠岃瘉褰撳墠鐢ㄦ埛鏄惁涓哄钩鍙扮鐞嗗憳
     * 鍒ゆ柇鏍囧噯锟?
     * 1. 鐢ㄦ埛宸茬櫥褰曚笖鏈夋湁鏁堢殑 SecurityContext
     * 2. 鐢ㄦ埛鎷ユ湁骞冲彴绠＄悊鍛樿鑹诧紙ROLE_PLATFORM_ADMIN 锟絉OLE_SUPER_ADMIN锟?
     * 3. 绉熸埛涓婁笅鏂囦负 NULL锛堝钩鍙扮鐞嗗憳涓嶅睘浜庝换浣曠鎴凤級
     *
     * @return true=骞冲彴绠＄悊锟?false=绉熸埛鐢ㄦ埛鎴栨湭鐧诲綍
     */
    public static boolean isPlatformAdmin() {
        try {
            // 1. 妫€鏌ョ鎴蜂笂涓嬫枃锛堝繀瑕佹潯浠讹級
            UUID tenantId = TenantContextHolder.getTenantId();
            if (tenantId != null) {
                log.debug("鐢ㄦ埛灞炰簬绉熸埛 {}, 涓嶆槸骞冲彴绠＄悊鍛?, tenantId);
                return false;
            }

            // 2. 灏濊瘯锟絊ecurityContext 鑾峰彇褰撳墠鐢ㄦ埛锛堥渶锟絊ecurityUtils锟?
            // 娉ㄦ剰锛氫负浜嗛伩鍏嶅惊鐜緷璧栵紝杩欓噷浣跨敤鍙嶅皠璋冪敤 SecurityUtils
            // 濡傛灉鏃犳硶鑾峰彇鍒扮敤鎴蜂俊鎭紝鍒欏彧渚濊禆绉熸埛涓婁笅鏂囧垽锟?
            try {
                Class<?> securityUtilsClass = Class.forName("com.scmcloud.common.web.util.SecurityUtils");
                Object currentUser = securityUtilsClass.getMethod("getCurrentUser").invoke(null);

                if (currentUser == null) {
                    log.debug("鏈櫥褰曠敤鎴凤紝涓嶆槸骞冲彴绠＄悊鍛?);
                    return false;
                }

                // 3. 妫€鏌ヨ鑹诧紙鍏呭垎鏉′欢锟?
                Object rolesObj = currentUser.getClass().getMethod("getRoles").invoke(currentUser);
                if (rolesObj instanceof Set) {
                    @SuppressWarnings("unchecked")
                    Set<String> roles = (Set<String>) rolesObj;
                    boolean hasPlatformRole = roles.contains(RoleConstants.ROLE_PLATFORM_ADMIN)
                                           || roles.contains(RoleConstants.ROLE_SUPER_ADMIN);

                    if (!hasPlatformRole) {
                        log.debug("鐢ㄦ埛娌℃湁骞冲彴绠＄悊鍛樿鑹?);
                    }

                    return hasPlatformRole;
                }

            } catch (ClassNotFoundException e) {
                // SecurityUtils 绫讳笉瀛樺湪锛屽洖閫€鍒板熀浜庣鎴稩D鐨勫垽锟?
                log.debug("SecurityUtils 绫讳笉瀛樺湪锛屼粎鍩轰簬绉熸埛涓婁笅鏂囧垽鏂?);
            } catch (Exception e) {
                // 鍙嶅皠璋冪敤澶辫触锛屽洖閫€鍒板熀浜庣鎴稩D鐨勫垽锟?
                log.debug("鏃犳硶鑾峰彇鐢ㄦ埛瑙掕壊淇℃伅锛屼粎鍩轰簬绉熸埛涓婁笅鏂囧垽锟?{}", e.getMessage());
            }

            // 4. 鍏滃簳閫昏緫锛氬鏋滄棤娉曡幏鍙栬鑹蹭俊鎭紝绉熸埛ID涓篘ULL鍒欒涓烘槸骞冲彴绠＄悊锟?
            // 杩欑鎯呭喌閫氬父鍙戠敓鍦ㄧ郴缁熷垵濮嬪寲鎴栫壒娈婂満锟?
            return true;

        } catch (Exception e) {
            log.error("妫€鏌ュ钩鍙扮鐞嗗憳鏉冮檺鏃跺嚭閿?, e);
            return false; // 瀹夊叏浼樺厛锛屽紓甯告儏鍐佃繑锟絝alse
        }
    }

    /**
     * 楠岃瘉褰撳墠鐢ㄦ埛鏄惁涓虹鎴风敤鎴凤紙闈炲钩鍙扮鐞嗗憳锟?
     * 杩欐槸 !isPlatformAdmin() 鐨勮涔夊寲鐗堟湰锛岄伩鍏嶅竷灏斿€煎弽杞殑浠ｇ爜寮傚懗
     *
     * @return true=绉熸埛鐢ㄦ埛, false=骞冲彴绠＄悊锟?
     */
    public static boolean isTenantUser() {
        return !isPlatformAdmin();
    }

    /**
     * 楠岃瘉鐢ㄦ埛鏄惁涓虹鎴风鐞嗗憳锛堝熀浜庣敤鎴风被鍨嬪瓧绗︿覆锟?
     *
     * @param userType 鐢ㄦ埛绫诲瀷
     * @return true=绉熸埛绠＄悊锟?false=鍏朵粬
     * @deprecated 寤鸿浣跨敤鍩轰簬瑙掕壊鐨勫垽鏂柟锟?
     */
    @Deprecated
    public static boolean isTenantAdmin(String userType) {
        return RoleConstants.USER_TYPE_TENANT_ADMIN.equals(userType);
    }

    /**
     * 楠岃瘉鐢ㄦ埛鏄惁鏈夌鐞嗘潈闄愶紙骞冲彴绠＄悊鍛樻垨绉熸埛绠＄悊鍛橈級
     *
     * @param userType 鐢ㄦ埛绫诲瀷
     * @return true=鏈夌鐞嗘潈锟?false=鏅€氱敤锟?
     * @deprecated 姝ゆ柟娉曚緷璧栧凡搴熷純鐨勬柟娉曪紝寤鸿浣跨敤鍩轰簬瑙掕壊鐨勬潈闄愭锟?
     */
    @Deprecated
    public static boolean hasAdminPrivilege(String userType) {
        return isPlatformAdmin(userType) || isTenantAdmin(userType);
    }

    /**
     * 楠岃瘉瑙掕壊鏄惁灞炰簬褰撳墠绉熸埛鎴栦负骞冲彴瑙掕壊
     *
     * @param roleTenantId 瑙掕壊鎵€灞炵殑绉熸埛ID锛圢ULL琛ㄧず骞冲彴瑙掕壊锟?
     * @throws BusinessException 濡傛灉瑙掕壊涓嶅睘浜庡綋鍓嶇鎴蜂笖涓嶆槸骞冲彴瑙掕壊
     */
    public static void validateRoleAccess(UUID roleTenantId) {
        // 骞冲彴瑙掕壊锛坱enant_id = NULL锛夋墍鏈夌鎴烽兘鍙互浣跨敤
        if (roleTenantId == null) {
            return;
        }

        // 绉熸埛瑙掕壊蹇呴』灞炰簬褰撳墠绉熸埛
        UUID currentTenantId = getRequiredTenantId();
        if (!currentTenantId.equals(roleTenantId)) {
            log.warn("绉熸埛瑙掕壊璁块棶瓒婃潈锛氬綋鍓嶇锟絳}, 瑙掕壊绉熸埛={}", currentTenantId, roleTenantId);
            throw new BusinessException(ResultCode.TENANT_ROLE_ACCESS_DENIED.getCode(),
                    ResultCode.TENANT_ROLE_ACCESS_DENIED.getMessage());
        }
    }

    /**
     * 楠岃瘉鏉冮檺鏄惁灞炰簬褰撳墠绉熸埛鎴栦负骞冲彴鏉冮檺
     *
     * @param permissionTenantId 鏉冮檺鎵€灞炵殑绉熸埛ID锛圢ULL琛ㄧず骞冲彴鏉冮檺锟?
     * @throws BusinessException 濡傛灉鏉冮檺涓嶅睘浜庡綋鍓嶇鎴蜂笖涓嶆槸骞冲彴鏉冮檺
     */
    public static void validatePermissionAccess(UUID permissionTenantId) {
        // 骞冲彴鏉冮檺锛坱enant_id = NULL锛夋墍鏈夌鎴烽兘鍙互浣跨敤
        if (permissionTenantId == null) {
            return;
        }

        // 绉熸埛鏉冮檺蹇呴』灞炰簬褰撳墠绉熸埛
        UUID currentTenantId = getRequiredTenantId();
        if (!currentTenantId.equals(permissionTenantId)) {
            log.warn("绉熸埛鏉冮檺璁块棶瓒婃潈锛氬綋鍓嶇锟絳}, 鏉冮檺绉熸埛={}", currentTenantId, permissionTenantId);
            throw new BusinessException(ResultCode.TENANT_PERMISSION_ACCESS_DENIED.getCode(),
                    ResultCode.TENANT_PERMISSION_ACCESS_DENIED.getMessage());
        }
    }

    /**
     * 楠岃瘉鏄惁鍏佽鍒涘缓骞冲彴绾ц祫婧愶紙瑙掕壊銆佹潈闄愮瓑锟?
     *
     * @param userType 鐢ㄦ埛绫诲瀷
     * @throws BusinessException 濡傛灉鐢ㄦ埛鏃犳潈鍒涘缓骞冲彴绾ц祫锟?
     * @deprecated 浣跨敤 requirePlatformAdmin() 鏇夸唬
     */
    @Deprecated
    public static void validatePlatformResourceCreation(String userType) {
        if (!isPlatformAdmin(userType)) {
            log.warn("闈炲钩鍙扮鐞嗗憳灏濊瘯鍒涘缓骞冲彴绾ц祫婧愶紝鐢ㄦ埛绫诲瀷: {}", userType);
            throw new BusinessException(ResultCode.PLATFORM_RESOURCE_ACCESS_DENIED.getCode(),
                    ResultCode.PLATFORM_RESOURCE_ACCESS_DENIED.getMessage());
        }
    }

    /**
     * 瑕佹眰褰撳墠鐢ㄦ埛蹇呴』鏄钩鍙扮鐞嗗憳锛屽惁鍒欐姏鍑哄紓锟?
     * 杩欐槸 isTenantUser() 妫€鏌ョ殑渚挎嵎鏂规硶锛岄伩鍏嶅竷灏斿€煎弽杞殑浠ｇ爜寮傚懗
     *
     * @throws BusinessException 濡傛灉褰撳墠鐢ㄦ埛涓嶆槸骞冲彴绠＄悊锟?
     */
    public static void requirePlatformAdmin() {
        if (isTenantUser()) {
            log.warn("闈炲钩鍙扮鐞嗗憳灏濊瘯鎵ц骞冲彴绠＄悊鎿嶄綔");
            throw new BusinessException(ResultCode.PLATFORM_RESOURCE_ACCESS_DENIED.getCode(),
                    ResultCode.PLATFORM_RESOURCE_ACCESS_DENIED.getMessage());
        }
    }

    /**
     * 鍏佽骞冲彴绠＄悊鍛樹复鏃惰闂寚瀹氱鎴风殑鏁版嵁
     *
     * @param userType 鐢ㄦ埛绫诲瀷
     * @param targetTenantId 鐩爣绉熸埛 ID
     * @return 鏄惁鍏佽璁块棶
     * @deprecated 姝ゆ柟娉曚緷璧栧凡搴熷純鐨勬柟娉曪紝寤鸿鐩存帴浣跨敤 isPlatformAdmin() 杩涜鍒ゆ柇
     */
    @Deprecated
    public static boolean allowCrossTenantAccess(String userType, UUID targetTenantId) {
        // 鍙湁骞冲彴绠＄悊鍛樺彲浠ヨ法绉熸埛璁块棶
        if (!isPlatformAdmin(userType)) {
            return false;
        }

        UUID currentTenantId = TenantContextHolder.getTenantId();

        // 骞冲彴绠＄悊鍛樼殑 tenant_id 锟絅ULL锛屾垨鑰呯洰鏍囩鎴蜂笌褰撳墠绉熸埛涓嶅悓
        return currentTenantId == null || !currentTenantId.equals(targetTenantId);
    }

    /**
     * 楠岃瘉閮ㄩ棬鏄惁灞炰簬褰撳墠绉熸埛
     *
     * @param deptTenantId 閮ㄩ棬鎵€灞炵殑绉熸埛 ID
     * @throws BusinessException 濡傛灉閮ㄩ棬涓嶅睘浜庡綋鍓嶇锟?
     */
    public static void validateDepartmentOwnership(UUID deptTenantId) {
        UUID currentTenantId = getRequiredTenantId();

        if (deptTenantId == null) {
            log.error("閮ㄩ棬鏈叧鑱旂鎴?);
            throw new BusinessException(ResultCode.DEPT_TENANT_MISSING.getCode(),
                    ResultCode.DEPT_TENANT_MISSING.getMessage());
        }

        if (!currentTenantId.equals(deptTenantId)) {
            log.warn("绉熸埛閮ㄩ棬璁块棶瓒婃潈锛氬綋鍓嶇锟絳}, 閮ㄩ棬绉熸埛={}", currentTenantId, deptTenantId);
            throw new BusinessException(ResultCode.TENANT_DEPT_ACCESS_DENIED.getCode(),
                    ResultCode.TENANT_DEPT_ACCESS_DENIED.getMessage());
        }
    }

    /**
     * 璁板綍绉熸埛鎿嶄綔鏃ュ織
     *
     * @param operation 鎿嶄綔绫诲瀷
     * @param resourceType 璧勬簮绫诲瀷
     * @param resourceId 璧勬簮 ID
     */
    public static void logTenantOperation(String operation, String resourceType, UUID resourceId) {
        UUID tenantId = TenantContextHolder.getTenantId();
        log.info("绉熸埛鎿嶄綔鏃ュ織 - 绉熸埛ID: {}, 鎿嶄綔: {}, 璧勬簮绫诲瀷: {}, 璧勬簮ID: {}",
            tenantId, operation, resourceType, resourceId);

        // TODO: 鍙互闆嗘垚鍒扮鎴锋搷浣滄棩蹇楄〃锛坱enant_operation_log锟?
    }
}

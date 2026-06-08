package com.scmcloud.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.scmcloud.common.dto.role.RoleDTO;
import com.scmcloud.system.domain.entity.SysRole;

import java.util.List;
import java.util.UUID;

/**
 * <p>
 * 瑙掕壊锟芥湇鍔★拷
 * </p>
 *
 * @author author
 * @since 2025-10-15
 */
public interface ISysRoleService extends IService<SysRole> {

    /**
     * 鍒嗛〉鏌ヨ瑙掕壊鍒楄〃锟?
     *
     * @param pageNum  椤电爜锛屼粠 1 寮€锟?
     * @param pageSize 姣忛〉鏁伴噺
     * @param roleName 瑙掕壊鍚嶇О锛堝彲閫夛紝鏀寔妯＄硦鏌ヨ锟?
     * @return 瑙掕壊鍒嗛〉鏁版嵁
     */
    Page<RoleDTO> listRoles(Integer pageNum, Integer pageSize, String roleName);

    /**
     * 鏌ヨ鎵€鏈夎鑹诧紙涓嶅垎椤碉級锟?
     *
     * @return 鍏ㄩ儴瑙掕壊鍒楄〃
     */
    List<RoleDTO> listAllRoles();

    /**
     * 鏍规嵁瑙掕壊ID鑾峰彇瑙掕壊璇︽儏锟?
     *
     * @param id 瑙掕壊 ID
     * @return 瑙掕壊璇︽儏
     */
    RoleDTO getRoleById(UUID id);

    /**
     * 鏂板瑙掕壊锟?
     *
     * @param roleDTO 瑙掕壊淇℃伅
     */
    void addRole(RoleDTO roleDTO);

    /**
     * 淇敼瑙掕壊锟?
     *
     * @param roleDTO 瑙掕壊淇℃伅
     */
    void updateRole(RoleDTO roleDTO);

    /**
     * 鍒犻櫎瑙掕壊锟?
     *
     * @param id 瑙掕壊 ID
     */
    void deleteRole(UUID id);

    /**
     * 涓鸿鑹叉巿浜堟潈闄愶拷
     *
     * @param roleId        瑙掕壊 ID
     * @param permissionIds 鏉冮檺 ID鍒楄〃
     */
    void grantPermissions(UUID roleId, List<UUID> permissionIds);

    /**
     * 鑾峰彇瑙掕壊宸茬粦瀹氱殑鏉冮檺ID鍒楄〃锟?
     *
     * @param roleId 瑙掕壊 ID
     * @return 鏉冮檺 ID鍒楄〃
     */
    List<UUID> getRolePermissionIds(UUID roleId);
}

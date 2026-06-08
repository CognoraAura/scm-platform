package com.scmcloud.system.sync.executor;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.scmcloud.system.mapper.SysDeptMapper;
import com.scmcloud.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * 鐢ㄦ埛鍚屾鎵ц锟?
 * <p>
 * 鐙珛锟紹ean锛岀敤浜庢墽琛岃法搴撲簨鍔℃搷浣滐拷
 * 閬垮厤 @Transactional 鑷皟鐢ㄩ棶锟?
 *
 * @author Deng
 * @since 2025-12-16
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserSyncExecutor {
    private final SysUserRoleMapper userRoleMapper;
    private final SysDeptMapper deptMapper;

    /**
     * 鍚屾鐢ㄦ埛淇℃伅锟絧ermission 锟?
     *
     * @param userId 鐢ㄦ埛 ID
     * @param data   鐢ㄦ埛鏁版嵁
     */
    @DS("permission")
    @Transactional(rollbackFor = Exception.class)
    public void syncToPermissionDb(UUID userId, Map<String, Object> data) {
        String username = (String) data.get("username");
        String realName = (String) data.get("realName");
        Integer status = (Integer) data.get("status");

        int updated = userRoleMapper.updateUserRedundancy(userId, username, realName, status);
        log.debug("[UserSync] Updated {} rows in sys_user_role for user: {}", updated, userId);
    }

    /**
     * 鍚屾鐢ㄦ埛淇℃伅锟給rg 搴擄紙璐熻矗浜轰俊鎭級
     *
     * @param userId 鐢ㄦ埛 ID
     * @param data   鐢ㄦ埛鏁版嵁
     */
    @DS("org")
    @Transactional(rollbackFor = Exception.class)
    public void syncToOrgDb(UUID userId, Map<String, Object> data) {
        String realName = (String) data.get("realName");
        String phone = (String) data.get("phone");

        int updated = deptMapper.updateLeaderRedundancy(userId, realName, phone);
        log.debug("[UserSync] Updated {} rows in sys_dept for leader: {}", updated, userId);
    }

    /**
     * 鏍囪鐢ㄦ埛锟絧ermission 搴撲腑宸插垹锟?
     *
     * @param userId 鐢ㄦ埛 ID
     */
    @DS("permission")
    @Transactional(rollbackFor = Exception.class)
    public void markDeletedInPermissionDb(UUID userId) {
        userRoleMapper.updateUserStatus(userId, 0);
        log.debug("[UserSync] Marked user as deleted in permission db: {}", userId);
    }
}
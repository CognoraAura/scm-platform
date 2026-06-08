package com.scmcloud.system.sync.executor;

import com.baomidou.dynamic.datasource.annotation.DS;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 瑙掕壊鍚屾鎵ц锟?
 * <p>
 * 鐙珛锟紹ean锛岀敤浜庢墽琛岃法搴撲簨鍔℃搷浣滐拷
 * 閬垮厤 @Transactional 鑷皟鐢ㄩ棶锟?
 *
 * @author Deng
 * @since 2025-12-16
 */
@Slf4j
@Component
public class RoleSyncExecutor {

    /**
     * 鍚屾瑙掕壊淇℃伅锟絘pproval 锟?
     *
     * @param roleId   瑙掕壊 ID
     * @param roleName 瑙掕壊鍚嶇О
     * @param roleCode 瑙掕壊缂栫爜
     */
    @DS("approval")
    @Transactional(rollbackFor = Exception.class)
    public void syncToApprovalDb(UUID roleId, String roleName, String roleCode) {
        // 鏇存柊鍖呭惈璇ヨ鑹茬殑瀹℃壒璁板綍锟絩ole_names 鏁扮粍
        log.debug("[RoleSync] Would update approval records for role: {}, name: {}", roleId, roleName);
    }

    /**
     * 鏍囪瑙掕壊锟絘pproval 搴撲腑宸插垹锟?
     *
     * @param roleId 瑙掕壊 ID
     */
    @DS("approval")
    @Transactional(rollbackFor = Exception.class)
    public void markRoleDeletedInApprovalDb(UUID roleId) {
        log.debug("[RoleSync] Would mark role as deleted in approval db: {}", roleId);
    }
}
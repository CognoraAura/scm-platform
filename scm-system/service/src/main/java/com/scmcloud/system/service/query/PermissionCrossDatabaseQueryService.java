package com.scmcloud.system.service.query;

import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.common.dto.permission.PermissionDTO;
import com.scmcloud.system.mapper.SysPermissionMapper;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 鏉冮檺璺ㄥ簱鏌ヨ鏈嶅姟
 * <p>
 * 澶勭悊涓庢潈闄愮浉鍏崇殑璺ㄥ簱鏌ヨ鎿嶄綔锛坉b_permission 锟絛b_user锟?
 *
 * @author Deng
 * @since 2025-01-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionCrossDatabaseQueryService {
    private final SysPermissionMapper permissionMapper;

    /**
     * 鏌ヨ鐢ㄦ埛鑿滃崟锟?
     * <p>
     * 鏇夸唬 SysPermissionMapper.findMenuTreeByUserId
     * 璺ㄥ簱鏌ヨ锛歞b_permission
     *
     * @param userId 鐢ㄦ埛 ID
     * @return 鑿滃崟鏉冮檺 DTO 鍒楄〃
     */
    @Slave
    @Timed(value = "cross_db_query", extraTags = {"method", "findMenuTreeByUserId"})
    @Cacheable(value = "userMenuTree", key = "#userId", unless = "#result.isEmpty()")
    public List<PermissionDTO> findMenuTreeByUserId(UUID userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return permissionMapper.findMenuTreeByUserId(userId);
    }
}
package com.scmcloud.common.security.util;

import com.scmcloud.common.web.domain.SecurityUser;
import com.scmcloud.common.web.util.SecurityUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
/**
 * 鏉冮檺楠岃瘉宸ュ叿锟?
 * 鎻愪緵缂栫▼寮忔潈闄愰獙璇佹柟锟?
 *
 * @author Deng
 * createData 2025/10/30 13:43
 * @version 1.0
 */
@Component
public class PermissionCheckUtil {

    /**
     * 楠岃瘉褰撳墠鐢ㄦ埛鏄惁鎷ユ湁鎸囧畾鏉冮檺
     */
    public boolean hasPermission(String permission) {
        return executeWithUser(user -> {
            Set<String> permissions = user.getPermissions();
            return permissions != null && permissions.contains(permission);
        });
    }

    /**
     * 楠岃瘉褰撳墠鐢ㄦ埛鏄惁鎷ユ湁浠讳竴鏉冮檺
     */
    public boolean hasAnyPermission(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }
        
        return executeWithUser(user -> {
            Set<String> userPermissions = user.getPermissions();
            if (userPermissions == null || userPermissions.isEmpty()) {
                return false;
            }

            for (String permission : permissions) {
                if (userPermissions.contains(permission)) {
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * 楠岃瘉褰撳墠鐢ㄦ埛鏄惁鎷ユ湁鎵€鏈夋潈锟?
     */
    public boolean hasAllPermissions(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }
        
        return executeWithUser(user -> {
            Set<String> userPermissions = user.getPermissions();
            if (userPermissions == null || userPermissions.isEmpty()) {
                return false;
            }

            for (String permission : permissions) {
                if (!userPermissions.contains(permission)) {
                    return false;
                }
            }
            return true;
        });
    }

    /**
     * 楠岃瘉褰撳墠鐢ㄦ埛鏄惁鎷ユ湁鎸囧畾瑙掕壊
     */
    public boolean hasRole(String role) {
        return executeWithUser(user -> {
            Set<String> roles = user.getRoles();
            return roles != null && roles.contains(role);
        });
    }

    /**
     * 楠岃瘉褰撳墠鐢ㄦ埛鏄惁鎷ユ湁浠讳竴瑙掕壊
     */
    public boolean hasAnyRole(String... roles) {
        if (roles == null || roles.length == 0) {
            return false;
        }
        
        return executeWithUser(user -> {
            Set<String> userRoles = user.getRoles();
            if (userRoles == null || userRoles.isEmpty()) {
                return false;
            }

            for (String role : roles) {
                if (userRoles.contains(role)) {
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * 楠岃瘉褰撳墠鐢ㄦ埛鏄惁鎷ユ湁鎵€鏈夎锟?
     */
    public boolean hasAllRoles(String... roles) {
        if (roles == null || roles.length == 0) {
            return false;
        }
        
        return executeWithUser(user -> {
            Set<String> userRoles = user.getRoles();
            if (userRoles == null || userRoles.isEmpty()) {
                return false;
            }

            for (String role : roles) {
                if (!userRoles.contains(role)) {
                    return false;
                }
            }
            return true;
        });
    }

    /**
     * 楠岃瘉鐢ㄦ埛鏄惁涓鸿秴绾х鐞嗗憳
     */
    public boolean isSuperAdmin() {
        return hasRole("ROLE_SUPER_ADMIN");
    }

    /**
     * 楠岃瘉鐢ㄦ埛鏄惁鏈夋潈璁块棶鎸囧畾閮ㄩ棬鐨勬暟锟?
     */
    public boolean canAccessDept(UUID deptId) {
        return executeWithUser(user -> {
            // 瓒呯骇绠＄悊鍛樺彲浠ヨ闂墍鏈夐儴锟?
            if (isSuperAdmin()) {
                return true;
            }

            // 鐢ㄦ埛鑷繁鐨勯儴锟?
            return user.getDeptId() != null && user.getDeptId().equals(deptId);

            // TODO: 鏍规嵁鏁版嵁鏉冮檺鑼冨洿鍒ゆ柇
            // 闇€瑕佹煡璇㈢敤鎴风殑鏁版嵁鏉冮檺閰嶇疆
        });
    }

    /**
     * 楠岃瘉鐢ㄦ埛鏄惁鏈夋潈璁块棶鎸囧畾鐢ㄦ埛鐨勬暟锟?
     */
    public boolean canAccessUser(UUID targetUserId) {
        return executeWithUser(user -> {
            // 瓒呯骇绠＄悊鍛樺彲浠ヨ闂墍鏈夌敤锟?
            if (isSuperAdmin()) {
                return true;
            }

            // 鐢ㄦ埛鍙互璁块棶鑷繁鐨勬暟锟?
            return user.getUserId().equals(targetUserId);

            // TODO: 鏍规嵁鏁版嵁鏉冮檺鑼冨洿鍒ゆ柇
        });
    }

    /**
     * 鑾峰彇褰撳墠鐢ㄦ埛鐨勬墍鏈夋潈锟?
     */
    public Set<String> getCurrentUserPermissions() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Set.of();
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth != null && !auth.startsWith("ROLE_"))
                .collect(Collectors.toSet());
    }

    /**
     * 鑾峰彇褰撳墠鐢ㄦ埛鐨勬墍鏈夎锟?
     */
    public Set<String> getCurrentUserRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Set.of();
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth != null && auth.startsWith("ROLE_"))
                .map(auth -> auth.substring(5)) // 绉婚櫎"ROLE_"鍓嶇紑
                .collect(Collectors.toSet());
    }
    
    /**
     * 浣跨敤褰撳墠鐢ㄦ埛鎵ц鎿嶄綔鐨勯€氱敤鏂规硶(杩斿洖boolean绫诲瀷)
     * @param function 瑕佹墽琛岀殑鎿嶄綔
     * @return 鎿嶄綔缁撴灉鎴栭粯璁わ拷false
     */
    private boolean executeWithUser(Function<SecurityUser, Boolean> function) {
        SecurityUser user = SecurityUtils.getCurrentUser();
        if (user == null) {
            return false;
        }
        return function.apply(user);
    }
}

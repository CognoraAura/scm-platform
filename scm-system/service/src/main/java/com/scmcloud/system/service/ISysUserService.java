package com.scmcloud.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.scmcloud.common.dto.user.UserDTO;
import com.scmcloud.common.dto.user.UserInfo;
import com.scmcloud.common.web.domain.SecurityUser;
import com.scmcloud.system.domain.entity.SysUser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * <p>
 * 鐢ㄦ埛锟経UID v7涓婚敭) 鏈嶅姟锟?
 * </p>
 *
 * @author author
 * @since 2025-10-14
 */
public interface ISysUserService extends IService<SysUser> {

    /**
     * 鍒嗛〉鏌ヨ鐢ㄦ埛鍒楄〃锟?
     *
     * @param pageNum  椤电爜锛屼粠 1 寮€锟?
     * @param pageSize 姣忛〉鏁伴噺
     * @param username 鐢ㄦ埛鍚嶏紙鍙€夛紝鏀寔妯＄硦鏌ヨ锟?
     * @param status   鐢ㄦ埛鐘舵€侊紙鍙€夛紝濡傦細0-绂佺敤锟?鍚敤锟?
     * @return 鐢ㄦ埛鍒嗛〉鏁版嵁
     */
    Page<UserDTO> listUsers(Integer pageNum, Integer pageSize, String username, Integer status);

    /**
     * 鏍规嵁鐢ㄦ埛鍚嶈幏鍙栫敤鎴凤紙鐢ㄤ簬 Spring Security 璁よ瘉锛夛拷
     *
     * @param username 鐢ㄦ埛锟?
     * @return SecurityUser 鐢ㄤ簬璁よ瘉鐨勭敤鎴峰璞★紝鍖呭惈瀵嗙爜銆佽鑹层€佹潈闄愮瓑淇℃伅锛涘鏋滅敤鎴蜂笉瀛樺湪鍒欒繑锟絥ull
     */
    SecurityUser getUserByUsername(String username);

    /**
     * 鏍规嵁鐢ㄦ埛ID鑾峰彇鐢ㄦ埛璇︽儏锟?
     *
     * @param id 鐢ㄦ埛 ID
     * @return 鐢ㄦ埛璇︽儏
     */
    UserDTO getUserById(UUID id);

    /**
     * 鑾峰彇鐢ㄦ埛鐨勭患鍚堜俊鎭紙鍚熀纭€淇℃伅銆佽鑹层€佹潈闄愮瓑锛夛拷
     *
     * @param userId 鐢ㄦ埛 ID
     * @return 鐢ㄦ埛缁煎悎淇℃伅
     */
    UserInfo getUserInfo(UUID userId);

    /**
     * 鏂板鐢ㄦ埛锟?
     *
     * @param userDTO 鐢ㄦ埛淇℃伅
     */
    void addUser(UserDTO userDTO);

    /**
     * 淇敼鐢ㄦ埛淇℃伅锟?
     *
     * @param userDTO 鐢ㄦ埛淇℃伅
     */
    void updateUser(UserDTO userDTO);

    /**
     * 鍒犻櫎鐢ㄦ埛锟?
     *
     * @param id 鐢ㄦ埛 ID
     */
    void deleteUser(UUID id);

    /**
     * 鏇存柊鐢ㄦ埛鏈€杩戜竴娆＄櫥褰曚俊鎭拷
     *
     * @param userId    鐢ㄦ埛 ID
     * @param ipAddress 鐧诲綍 IP 鍦板潃
     */
    void updateLastLogin(UUID userId, String ipAddress);

    /**
     * 閲嶇疆鐢ㄦ埛瀵嗙爜锛岃繑鍥炴柊鐢熸垚鐨勪复鏃跺瘑鐮侊紙鎴栧垵濮嬪瘑鐮侊級锟?
     *
     * @param id 鐢ㄦ埛 ID
     * @return 鏂板瘑鐮佸瓧绗︿覆
     */
    String resetPassword(UUID id);

    /**
     * 淇敼瀵嗙爜锟?
     *
     * @param userId     鐢ㄦ埛 ID
     * @param oldPassword 鏃у瘑锟?
     * @param newPassword 鏂板瘑锟?
     */
    void changePassword(UUID userId, String oldPassword, String newPassword);

    /**
     * 鎺堜簣姘镐箙瑙掕壊锟?
     *
     * @param userId  鐢ㄦ埛 ID
     * @param roleIds 瑙掕壊 ID鍒楄〃
     */
    void grantRoles(UUID userId, List<UUID> roleIds);

    /**
     * 鎺堜簣涓存椂瑙掕壊锛堝湪鏈夋晥鏈熷唴鐢熸晥锛夛拷
     *
     * @param userId        鐢ㄦ埛 ID
     * @param roleIds       瑙掕壊 ID鍒楄〃
     * @param effectiveTime 鐢熸晥鏃堕棿
     * @param expireTime    杩囨湡鏃堕棿
     */
    void grantTemporaryRoles(UUID userId, List<UUID> roleIds,
                             LocalDateTime effectiveTime, LocalDateTime expireTime);

    /**
     * 寤堕暱涓存椂瑙掕壊鐨勬湁鏁堟湡锟?
     *
     * @param userId       鐢ㄦ埛 ID
     * @param roleId       瑙掕壊 ID
     * @param newExpireTime 鏂扮殑杩囨湡鏃堕棿
     */
    void extendTemporaryRole(UUID userId, UUID roleId, LocalDateTime newExpireTime);

    /**
     * 鎻愬墠缁堟涓存椂瑙掕壊锟?
     *
     * @param userId 鐢ㄦ埛 ID
     * @param roleId 瑙掕壊 ID
     */
    void terminateTemporaryRole(UUID userId, UUID roleId);

    /**
     * 鏌ヨ鐢ㄦ埛鐨勪复鏃惰鑹插垪琛拷
     *
     * @param userId 鐢ㄦ埛 ID
     * @return 涓存椂瑙掕壊鍒楄〃锛堝寘鍚鑹蹭笌鏈夋晥鏈熺瓑淇℃伅锟?
     */
    List<Map<String, Object>> getUserTemporaryRoles(UUID userId);

    /**
     * 閿佸畾鎴栬В閿佺敤鎴凤拷
     *
     * @param id   鐢ㄦ埛 ID
     * @param lock 鏄惁閿佸畾锛坱rue 閿佸畾锛沠alse 瑙ｉ攣锟?
     */
    void lockUser(UUID id, Boolean lock);

    /**
     * 妫€鏌ョ敤鎴锋槸鍚︽湁璁块棶鎸囧畾閮ㄩ棬鏁版嵁鐨勬潈闄愶拷
     *
     * @param userId 鐢ㄦ埛 ID
     * @param deptId 閮ㄩ棬 ID
     * @return true 鏈夎闂潈闄愶紱false 鏃犺闂潈锟?
     */
    boolean canAccessDept(UUID userId, UUID deptId);

    /**
     * 鑾峰彇鐢ㄦ埛鐨勬暟鎹潈闄愯寖鍥达拷
     *
     * @param userId 鐢ㄦ埛 ID
     * @return 鏁版嵁鑼冨洿鏍囪瘑锛堜緥濡傦細0-浠呮湰浜猴紝1-鏈儴闂紝2-鏈儴闂ㄥ強浠ヤ笅锟?鍏ㄩ儴绛夛級
     */
    Integer getUserDataScope(UUID userId);

    /**
     * 缁熻鐢ㄦ埛鐩稿叧淇℃伅锛堢櫥褰曟鏁般€佽鑹叉暟閲忋€佹潈闄愭暟閲忕瓑锛夛拷
     *
     * @param userId 鐢ㄦ埛 ID
     * @return 缁熻缁撴灉閿€煎
     */
    Map<String, Object> getUserStatistics(UUID userId);
}

package com.scmcloud.common.dto.role;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 *
 *
 * @author Deng
 * createData 2025/10/15 11:43
 * @version 1.0
 */
@Data
public class RoleDTO {
    private UUID id;

    private UUID tenantId;

    @NotBlank(message = "瑙掕壊缂栫爜涓嶈兘涓虹┖")
    @Pattern(regexp = "^ROLE_[A-Z_]+$", message = "瑙掕壊缂栫爜蹇呴』浠OLE_寮€澶达紝鍙兘鍖呭惈澶у啓瀛楁瘝鍜屼笅鍒掔嚎")
    private String roleCode;

    @NotBlank(message = "瑙掕壊鍚嶇О涓嶈兘涓虹┖")
    @Size(max = 64, message = "瑙掕壊鍚嶇О闀垮害涓嶈兘瓒呰繃64涓瓧绗?)
    private String roleName;

    @Size(max = 255, message = "瑙掕壊鎻忚堪闀垮害涓嶈兘瓒呰繃255涓瓧绗?)
    private String roleDesc;

    private String roleType;

    private String roleCategory;

    @NotNull(message = "瑙掕壊绾у埆涓嶈兘涓虹┖")
    @Min(value = 0, message = "瑙掕壊绾у埆涓嶈兘灏忎簬0")
    @Max(value = 999, message = "瑙掕壊绾у埆涓嶈兘澶т簬999")
    private Integer roleLevel;

    @NotNull(message = "鏁版嵁鏉冮檺涓嶈兘涓虹┖")
    private Integer dataScope;

    @DecimalMin(value = "0.00", message = "鏈€澶у鎵归噾棰濅笉鑳戒负璐熸暟")
    private BigDecimal maxApprovalAmount;

    private String businessScope;

    private List<UUID> customDeptIds;

    private Integer status;

    private Integer sortOrder;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    // 鍏宠仈鏁版嵁
    private List<UUID> permissionIds;
    private List<UUID> deptIds;
    private Integer userCount; // 鎷ユ湁璇ヨ鑹茬殑鐢ㄦ埛锟?
}

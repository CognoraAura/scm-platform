package com.scmcloud.common.dto.permission;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 *
 *
 * @author Deng
 * createData 2025/10/15 11:45
 * @version 1.0
 */
@Data
public class PermissionDTO {
    private UUID id;

    @NotNull(message = "鐖剁骇 ID涓嶈兘涓虹┖")
    private UUID parentId;

    @NotBlank(message = "鏉冮檺缂栫爜涓嶈兘涓虹┖")
    @Pattern(regexp = "^[a-z][a-z0-9:]*$", message = "鏉冮檺缂栫爜鍙兘鍖呭惈灏忓啓瀛楁瘝銆佹暟瀛楀拰鍐掑彿")
    private String permissionCode;

    @NotBlank(message = "鏉冮檺鍚嶇О涓嶈兘涓虹┖")
    @Size(max = 64, message = "鏉冮檺鍚嶇О闀垮害涓嶈兘瓒呰繃64涓瓧绗?)
    private String permissionName;

    @NotNull(message = "鏉冮檺绫诲瀷涓嶈兘涓虹┖")
    @Min(value = 1, message = "鏉冮檺绫诲瀷蹇呴』锟?5涔嬮棿")
    @Max(value = 5, message = "鏉冮檺绫诲瀷蹇呴』锟?5涔嬮棿")
    private Integer permissionType;

    private String routePath;

    private String component;

    private String redirect;

    private String icon;

    private String apiPath;

    private String httpMethod;

    @NotBlank(message = "鏉冮檺褰掑睘涓嶈兘涓虹┖")
    @Pattern(regexp = "^(PLATFORM|TENANT)$", message = "鏉冮檺褰掑睘蹇呴』鏄疨LATFORM鎴朤ENANT")
    private String permissionScope;

    @NotNull(message = "鏉冮檺绛夌骇涓嶈兘涓虹┖")
    @Min(value = 1, message = "鏉冮檺绛夌骇蹇呴』锟?4涔嬮棿")
    @Max(value = 4, message = "鏉冮檺绛夌骇蹇呴』锟?4涔嬮棿")
    private Integer permissionLevel;

    @NotNull(message = "椋庨櫓绛夌骇涓嶈兘涓虹┖")
    @Min(value = 1, message = "椋庨櫓绛夌骇蹇呴』锟?4涔嬮棿")
    @Max(value = 4, message = "椋庨櫓绛夌骇蹇呴』锟?4涔嬮棿")
    private Integer riskLevel;

    private Boolean needApproval;

    private Boolean needTwoFactor;

    private Integer sortOrder;

    private Boolean visible;

    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    // 鏍戝舰缁撴瀯
    private List<PermissionDTO> children;
}

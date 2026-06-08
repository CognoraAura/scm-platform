package com.scmcloud.inventory.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 搴撳瓨璋冩嫧璇锋眰
 *
 * <p>鐢ㄤ簬浠撳簱闂寸殑搴撳瓨杞Щ鎿嶄綔
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Data
public class InventoryTransferRequest {

  /**
   * SKU ID
   */
  @NotBlank(message = "SKU ID 涓嶈兘涓虹┖")
  private String skuId;

  /**
   * 婧愪粨锟絀D锛堜粠鍝釜浠撳簱杞嚭锟?
   */
  @NotBlank(message = "婧愪粨锟絀D 涓嶈兘涓虹┖")
  private String fromWarehouseId;

  /**
   * 鐩爣浠撳簱 ID锛堣浆鍏ュ埌鍝釜浠撳簱锟?
   */
  @NotBlank(message = "鐩爣浠撳簱 ID 涓嶈兘涓虹┖")
  private String toWarehouseId;

  /**
   * 璋冩嫧鏁伴噺
   */
  @NotNull(message = "璋冩嫧鏁伴噺涓嶈兘涓虹┖")
  @Positive(message = "璋冩嫧鏁伴噺蹇呴』澶т簬0")
  private Integer quantity;

  /**
   * 璋冩嫧鍗曞彿
   */
  private String transferNo;

  /**
   * 鎿嶄綔锟絀D
   */
  private String operatorId;

  /**
   * 鎿嶄綔浜哄锟?
   */
  private String operatorName;

  /**
   * 澶囨敞
   */
  private String remark;
}
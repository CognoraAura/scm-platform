package com.scmcloud.inventory.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 搴撳瓨璋冩暣璇锋眰
 *
 * <p>鐢ㄤ簬鍚勭搴撳瓨鎿嶄綔锛氬叆搴撱€佸嚭搴撱€佺洏鐐硅皟鏁寸瓑
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Data
public class InventoryAdjustRequest {

  /**
   * SKU ID
   */
  @NotBlank(message = "SKU ID 涓嶈兘涓虹┖")
  private String skuId;

  /**
   * 浠撳簱 ID
   */
  @NotBlank(message = "浠撳簱 ID 涓嶈兘涓虹┖")
  private String warehouseId;

  /**
   * 璋冩暣鏁伴噺锛堟鏁颁负澧炲姞锛岃礋鏁颁负鍑忓皯锟?
   */
  @NotNull(message = "璋冩暣鏁伴噺涓嶈兘涓虹┖")
  private Integer quantity;

  /**
   * 璋冩暣绫诲瀷锟?鍏ュ簱, 2-鍑哄簱, 6-鐩樼偣璋冩暣, 7-璋冩嫧锟?
   */
  @NotNull(message = "璋冩暣绫诲瀷涓嶈兘涓虹┖")
  private Integer adjustType;

  /**
   * 鍏宠仈涓氬姟鍗曞彿锛堥噰璐崟鍙枫€侀攢鍞崟鍙风瓑锟?
   */
  private String referenceNo;

  /**
   * 鍏宠仈涓氬姟 ID
   */
  private String referenceId;

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
package com.scmcloud.inventory.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 库存调拨请求
 *
 * <p>用于仓库间的库存转移操作
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Data
public class InventoryTransferRequest {

  /**
   * SKU ID
   */
  @NotBlank(message = "SKU ID 不能为空")
  private String skuId;

  /**
   * 源仓�ID（从哪个仓库转出�
   */
  @NotBlank(message = "源仓�ID 不能为空")
  private String fromWarehouseId;

  /**
   * 目标仓库 ID（转入到哪个仓库�
   */
  @NotBlank(message = "目标仓库 ID 不能为空")
  private String toWarehouseId;

  /**
   * 调拨数量
   */
  @NotNull(message = "调拨数量不能为空")
  @Positive(message = "调拨数量必须大于0")
  private Integer quantity;

  /**
   * 调拨单号
   */
  private String transferNo;

  /**
   * 操作�ID
   */
  private String operatorId;

  /**
   * 操作人姓�
   */
  private String operatorName;

  /**
   * 备注
   */
  private String remark;
}
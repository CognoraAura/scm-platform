package com.scmcloud.inventory.domain.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存响应对象
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Data
public class InventoryResponse {

  /**
   * 库存 ID
   */
  private String id;

  /**
   * SKU ID
   */
  private String skuId;

  /**
   * 仓库 ID
   */
  private String warehouseId;

  /**
   * 总库�?
   */
  private Integer totalStock;

  /**
   * 可用库存
   */
  private Integer availableStock;

  /**
   * 锁定库存（已预占�?
   */
  private Integer lockedStock;

  /**
   * 损坏库存
   */
  private Integer damagedStock;

  /**
   * 安全库存
   */
  private Integer safetyStock;

  /**
   * 最大库�?
   */
  private Integer maxStock;

  /**
   * 库位编码
   */
  private String locationCode;

  /**
   * 平均成本
   */
  private BigDecimal averageCost;

  /**
   * 库存状态（NORMAL-正常, LOW_STOCK-低库�? OUT_OF_STOCK-缺货�?
   */
  private String stockStatus;

  /**
   * 乐观锁版本号
   */
  private Integer version;

  /**
   * 最近入库时�?
   */
  private LocalDateTime lastInboundAt;

  /**
   * 最近出库时�?
   */
  private LocalDateTime lastOutboundAt;

  /**
   * 创建时间
   */
  private LocalDateTime createTime;

  /**
   * 更新时间
   */
  private LocalDateTime updateTime;

  /**
   * 备注
   */
  private String remark;
}
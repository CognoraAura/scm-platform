package com.scmcloud.inventory.domain.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 库存统计响应
 *
 * <p>提供全局库存统计信息
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Data
public class InventoryStatsResponse {

  /**
   * SKU 总数
   */
  private Long totalSkuCount;

  /**
   * 仓库总数
   */
  private Long totalWarehouseCount;

  /**
   * 总库存数�?
   */
  private Long totalStockQuantity;

  /**
   * 可用库存数量
   */
  private Long availableStockQuantity;

  /**
   * 锁定库存数量
   */
  private Long lockedStockQuantity;

  /**
   * 损坏库存数量
   */
  private Long damagedStockQuantity;

  /**
   * 库存总价�?
   */
  private BigDecimal totalStockValue;

  /**
   * 缺货 SKU 数量
   */
  private Long outOfStockCount;

  /**
   * 低库�?SKU 数量
   */
  private Long lowStockCount;

  /**
   * 正常库存 SKU 数量
   */
  private Long normalStockCount;
}
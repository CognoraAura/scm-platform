package com.scmcloud.inventory.domain.dto;

import lombok.Data;

import java.util.List;

/**
 * 库存查询请求
 *
 * <p>支持多种查询条件组合�?
 * <ul>
 *   <li>SKU 列表查询</li>
 *   <li>仓库列表查询</li>
 *   <li>库位编码查询</li>
 *   <li>库存状态过滤（缺货/低库�?正常�?/li>
 *   <li>可用库存范围查询</li>
 * </ul>
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Data
public class InventoryQueryRequest {

  /**
   * SKU ID 列表（支持批量查询）
   */
  private List<String> skuIds;

  /**
   * 仓库 ID 列表
   */
  private List<String> warehouseIds;

  /**
   * 库位编码
   */
  private String locationCode;

  /**
   * 库存状态过滤（NORMAL-正常, LOW_STOCK-低库�? OUT_OF_STOCK-缺货�?
   */
  private String stockStatus;

  /**
   * 最小可用库存（大于等于�?
   */
  private Integer minAvailableStock;

  /**
   * 最大可用库存（小于等于�?
   */
  private Integer maxAvailableStock;

  /**
   * 是否仅查询有库存商品
   */
  private Boolean onlyInStock;

  /**
   * 页码（从1开始）
   */
  private Integer page = 1;

  /**
   * 每页条数
   */
  private Integer size = 20;

  /**
   * 排序字段（available_stock, total_stock, update_time�?
   */
  private String sortBy = "update_time";

  /**
   * 排序方向（ASC, DESC�?
   */
  private String sortOrder = "DESC";
}
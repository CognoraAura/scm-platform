package com.frog.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frog.inventory.domain.dto.InventoryAdjustRequest;
import com.frog.inventory.domain.dto.InventoryQueryRequest;
import com.frog.inventory.domain.dto.InventoryResponse;
import com.frog.inventory.domain.dto.InventoryStatsResponse;
import com.frog.inventory.domain.dto.InventoryTransferRequest;
import com.frog.inventory.service.IInvInventoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 库存管理控制�?
 *
 * <p>提供库存查询、调整、转移等REST API接口
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/inventory")
public class InvInventoryController {

  @Autowired
  private IInvInventoryService inventoryService;

  /**
   * 查询单个SKU在指定仓库的库存
   */
  @GetMapping
  public InventoryResponse getInventory(
      @RequestParam @NotBlank(message = "SKU ID 不能为空") String skuId,
      @RequestParam @NotBlank(message = "仓库 ID 不能为空") String warehouseId) {

    log.info("📦 [API] 查询库存: skuId={}, warehouseId={}", skuId, warehouseId);

    InventoryResponse response = inventoryService.getInventory(skuId, warehouseId);

    if (response == null) {
      log.warn("⚠️  [API] 库存不存�? skuId={}, warehouseId={}", skuId, warehouseId);
    } else {
      log.info("�?[API] 查询库存成功: skuId={}, availableStock={}",
          skuId, response.getAvailableStock());
    }

    return response;
  }

  /**
   * 批量查询库存
   */
  @PostMapping("/batch")
  public List<InventoryResponse> batchGetInventory(
      @RequestBody List<String> skuIds,
      @RequestParam(required = false) String warehouseId) {

    log.info("📦 [API] 批量查询库存: skuIds.size={}, warehouseId={}", skuIds.size(), warehouseId);

    List<InventoryResponse> responses = inventoryService.batchGetInventory(skuIds, warehouseId);

    log.info("�?[API] 批量查询成功: 返回{}条库存记�?, responses.size());

    return responses;
  }

  /**
   * 分页查询库存（支持多种过滤条件）
   */
  @PostMapping("/query")
  public Page<InventoryResponse> queryInventory(
      @RequestBody @Valid InventoryQueryRequest request) {

    log.info("📦 [API] 分页查询库存: page={}, size={}, stockStatus={}",
        request.getPage(), request.getSize(), request.getStockStatus());

    Page<InventoryResponse> page = inventoryService.queryInventory(request);

    log.info("�?[API] 分页查询成功: total={}, current={}",
        page.getTotal(), page.getCurrent());

    return page;
  }

  /**
   * 调整库存（入库、出库、盘点调整等�?
   */
  @PostMapping("/adjust")
  public InventoryResponse adjustInventory(
      @RequestBody @Valid InventoryAdjustRequest request) {

    log.info("📝 [API] 调整库存: skuId={}, warehouseId={}, quantity={}, adjustType={}",
        request.getSkuId(), request.getWarehouseId(),
        request.getQuantity(), request.getAdjustType());

    try {
      InventoryResponse response = inventoryService.adjustInventory(request);

      log.info("�?[API] 库存调整成功: skuId={}, availableStock={}",
          request.getSkuId(), response.getAvailableStock());

      return response;

    } catch (IllegalArgumentException e) {
      log.error("�?[API] 库存调整失败: {}", e.getMessage());
      throw e;
    } catch (Exception e) {
      log.error("�?[API] 库存调整异常: skuId={}, error={}",
          request.getSkuId(), e.getMessage(), e);
      throw new RuntimeException("库存调整失败: " + e.getMessage(), e);
    }
  }

  /**
   * 库存调拨（从一个仓库转移到另一个仓库）
   */
  @PostMapping("/transfer")
  public boolean transferInventory(
      @RequestBody @Valid InventoryTransferRequest request) {

    log.info("🔄 [API] 库存调拨: skuId={}, from={}, to={}, quantity={}",
        request.getSkuId(), request.getFromWarehouseId(),
        request.getToWarehouseId(), request.getQuantity());

    try {
      boolean success = inventoryService.transferInventory(request);

      if (success) {
        log.info("�?[API] 库存调拨成功: skuId={}, transferNo={}",
            request.getSkuId(), request.getTransferNo());
      } else {
        log.warn("⚠️  [API] 库存调拨失败: skuId={}", request.getSkuId());
      }

      return success;

    } catch (IllegalArgumentException e) {
      log.error("�?[API] 库存调拨失败: {}", e.getMessage());
      throw e;
    } catch (Exception e) {
      log.error("�?[API] 库存调拨异常: skuId={}, error={}",
          request.getSkuId(), e.getMessage(), e);
      throw new RuntimeException("库存调拨失败: " + e.getMessage(), e);
    }
  }

  /**
   * 检查库存是否充�?
   */
  @GetMapping("/check")
  public boolean checkStockAvailable(
      @RequestParam @NotBlank(message = "SKU ID 不能为空") String skuId,
      @RequestParam @NotBlank(message = "仓库 ID 不能为空") String warehouseId,
      @RequestParam @Positive(message = "数量必须大于0") Integer quantity) {

    log.debug("📦 [API] 检查库�? skuId={}, warehouseId={}, quantity={}",
        skuId, warehouseId, quantity);

    boolean available = inventoryService.checkStockAvailable(skuId, warehouseId, quantity);

    if (available) {
      log.debug("�?[API] 库存充足: skuId={}, quantity={}", skuId, quantity);
    } else {
      log.warn("⚠️  [API] 库存不足: skuId={}, quantity={}", skuId, quantity);
    }

    return available;
  }

  /**
   * 获取库存统计信息
   */
  @GetMapping("/stats")
  public InventoryStatsResponse getInventoryStats() {
    log.info("📊 [API] 获取库存统计");

    InventoryStatsResponse stats = inventoryService.getInventoryStats();

    log.info("�?[API] 库存统计成功: totalSku={}, totalStock={}, outOfStock={}",
        stats.getTotalSkuCount(), stats.getTotalStockQuantity(), stats.getOutOfStockCount());

    return stats;
  }

  /**
   * 初始化库�?
   */
  @PostMapping("/init")
  public InventoryResponse initInventory(
      @RequestParam @NotBlank(message = "SKU ID 不能为空") String skuId,
      @RequestParam @NotBlank(message = "仓库 ID 不能为空") String warehouseId,
      @RequestParam(required = false) Integer initialStock) {

    log.info("🆕 [API] 初始化库�? skuId={}, warehouseId={}, initialStock={}",
        skuId, warehouseId, initialStock);

    try {
      InventoryResponse response = inventoryService.initInventory(skuId, warehouseId, initialStock);

      log.info("�?[API] 库存初始化成�? id={}, skuId={}", response.getId(), skuId);

      return response;

    } catch (Exception e) {
      log.error("�?[API] 库存初始化失�? skuId={}, error={}", skuId, e.getMessage(), e);
      throw new RuntimeException("库存初始化失�? " + e.getMessage(), e);
    }
  }
}
package com.scmcloud.warehouse.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.warehouse.domain.entity.WmsWarehouse;
import com.scmcloud.warehouse.service.command.WmsWarehouseCommandService;
import com.scmcloud.warehouse.service.query.WmsWarehouseQueryService;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/wms-warehouse")
public class WmsWarehouseController {

    private final WmsWarehouseCommandService warehouseCommandService;
    private final WmsWarehouseQueryService warehouseQueryService;

    @PostMapping
    public ApiResponse<WmsWarehouse> create(@RequestBody WmsWarehouse warehouse) {
        log.info("[API] 创建仓库: code={}, name={}", warehouse.getWarehouseCode(), warehouse.getWarehouseName());
        try {
            WmsWarehouse created = warehouseCommandService.create(warehouse);
            log.info("[API] 仓库创建成功: id={}", created.getId());
            return ApiResponse.success(created);
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<WmsWarehouse> update(@PathVariable String id, @RequestBody WmsWarehouse warehouse) {
        log.info("[API] 更新仓库: id={}", id);
        warehouse.setId(id);
        boolean success = warehouseCommandService.update(warehouse);
        if (!success) {
            return ApiResponse.fail(404, "仓库不存在");
        }
        return ApiResponse.success(warehouseQueryService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("[API] 删除仓库: id={}", id);
        boolean success = warehouseCommandService.softDeleteById(id);
        return success ? ApiResponse.success() : ApiResponse.fail(404, "仓库不存在");
    }

    @GetMapping("/{id}")
    public ApiResponse<WmsWarehouse> getById(@PathVariable String id) {
        WmsWarehouse warehouse = warehouseQueryService.getById(id);
        if (warehouse == null || Boolean.TRUE.equals(warehouse.getDeleted())) {
            return ApiResponse.fail(404, "仓库不存在");
        }
        return ApiResponse.success(warehouse);
    }

    @GetMapping("/list")
    public ApiResponse<List<WmsWarehouse>> listEnabled() {
        return ApiResponse.success(warehouseQueryService.listEnabled());
    }

    @GetMapping("/page")
    public ApiResponse<Page<WmsWarehouse>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String warehouseName,
            @RequestParam(required = false) Integer warehouseType,
            @RequestParam(required = false) Boolean enabled) {
        return ApiResponse.success(warehouseQueryService.pageList(page, size, warehouseName, warehouseType, enabled));
    }

    @PutMapping("/{id}/enable")
    public ApiResponse<Void> enable(@PathVariable String id) {
        log.info("[API] 启用仓库: id={}", id);
        boolean success = warehouseCommandService.enable(id);
        return success ? ApiResponse.success() : ApiResponse.fail(400, "启用失败");
    }

    @PutMapping("/{id}/disable")
    public ApiResponse<Void> disable(@PathVariable String id) {
        log.info("[API] 停用仓库: id={}", id);
        boolean success = warehouseCommandService.disable(id);
        return success ? ApiResponse.success() : ApiResponse.fail(400, "停用失败");
    }
}

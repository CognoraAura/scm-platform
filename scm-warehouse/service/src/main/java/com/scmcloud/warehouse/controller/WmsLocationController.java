package com.scmcloud.warehouse.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.warehouse.domain.entity.WmsLocation;
import com.scmcloud.warehouse.service.command.WmsLocationCommandService;
import com.scmcloud.warehouse.service.query.WmsLocationQueryService;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/wms-location")
public class WmsLocationController {

    private final WmsLocationCommandService locationCommandService;
    private final WmsLocationQueryService locationQueryService;

    @PostMapping
    public ApiResponse<WmsLocation> create(@RequestBody WmsLocation location) {
        log.info("[API] 创建库位: warehouseId={}, code={}", location.getWarehouseId(), location.getLocationCode());
        try {
            WmsLocation created = locationCommandService.create(location);
            log.info("[API] 库位创建成功: id={}", created.getId());
            return ApiResponse.success(created);
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<WmsLocation> update(@PathVariable String id, @RequestBody WmsLocation location) {
        log.info("[API] 更新库位: id={}", id);
        location.setId(id);
        boolean success = locationCommandService.update(location);
        if (!success) {
            return ApiResponse.fail(404, "库位不存在");
        }
        return ApiResponse.success(locationQueryService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("[API] 删除库位: id={}", id);
        boolean success = locationCommandService.softDeleteById(id);
        return success ? ApiResponse.success() : ApiResponse.fail(404, "库位不存在");
    }

    @GetMapping("/{id}")
    public ApiResponse<WmsLocation> getById(@PathVariable String id) {
        WmsLocation location = locationQueryService.getById(id);
        if (location == null || Boolean.TRUE.equals(location.getDeleted())) {
            return ApiResponse.fail(404, "库位不存在");
        }
        return ApiResponse.success(location);
    }

    @GetMapping("/list")
    public ApiResponse<List<WmsLocation>> listByWarehouseId(
            @RequestParam String warehouseId) {
        return ApiResponse.success(locationQueryService.listByWarehouseId(warehouseId));
    }

    @GetMapping("/page")
    public ApiResponse<Page<WmsLocation>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) Integer locationType,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.success(locationQueryService.pageList(page, size, warehouseId, locationType, status));
    }
}

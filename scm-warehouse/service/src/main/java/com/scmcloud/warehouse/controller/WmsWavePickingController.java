package com.scmcloud.warehouse.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.warehouse.domain.entity.WmsWavePicking;
import com.scmcloud.warehouse.service.command.WmsWavePickingCommandService;
import com.scmcloud.warehouse.service.query.WmsWavePickingQueryService;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/wms-wave-picking")
public class WmsWavePickingController {

    private final WmsWavePickingCommandService wavePickingCommandService;
    private final WmsWavePickingQueryService wavePickingQueryService;

    @PostMapping
    public ApiResponse<WmsWavePicking> create(@RequestBody WmsWavePicking wave) {
        log.info("[API] 创建波次拣货单 warehouseId={}, orderCount={}", wave.getWarehouseId(), wave.getOrderCount());
        WmsWavePicking created = wavePickingCommandService.create(wave);
        log.info("[API] 波次拣货单创建成功 id={}, waveNo={}", created.getId(), created.getWaveNo());
        return ApiResponse.success(created);
    }

    @PutMapping("/{id}")
    public ApiResponse<WmsWavePicking> update(@PathVariable String id, @RequestBody WmsWavePicking wave) {
        log.info("[API] 更新波次拣货单 id={}", id);
        wave.setId(id);
        try {
            boolean success = wavePickingCommandService.update(wave);
            if (!success) {
                return ApiResponse.fail(404, "波次拣货单不存在");
            }
            return ApiResponse.success(wavePickingQueryService.getById(id));
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("[API] 删除波次拣货单 id={}", id);
        boolean success = wavePickingCommandService.removeById(id);
        return success ? ApiResponse.success() : ApiResponse.fail(404, "波次拣货单不存在");
    }

    @GetMapping("/{id}")
    public ApiResponse<WmsWavePicking> getById(@PathVariable String id) {
        WmsWavePicking wave = wavePickingQueryService.getById(id);
        if (wave == null) {
            return ApiResponse.fail(404, "波次拣货单不存在");
        }
        return ApiResponse.success(wave);
    }

    @GetMapping("/page")
    public ApiResponse<Page<WmsWavePicking>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.success(wavePickingQueryService.pageList(page, size, warehouseId, status));
    }

    @PutMapping("/{id}/start")
    public ApiResponse<Void> start(
            @PathVariable String id,
            @RequestParam String pickerId,
            @RequestParam String pickerName) {
        log.info("[API] 开始拣货 id={}, picker={}", id, pickerName);
        try {
            boolean success = wavePickingCommandService.start(id, pickerId, pickerName);
            return success ? ApiResponse.success() : ApiResponse.fail(400, "开始拣货失败");
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @PutMapping("/{id}/complete")
    public ApiResponse<Void> complete(
            @PathVariable String id,
            @RequestParam String operatorId) {
        log.info("[API] 完成拣货: id={}", id);
        try {
            boolean success = wavePickingCommandService.complete(id, operatorId);
            return success ? ApiResponse.success() : ApiResponse.fail(400, "完成拣货失败");
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(
            @PathVariable String id,
            @RequestParam String operatorId) {
        log.info("[API] 取消波次拣货单 id={}", id);
        try {
            boolean success = wavePickingCommandService.cancel(id, operatorId);
            return success ? ApiResponse.success() : ApiResponse.fail(400, "取消失败");
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }
}

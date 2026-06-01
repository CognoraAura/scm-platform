package com.scmcloud.warehouse.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.common.util.UUIDv7Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.warehouse.domain.entity.WmsWavePicking;
import com.scmcloud.warehouse.service.IWmsWavePickingService;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/wms-wave-picking")
public class WmsWavePickingController {

    private final IWmsWavePickingService wavePickingService;

    @PostMapping
    public ApiResponse<WmsWavePicking> create(@RequestBody WmsWavePicking wave) {
        log.info("[API] 创建波次拣货�? warehouseId={}, orderCount={}", wave.getWarehouseId(), wave.getOrderCount());

        wave.setId(UUIDv7Util.generateString());
        wave.setWaveNo("WAVE" + System.currentTimeMillis());
        wave.setStatus(0); // 0-待拣�?
        wave.setCreateTime(LocalDateTime.now());
        wave.setUpdateTime(LocalDateTime.now());

        wavePickingService.save(wave);
        log.info("[API] 波次拣货单创建成�? id={}, waveNo={}", wave.getId(), wave.getWaveNo());
        return ApiResponse.success(wave);
    }

    @PutMapping("/{id}")
    public ApiResponse<WmsWavePicking> update(@PathVariable String id, @RequestBody WmsWavePicking wave) {
        log.info("[API] 更新波次拣货�? id={}", id);

        WmsWavePicking existing = wavePickingService.getById(id);
        if (existing == null) {
            return ApiResponse.fail(404, "波次拣货单不存在");
        }
        if (existing.getStatus() != 0) {
            return ApiResponse.fail(400, "只有待拣货状态的波次拣货单才能修�?);
        }

        wave.setId(id);
        wave.setUpdateTime(LocalDateTime.now());
        wavePickingService.updateById(wave);
        return ApiResponse.success(wavePickingService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("[API] 删除波次拣货�? id={}", id);

        WmsWavePicking existing = wavePickingService.getById(id);
        if (existing == null) {
            return ApiResponse.fail(404, "波次拣货单不存在");
        }

        wavePickingService.removeById(id);
        return ApiResponse.success();
    }

    @GetMapping("/{id}")
    public ApiResponse<WmsWavePicking> getById(@PathVariable String id) {
        WmsWavePicking wave = wavePickingService.getById(id);
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
        return ApiResponse.success(wavePickingService.pageList(page, size, warehouseId, status));
    }

    @PutMapping("/{id}/start")
    public ApiResponse<Void> start(
            @PathVariable String id,
            @RequestParam String pickerId,
            @RequestParam String pickerName) {
        log.info("[API] 开始拣�? id={}, picker={}", id, pickerName);

        try {
            boolean success = wavePickingService.start(id, pickerId, pickerName);
            return success ? ApiResponse.success() : ApiResponse.fail(400, "开始拣货失�?);
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
            boolean success = wavePickingService.complete(id, operatorId);
            return success ? ApiResponse.success() : ApiResponse.fail(400, "完成拣货失败");
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(
            @PathVariable String id,
            @RequestParam String operatorId) {
        log.info("[API] 取消波次拣货�? id={}", id);

        try {
            boolean success = wavePickingService.cancel(id, operatorId);
            return success ? ApiResponse.success() : ApiResponse.fail(400, "取消失败");
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }
}

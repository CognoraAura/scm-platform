package com.scmcloud.warehouse.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.common.util.UUIDv7Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.warehouse.domain.entity.WmsOutbound;
import com.scmcloud.warehouse.service.IWmsOutboundService;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/wms-outbound")
public class WmsOutboundController {

    private final IWmsOutboundService outboundService;

    @PostMapping
    public ApiResponse<WmsOutbound> create(@RequestBody WmsOutbound outbound) {
        log.info("[API] 创建出库�? warehouseId={}, type={}", outbound.getWarehouseId(), outbound.getOutboundType());

        outbound.setId(UUIDv7Util.generateString());
        outbound.setOutboundNo("OUT" + System.currentTimeMillis());
        outbound.setStatus(0); // 0-待拣�?
        outbound.setPickedQuantity(0);
        outbound.setDeleted(false);
        outbound.setCreateTime(LocalDateTime.now());
        outbound.setUpdateTime(LocalDateTime.now());

        outboundService.save(outbound);
        log.info("[API] 出库单创建成�? id={}, outboundNo={}", outbound.getId(), outbound.getOutboundNo());
        return ApiResponse.success(outbound);
    }

    @PutMapping("/{id}")
    public ApiResponse<WmsOutbound> update(@PathVariable String id, @RequestBody WmsOutbound outbound) {
        log.info("[API] 更新出库�? id={}", id);

        WmsOutbound existing = outboundService.getById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            return ApiResponse.fail(404, "出库单不存在");
        }
        if (existing.getStatus() != 0) {
            return ApiResponse.fail(400, "只有待拣货状态的出库单才能修�?);
        }

        outbound.setId(id);
        outbound.setUpdateTime(LocalDateTime.now());
        outboundService.updateById(outbound);
        return ApiResponse.success(outboundService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("[API] 删除出库�? id={}", id);

        WmsOutbound existing = outboundService.getById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            return ApiResponse.fail(404, "出库单不存在");
        }

        existing.setDeleted(true);
        existing.setUpdateTime(LocalDateTime.now());
        outboundService.updateById(existing);
        return ApiResponse.success();
    }

    @GetMapping("/{id}")
    public ApiResponse<WmsOutbound> getById(@PathVariable String id) {
        WmsOutbound outbound = outboundService.getById(id);
        if (outbound == null || Boolean.TRUE.equals(outbound.getDeleted())) {
            return ApiResponse.fail(404, "出库单不存在");
        }
        return ApiResponse.success(outbound);
    }

    @GetMapping("/page")
    public ApiResponse<Page<WmsOutbound>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) Integer outboundType,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.success(outboundService.pageList(page, size, warehouseId, outboundType, status));
    }

    @PutMapping("/{id}/ship")
    public ApiResponse<Void> ship(
            @PathVariable String id,
            @RequestParam String operatorId,
            @RequestParam String operatorName) {
        log.info("[API] 出库确认: id={}, operator={}", id, operatorName);

        try {
            boolean success = outboundService.ship(id, operatorId, operatorName);
            return success ? ApiResponse.success() : ApiResponse.fail(400, "出库失败");
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(
            @PathVariable String id,
            @RequestParam String operatorId,
            @RequestParam String operatorName) {
        log.info("[API] 取消出库�? id={}, operator={}", id, operatorName);

        try {
            boolean success = outboundService.cancel(id, operatorId, operatorName);
            return success ? ApiResponse.success() : ApiResponse.fail(400, "取消失败");
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }
}

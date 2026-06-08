package com.scmcloud.warehouse.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.warehouse.domain.entity.WmsInbound;
import com.scmcloud.warehouse.service.command.WmsInboundCommandService;
import com.scmcloud.warehouse.service.query.WmsInboundQueryService;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/wms-inbound")
public class WmsInboundController {

    private final WmsInboundCommandService inboundCommandService;
    private final WmsInboundQueryService inboundQueryService;

    @PostMapping
    public ApiResponse<WmsInbound> create(@RequestBody WmsInbound inbound) {
        log.info("[API] 创建入库单 warehouseId={}, type={}", inbound.getWarehouseId(), inbound.getInboundType());
        WmsInbound created = inboundCommandService.create(inbound);
        log.info("[API] 入库单创建成功 id={}, inboundNo={}", created.getId(), created.getInboundNo());
        return ApiResponse.success(created);
    }

    @PutMapping("/{id}")
    public ApiResponse<WmsInbound> update(@PathVariable String id, @RequestBody WmsInbound inbound) {
        log.info("[API] 更新入库单 id={}", id);
        inbound.setId(id);
        try {
            boolean success = inboundCommandService.update(inbound);
            if (!success) {
                return ApiResponse.fail(404, "入库单不存在");
            }
            return ApiResponse.success(inboundQueryService.getById(id));
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("[API] 删除入库单 id={}", id);
        boolean success = inboundCommandService.softDeleteById(id);
        return success ? ApiResponse.success() : ApiResponse.fail(404, "入库单不存在");
    }

    @GetMapping("/{id}")
    public ApiResponse<WmsInbound> getById(@PathVariable String id) {
        WmsInbound inbound = inboundQueryService.getById(id);
        if (inbound == null || Boolean.TRUE.equals(inbound.getDeleted())) {
            return ApiResponse.fail(404, "入库单不存在");
        }
        return ApiResponse.success(inbound);
    }

    @GetMapping("/page")
    public ApiResponse<Page<WmsInbound>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) Integer inboundType,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.success(inboundQueryService.pageList(page, size, warehouseId, inboundType, status));
    }

    @PutMapping("/{id}/receive")
    public ApiResponse<Void> receive(
            @PathVariable String id,
            @RequestParam String operatorId,
            @RequestParam String operatorName) {
        log.info("[API] 入库收货: id={}, operator={}", id, operatorName);
        try {
            boolean success = inboundCommandService.receive(id, operatorId, operatorName);
            return success ? ApiResponse.success() : ApiResponse.fail(400, "收货失败");
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(
            @PathVariable String id,
            @RequestParam String operatorId,
            @RequestParam String operatorName) {
        log.info("[API] 取消入库单 id={}, operator={}", id, operatorName);
        try {
            boolean success = inboundCommandService.cancel(id, operatorId, operatorName);
            return success ? ApiResponse.success() : ApiResponse.fail(400, "取消失败");
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }
}

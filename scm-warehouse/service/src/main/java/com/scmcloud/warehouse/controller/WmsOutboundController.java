package com.scmcloud.warehouse.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.warehouse.domain.entity.WmsOutbound;
import com.scmcloud.warehouse.service.command.WmsOutboundCommandService;
import com.scmcloud.warehouse.service.query.WmsOutboundQueryService;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/wms-outbound")
public class WmsOutboundController {

    private final WmsOutboundCommandService outboundCommandService;
    private final WmsOutboundQueryService outboundQueryService;

    @PostMapping
    public ApiResponse<WmsOutbound> create(@RequestBody WmsOutbound outbound) {
        log.info("[API] 创建出库单 warehouseId={}, type={}", outbound.getWarehouseId(), outbound.getOutboundType());
        WmsOutbound created = outboundCommandService.create(outbound);
        log.info("[API] 出库单创建成功 id={}, outboundNo={}", created.getId(), created.getOutboundNo());
        return ApiResponse.success(created);
    }

    @PutMapping("/{id}")
    public ApiResponse<WmsOutbound> update(@PathVariable String id, @RequestBody WmsOutbound outbound) {
        log.info("[API] 更新出库单 id={}", id);
        outbound.setId(id);
        try {
            boolean success = outboundCommandService.update(outbound);
            if (!success) {
                return ApiResponse.fail(404, "出库单不存在");
            }
            return ApiResponse.success(outboundQueryService.getById(id));
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("[API] 删除出库单 id={}", id);
        boolean success = outboundCommandService.softDeleteById(id);
        return success ? ApiResponse.success() : ApiResponse.fail(404, "出库单不存在");
    }

    @GetMapping("/{id}")
    public ApiResponse<WmsOutbound> getById(@PathVariable String id) {
        WmsOutbound outbound = outboundQueryService.getById(id);
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
        return ApiResponse.success(outboundQueryService.pageList(page, size, warehouseId, outboundType, status));
    }

    @PutMapping("/{id}/ship")
    public ApiResponse<Void> ship(
            @PathVariable String id,
            @RequestParam String operatorId,
            @RequestParam String operatorName) {
        log.info("[API] 出库确认: id={}, operator={}", id, operatorName);
        try {
            boolean success = outboundCommandService.ship(id, operatorId, operatorName);
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
        log.info("[API] 取消出库单 id={}, operator={}", id, operatorName);
        try {
            boolean success = outboundCommandService.cancel(id, operatorId, operatorName);
            return success ? ApiResponse.success() : ApiResponse.fail(400, "取消失败");
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }
}

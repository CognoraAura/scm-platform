package com.scmcloud.warehouse.controller;

import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.warehouse.domain.entity.WmsOutboundItem;
import com.scmcloud.warehouse.service.command.WmsOutboundItemCommandService;
import com.scmcloud.warehouse.service.query.WmsOutboundItemQueryService;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/wms-outbound-item")
public class WmsOutboundItemController {

    private final WmsOutboundItemCommandService outboundItemCommandService;
    private final WmsOutboundItemQueryService outboundItemQueryService;

    @PostMapping
    public ApiResponse<WmsOutboundItem> create(@RequestBody WmsOutboundItem item) {
        log.info("[API] 创建出库明细: outboundId={}, skuId={}", item.getOutboundId(), item.getSkuId());
        WmsOutboundItem created = outboundItemCommandService.create(item);
        log.info("[API] 出库明细创建成功: id={}", created.getId());
        return ApiResponse.success(created);
    }

    @PutMapping("/{id}")
    public ApiResponse<WmsOutboundItem> update(@PathVariable String id, @RequestBody WmsOutboundItem item) {
        log.info("[API] 更新出库明细: id={}", id);
        item.setId(id);
        boolean success = outboundItemCommandService.update(item);
        if (!success) {
            return ApiResponse.fail(404, "出库明细不存在");
        }
        return ApiResponse.success(outboundItemQueryService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("[API] 删除出库明细: id={}", id);
        boolean success = outboundItemCommandService.softDeleteById(id);
        return success ? ApiResponse.success() : ApiResponse.fail(404, "出库明细不存在");
    }

    @GetMapping("/{id}")
    public ApiResponse<WmsOutboundItem> getById(@PathVariable String id) {
        WmsOutboundItem item = outboundItemQueryService.getById(id);
        if (item == null || Boolean.TRUE.equals(item.getDeleted())) {
            return ApiResponse.fail(404, "出库明细不存在");
        }
        return ApiResponse.success(item);
    }

    @GetMapping("/list")
    public ApiResponse<List<WmsOutboundItem>> listByOutboundId(
            @RequestParam String outboundId) {
        return ApiResponse.success(outboundItemQueryService.listByOutboundId(outboundId));
    }
}

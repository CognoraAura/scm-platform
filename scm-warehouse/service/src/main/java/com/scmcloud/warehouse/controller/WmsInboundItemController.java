package com.scmcloud.warehouse.controller;

import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.warehouse.domain.entity.WmsInboundItem;
import com.scmcloud.warehouse.service.command.WmsInboundItemCommandService;
import com.scmcloud.warehouse.service.query.WmsInboundItemQueryService;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/wms-inbound-item")
public class WmsInboundItemController {

    private final WmsInboundItemCommandService inboundItemCommandService;
    private final WmsInboundItemQueryService inboundItemQueryService;

    @PostMapping
    public ApiResponse<WmsInboundItem> create(@RequestBody WmsInboundItem item) {
        log.info("[API] 创建入库明细: inboundId={}, skuId={}", item.getInboundId(), item.getSkuId());
        WmsInboundItem created = inboundItemCommandService.create(item);
        log.info("[API] 入库明细创建成功: id={}", created.getId());
        return ApiResponse.success(created);
    }

    @PutMapping("/{id}")
    public ApiResponse<WmsInboundItem> update(@PathVariable String id, @RequestBody WmsInboundItem item) {
        log.info("[API] 更新入库明细: id={}", id);
        item.setId(id);
        boolean success = inboundItemCommandService.update(item);
        if (!success) {
            return ApiResponse.fail(404, "入库明细不存在");
        }
        return ApiResponse.success(inboundItemQueryService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("[API] 删除入库明细: id={}", id);
        boolean success = inboundItemCommandService.softDeleteById(id);
        return success ? ApiResponse.success() : ApiResponse.fail(404, "入库明细不存在");
    }

    @GetMapping("/{id}")
    public ApiResponse<WmsInboundItem> getById(@PathVariable String id) {
        WmsInboundItem item = inboundItemQueryService.getById(id);
        if (item == null || Boolean.TRUE.equals(item.getDeleted())) {
            return ApiResponse.fail(404, "入库明细不存在");
        }
        return ApiResponse.success(item);
    }

    @GetMapping("/list")
    public ApiResponse<List<WmsInboundItem>> listByInboundId(
            @RequestParam String inboundId) {
        return ApiResponse.success(inboundItemQueryService.listByInboundId(inboundId));
    }
}

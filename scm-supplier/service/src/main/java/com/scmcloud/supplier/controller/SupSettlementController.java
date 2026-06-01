package com.scmcloud.supplier.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.supplier.domain.entity.SupSettlement;
import com.scmcloud.supplier.service.ISupSettlementService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/settlements")
public class SupSettlementController {

    private final ISupSettlementService settlementService;

    @GetMapping("/{id}")
    public ApiResponse<SupSettlement> getById(@PathVariable String id) {
        log.info("[API] 查询对账单详�? id={}", id);
        SupSettlement settlement = settlementService.getById(id);
        if (settlement == null || Boolean.TRUE.equals(settlement.getDeleted())) {
            return ApiResponse.fail(404, "对账单不存在");
        }
        return ApiResponse.success(settlement);
    }

    @PostMapping
    public ApiResponse<SupSettlement> create(@RequestBody SupSettlement settlement) {
        log.info("[API] 创建对账�? supplierId={}", settlement.getSupplierId());
        settlement.setId(UUID.randomUUID());
        settlement.setStatus(0);
        settlement.setDeleted(false);
        settlement.setCreateTime(LocalDateTime.now());
        settlement.setUpdateTime(LocalDateTime.now());
        settlementService.save(settlement);
        log.info("[API] 对账单创建成�? id={}", settlement.getId());
        return ApiResponse.success(settlement);
    }

    @PutMapping("/{id}")
    public ApiResponse<SupSettlement> update(@PathVariable String id, @RequestBody SupSettlement settlement) {
        log.info("[API] 更新对账�? id={}", id);
        SupSettlement existing = settlementService.getById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            return ApiResponse.fail(404, "对账单不存在");
        }
        if (existing.getStatus() != 0) {
            return ApiResponse.fail(400, "只有待确认状态的对账单才能修�?);
        }
        settlement.setId(UUID.fromString(id));
        settlement.setUpdateTime(LocalDateTime.now());
        settlementService.updateById(settlement);
        return ApiResponse.success(settlementService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("[API] 删除对账�? id={}", id);
        SupSettlement existing = settlementService.getById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            return ApiResponse.fail(404, "对账单不存在");
        }
        existing.setDeleted(true);
        existing.setUpdateTime(LocalDateTime.now());
        settlementService.updateById(existing);
        return ApiResponse.success();
    }

    @GetMapping
    public ApiResponse<Page<SupSettlement>> pageList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String supplierId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String settlementPeriod) {
        log.info("[API] 分页查询对账�? page={}, size={}, supplierId={}, status={}", page, size, supplierId, status);
        Page<SupSettlement> result = settlementService.pageList(page, size, supplierId, status, settlementPeriod);
        return ApiResponse.success(result);
    }

    @GetMapping("/supplier/{supplierId}")
    public ApiResponse<List<SupSettlement>> listBySupplierId(@PathVariable String supplierId) {
        log.info("[API] 查询供应商对账单列表: supplierId={}", supplierId);
        return ApiResponse.success(settlementService.listBySupplierId(supplierId));
    }

    @PutMapping("/{id}/confirm")
    public ApiResponse<Void> confirm(@PathVariable String id,
                                     @RequestParam String approverId,
                                     @RequestParam String approverName) {
        log.info("[API] 确认对账�? id={}, approverId={}", id, approverId);
        boolean success = settlementService.confirm(id, approverId, approverName);
        return success ? ApiResponse.success() : ApiResponse.fail(400, "确认失败，对账单状态不正确");
    }

    @PutMapping("/{id}/pay")
    public ApiResponse<Void> markAsPaid(@PathVariable String id, @RequestParam String updateBy) {
        log.info("[API] 标记对账单已付款: id={}", id);
        boolean success = settlementService.markAsPaid(id, updateBy);
        return success ? ApiResponse.success() : ApiResponse.fail(400, "标记付款失败，对账单状态不正确");
    }
}

package com.scmcloud.order.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.order.domain.entity.OrdRefund;
import com.scmcloud.order.service.IOrdRefundService;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/refunds")
public class OrdRefundController {

    private final IOrdRefundService refundService;

    @GetMapping("/{id}")
    public OrdRefund getById(@PathVariable String id) {
        log.info("[API] 查询退款记�? id={}", id);
        return refundService.getById(id);
    }

    @GetMapping("/order/{orderId}")
    public List<OrdRefund> listByOrderId(@PathVariable Long orderId) {
        log.info("[API] 查询订单退款记�? orderId={}", orderId);
        return refundService.listByOrderId(orderId);
    }

    @PostMapping
    public OrdRefund createRefund(@RequestBody OrdRefund refund) {
        log.info("[API] 创建退款记�? orderNo={}, refundAmount={}", refund.getOrderNo(), refund.getRefundAmount());
        return refundService.createRefund(refund);
    }

    @PutMapping
    public boolean update(@RequestBody OrdRefund refund) {
        log.info("[API] 更新退款记�? id={}", refund.getId());
        return refundService.updateById(refund);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        log.info("[API] 删除退款记�? id={}", id);
        return refundService.removeById(id);
    }
}

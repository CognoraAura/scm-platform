package com.scmcloud.order.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.order.domain.entity.OrdPayment;
import com.scmcloud.order.service.IOrdPaymentService;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
public class OrdPaymentController {

    private final IOrdPaymentService paymentService;

    @GetMapping("/{id}")
    public OrdPayment getById(@PathVariable String id) {
        log.info("[API] 查询支付记录: id={}", id);
        return paymentService.getById(id);
    }

    @GetMapping
    public List<OrdPayment> list() {
        log.info("[API] 查询所有支付记�?);
        return paymentService.list();
    }

    @PostMapping
    public OrdPayment createPayment(@RequestBody OrdPayment payment) {
        log.info("[API] 创建支付记录: orderNo={}, amount={}", payment.getOrderNo(), payment.getPaymentAmount());
        return paymentService.createPayment(payment);
    }

    @PutMapping("/{id}/status")
    public boolean updateStatus(
            @PathVariable Long id,
@RequestParam Integer status) {
        log.info("[API] 更新支付状�? id={}, status={}", id, status);
        return paymentService.updatePaymentStatus(id, status);
    }

    @PutMapping
    public boolean update(@RequestBody OrdPayment payment) {
        log.info("[API] 更新支付记录: id={}", payment.getId());
        return paymentService.updateById(payment);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        log.info("[API] 删除支付记录: id={}", id);
        return paymentService.removeById(id);
    }
}

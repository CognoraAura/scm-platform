package com.scmcloud.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.scmcloud.order.domain.entity.OrdPayment;
import com.scmcloud.order.mapper.OrdPaymentMapper;
import com.scmcloud.order.service.IOrdPaymentService;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class OrdPaymentServiceImpl extends ServiceImpl<OrdPaymentMapper, OrdPayment> implements IOrdPaymentService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrdPayment createPayment(OrdPayment payment) {
        log.info("创建支付记录: orderNo={}, amount={}", payment.getOrderNo(), payment.getPaymentAmount());

        payment.setId(UUID.randomUUID().toString());
        payment.setStatus(0);
        payment.setInitiatedAt(LocalDateTime.now());
        payment.setCreateTime(LocalDateTime.now());
        payment.setUpdateTime(LocalDateTime.now());

        boolean saved = save(payment);
        if (!saved) {
            throw new RuntimeException("创建支付记录失败");
        }

        log.info("支付记录创建成功: id={}, paymentNo={}", payment.getId(), payment.getPaymentNo());
        return payment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePaymentStatus(Long paymentId, Integer status) {
        log.info("更新支付状�? paymentId={}, status={}", paymentId, status);

        OrdPayment payment = getById(paymentId);
        if (payment == null) {
            log.warn("支付记录不存�? paymentId={}", paymentId);
            return false;
        }

        payment.setStatus(status);
        payment.setUpdateTime(LocalDateTime.now());

        if (status == 2) {
            payment.setPaidAt(LocalDateTime.now());
        } else if (status == 3) {
            payment.setFailedAt(LocalDateTime.now());
        } else if (status == 5) {
            payment.setRefundedAt(LocalDateTime.now());
        }

        return updateById(payment);
    }
}

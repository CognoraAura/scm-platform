package com.scmcloud.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.scmcloud.order.domain.entity.OrdPayment;
import com.scmcloud.order.mapper.OrdPaymentMapper;
import com.scmcloud.order.service.IOrdPaymentService;
import com.scmcloud.common.status.StatusValidator;

import java.time.LocalDateTime;

@Slf4j
@Service
public class OrdPaymentServiceImpl extends ServiceImpl<OrdPaymentMapper, OrdPayment> implements IOrdPaymentService {

    @Autowired
    private StatusValidator statusValidator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrdPayment createPayment(OrdPayment payment) {
        log.info("创建支付记录: orderNo={}, amount={}", payment.getOrderNo(), payment.getPaymentAmount());

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
        log.info("更新支付状态 paymentId={}, status={}", paymentId, status);

        OrdPayment payment = getById(paymentId);
        if (payment == null) {
            log.warn("支付记录不存在 paymentId={}", paymentId);
            return false;
        }

        String fromStatus = paymentStatusName(payment.getStatus());
        String toStatus = paymentStatusName(status);
        statusValidator.validateTransition("PAYMENT", fromStatus, toStatus);

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

    private String paymentStatusName(Integer status) {
        return switch (status) {
            case 0 -> "PENDING";
            case 1 -> "PROCESSING";
            case 2 -> "SUCCESS";
            case 3 -> "FAILED";
            case 4 -> "REFUNDING";
            case 5 -> "REFUNDED";
            case 6 -> "CANCELLED";
            default -> String.valueOf(status);
        };
    }
}

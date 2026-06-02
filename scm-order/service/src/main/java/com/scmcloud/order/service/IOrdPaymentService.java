package com.scmcloud.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scmcloud.order.domain.entity.OrdPayment;

/**
 * <p>
 * 支付记录�服务�
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
public interface IOrdPaymentService extends IService<OrdPayment> {

    OrdPayment createPayment(OrdPayment payment);

    boolean updatePaymentStatus(Long paymentId, Integer status);
}

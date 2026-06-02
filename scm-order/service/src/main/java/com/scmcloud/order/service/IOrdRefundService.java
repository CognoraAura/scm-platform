package com.scmcloud.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scmcloud.order.domain.entity.OrdRefund;

import java.util.List;

/**
 * <p>
 * 退�退货表 服务�
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
public interface IOrdRefundService extends IService<OrdRefund> {

    OrdRefund createRefund(OrdRefund refund);

    List<OrdRefund> listByOrderId(Long orderId);
}

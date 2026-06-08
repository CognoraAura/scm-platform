package com.scmcloud.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scmcloud.order.domain.entity.OrdRefund;

import java.util.List;

/**
 * <p>
 * 閫€锟介€€璐ц〃 鏈嶅姟锟?
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
public interface IOrdRefundService extends IService<OrdRefund> {

    OrdRefund createRefund(OrdRefund refund);

    List<OrdRefund> listByOrderId(Long orderId);
}

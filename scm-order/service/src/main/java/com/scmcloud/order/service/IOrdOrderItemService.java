package com.scmcloud.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scmcloud.order.domain.entity.OrdOrderItem;

import java.util.List;

/**
 * <p>
 * 订单明细�?服务�?
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
public interface IOrdOrderItemService extends IService<OrdOrderItem> {

    List<OrdOrderItem> listByOrderId(Long orderId);
}

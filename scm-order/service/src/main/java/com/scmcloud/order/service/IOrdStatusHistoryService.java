package com.scmcloud.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scmcloud.order.domain.entity.OrdStatusHistory;

import java.util.List;

/**
 * <p>
 * 订单状态流转历�服务�
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
public interface IOrdStatusHistoryService extends IService<OrdStatusHistory> {

    List<OrdStatusHistory> listByOrderId(Long orderId);
}

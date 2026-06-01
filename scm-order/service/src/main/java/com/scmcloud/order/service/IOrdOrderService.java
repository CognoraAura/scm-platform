package com.scmcloud.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.scmcloud.order.domain.entity.OrdOrder;
import com.scmcloud.order.domain.entity.OrdOrderItem;

import java.util.List;

public interface IOrdOrderService extends IService<OrdOrder> {

    OrdOrder createOrder(OrdOrder order, List<OrdOrderItem> items);

    boolean updateOrderStatus(Long orderId, Integer status);

    List<OrdOrder> listByUserId(Long userId);

    Page<OrdOrder> pageByUserId(Long userId, Integer pageNum, Integer pageSize);
}

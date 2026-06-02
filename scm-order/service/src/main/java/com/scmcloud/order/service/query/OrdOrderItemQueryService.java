package com.scmcloud.order.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.order.domain.entity.OrdOrderItem;
import com.scmcloud.order.mapper.OrdOrderItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrdOrderItemQueryService {

    private final OrdOrderItemMapper ordOrderItemMapper;

    @Slave
    public OrdOrderItem getById(String id) {
        return ordOrderItemMapper.selectById(id);
    }

    @Slave
    public List<OrdOrderItem> listByOrderId(Long orderId) {
        LambdaQueryWrapper<OrdOrderItem> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OrdOrderItem::getOrderId, orderId)
                .orderByAsc(OrdOrderItem::getCreateTime);
        return ordOrderItemMapper.selectList(wrapper);
    }
}

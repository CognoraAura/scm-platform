package com.scmcloud.order.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.order.domain.entity.OrdRefund;
import com.scmcloud.order.mapper.OrdRefundMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrdRefundQueryService {

    private final OrdRefundMapper ordRefundMapper;

    @Slave
    public OrdRefund getById(String id) {
        return ordRefundMapper.selectById(id);
    }

    @Slave
    public List<OrdRefund> listByOrderId(Long orderId) {
        LambdaQueryWrapper<OrdRefund> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OrdRefund::getOrderId, orderId)
                .orderByDesc(OrdRefund::getCreateTime);
        return ordRefundMapper.selectList(wrapper);
    }
}

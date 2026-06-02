package com.scmcloud.order.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.order.domain.entity.OrdPayment;
import com.scmcloud.order.mapper.OrdPaymentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrdPaymentCommandService {

    private final OrdPaymentMapper ordPaymentMapper;

    @Master(reason = "创建支付记录")
    @Transactional(rollbackFor = Exception.class)
    public int save(OrdPayment payment) {
        return ordPaymentMapper.insert(payment);
    }

    @Master(reason = "更新支付记录")
    @Transactional(rollbackFor = Exception.class)
    public int updateById(OrdPayment payment) {
        return ordPaymentMapper.updateById(payment);
    }

    @Master(reason = "删除支付记录")
    @Transactional(rollbackFor = Exception.class)
    public int removeById(String id) {
        return ordPaymentMapper.deleteById(id);
    }
}

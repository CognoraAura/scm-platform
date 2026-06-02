package com.scmcloud.order.service.query;

import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.order.domain.entity.OrdPayment;
import com.scmcloud.order.mapper.OrdPaymentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrdPaymentQueryService {

    private final OrdPaymentMapper ordPaymentMapper;

    @Slave
    public OrdPayment getById(String id) {
        return ordPaymentMapper.selectById(id);
    }

    @Slave
    public List<OrdPayment> list() {
        return ordPaymentMapper.selectList(null);
    }
}

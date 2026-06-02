package com.scmcloud.order.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.order.domain.entity.OrdRefund;
import com.scmcloud.order.mapper.OrdRefundMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrdRefundCommandService {

    private final OrdRefundMapper ordRefundMapper;

    @Master(reason = "创建退款记录")
    @Transactional(rollbackFor = Exception.class)
    public int save(OrdRefund refund) {
        return ordRefundMapper.insert(refund);
    }

    @Master(reason = "更新退款记录")
    @Transactional(rollbackFor = Exception.class)
    public int updateById(OrdRefund refund) {
        return ordRefundMapper.updateById(refund);
    }

    @Master(reason = "删除退款记录")
    @Transactional(rollbackFor = Exception.class)
    public int removeById(String id) {
        return ordRefundMapper.deleteById(id);
    }
}

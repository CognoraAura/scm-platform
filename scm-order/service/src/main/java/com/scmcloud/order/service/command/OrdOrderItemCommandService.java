package com.scmcloud.order.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.order.domain.entity.OrdOrderItem;
import com.scmcloud.order.mapper.OrdOrderItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrdOrderItemCommandService {

    private final OrdOrderItemMapper ordOrderItemMapper;

    @Master(reason = "创建订单明细")
    @Transactional(rollbackFor = Exception.class)
    public int save(OrdOrderItem item) {
        return ordOrderItemMapper.insert(item);
    }

    @Master(reason = "批量创建订单明细")
    @Transactional(rollbackFor = Exception.class)
    public int saveBatch(java.util.List<OrdOrderItem> items) {
        return items.stream().map(ordOrderItemMapper::insert).reduce(0, Integer::sum);
    }

    @Master(reason = "更新订单明细")
    @Transactional(rollbackFor = Exception.class)
    public int updateById(OrdOrderItem item) {
        return ordOrderItemMapper.updateById(item);
    }

    @Master(reason = "删除订单明细")
    @Transactional(rollbackFor = Exception.class)
    public int removeById(String id) {
        return ordOrderItemMapper.deleteById(id);
    }
}

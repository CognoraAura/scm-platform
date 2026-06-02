package com.scmcloud.order.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.order.domain.entity.OrdOrder;
import com.scmcloud.order.mapper.OrdOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrdOrderQueryService {

    private final OrdOrderMapper ordOrderMapper;

    @Slave
    public OrdOrder getById(String id) {
        return ordOrderMapper.selectById(id);
    }

    @Slave
    public List<OrdOrder> list() {
        return ordOrderMapper.selectList(null);
    }

    @Slave
    public Page<OrdOrder> page(Page<OrdOrder> page) {
        return ordOrderMapper.selectPage(page, null);
    }

    @Slave
    public List<OrdOrder> listByUserId(Long userId) {
        LambdaQueryWrapper<OrdOrder> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OrdOrder::getUserId, userId)
                .eq(OrdOrder::getDeleted, false)
                .orderByDesc(OrdOrder::getCreateTime);
        return ordOrderMapper.selectList(wrapper);
    }

    @Slave
    public Page<OrdOrder> pageByUserId(Long userId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<OrdOrder> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OrdOrder::getUserId, userId)
                .eq(OrdOrder::getDeleted, false)
                .orderByDesc(OrdOrder::getCreateTime);
        return ordOrderMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }
}

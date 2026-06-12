package com.scmcloud.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.scmcloud.order.domain.entity.OrdOrderEvent;

@Mapper
public interface OrdOrderEventMapper extends BaseMapper<OrdOrderEvent> {
}

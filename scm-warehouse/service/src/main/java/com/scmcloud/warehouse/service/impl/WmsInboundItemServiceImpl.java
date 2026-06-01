package com.scmcloud.warehouse.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.scmcloud.warehouse.domain.entity.WmsInboundItem;
import com.scmcloud.warehouse.mapper.WmsInboundItemMapper;
import com.scmcloud.warehouse.service.IWmsInboundItemService;

import java.util.List;

@Slf4j
@Service
public class WmsInboundItemServiceImpl extends ServiceImpl<WmsInboundItemMapper, WmsInboundItem>
        implements IWmsInboundItemService {

    @Override
    public List<WmsInboundItem> listByInboundId(String inboundId) {
        return lambdaQuery()
                .eq(WmsInboundItem::getInboundId, inboundId)
                .eq(WmsInboundItem::getDeleted, false)
                .list();
    }
}

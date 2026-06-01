package com.scmcloud.warehouse.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.scmcloud.warehouse.domain.entity.WmsOutboundItem;
import com.scmcloud.warehouse.mapper.WmsOutboundItemMapper;
import com.scmcloud.warehouse.service.IWmsOutboundItemService;

import java.util.List;

@Slf4j
@Service
public class WmsOutboundItemServiceImpl extends ServiceImpl<WmsOutboundItemMapper, WmsOutboundItem>
        implements IWmsOutboundItemService {

    @Override
    public List<WmsOutboundItem> listByOutboundId(String outboundId) {
        return lambdaQuery()
                .eq(WmsOutboundItem::getOutboundId, outboundId)
                .eq(WmsOutboundItem::getDeleted, false)
                .list();
    }
}

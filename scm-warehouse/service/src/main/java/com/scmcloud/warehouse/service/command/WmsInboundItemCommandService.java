package com.scmcloud.warehouse.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.warehouse.domain.entity.WmsInboundItem;
import com.scmcloud.warehouse.mapper.WmsInboundItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WmsInboundItemCommandService {

    private final WmsInboundItemMapper inboundItemMapper;

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public int save(WmsInboundItem item) {
        return inboundItemMapper.insert(item);
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public int updateById(WmsInboundItem item) {
        return inboundItemMapper.updateById(item);
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean softDeleteById(String id) {
        WmsInboundItem item = inboundItemMapper.selectById(id);
        if (item == null) {
            return false;
        }
        item.setDeleted(true);
        return inboundItemMapper.updateById(item) > 0;
    }
}

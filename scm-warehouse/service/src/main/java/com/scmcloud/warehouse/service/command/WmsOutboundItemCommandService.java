package com.scmcloud.warehouse.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.warehouse.domain.entity.WmsOutboundItem;
import com.scmcloud.warehouse.mapper.WmsOutboundItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WmsOutboundItemCommandService {

    private final WmsOutboundItemMapper outboundItemMapper;

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public int save(WmsOutboundItem item) {
        return outboundItemMapper.insert(item);
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public int updateById(WmsOutboundItem item) {
        return outboundItemMapper.updateById(item);
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean softDeleteById(String id) {
        WmsOutboundItem item = outboundItemMapper.selectById(id);
        if (item == null) {
            return false;
        }
        item.setDeleted(true);
        return outboundItemMapper.updateById(item) > 0;
    }
}

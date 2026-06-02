package com.scmcloud.warehouse.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.warehouse.domain.entity.WmsLocation;
import com.scmcloud.warehouse.mapper.WmsLocationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WmsLocationCommandService {

    private final WmsLocationMapper locationMapper;

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public int save(WmsLocation location) {
        return locationMapper.insert(location);
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public int updateById(WmsLocation location) {
        return locationMapper.updateById(location);
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean softDeleteById(String id) {
        WmsLocation location = locationMapper.selectById(id);
        if (location == null) {
            return false;
        }
        location.setDeleted(true);
        location.setUpdateTime(LocalDateTime.now());
        return locationMapper.updateById(location) > 0;
    }
}

package com.scmcloud.logistics.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.logistics.domain.entity.TmsDeliveryArea;
import com.scmcloud.logistics.mapper.TmsDeliveryAreaMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmsDeliveryAreaCommandService {

    private final TmsDeliveryAreaMapper tmsDeliveryAreaMapper;

    @Master(reason = "保存配送区域")
    @Transactional(rollbackFor = Exception.class)
    public boolean save(TmsDeliveryArea entity) {
        return tmsDeliveryAreaMapper.insert(entity) > 0;
    }

    @Master(reason = "更新配送区域")
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(TmsDeliveryArea entity) {
        return tmsDeliveryAreaMapper.updateById(entity) > 0;
    }

    @Master(reason = "删除配送区域")
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(String id) {
        return tmsDeliveryAreaMapper.deleteById(id) > 0;
    }
}

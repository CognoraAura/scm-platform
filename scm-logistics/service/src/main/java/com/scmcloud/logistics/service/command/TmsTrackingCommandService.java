package com.scmcloud.logistics.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.logistics.domain.entity.TmsTracking;
import com.scmcloud.logistics.mapper.TmsTrackingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmsTrackingCommandService {

    private final TmsTrackingMapper tmsTrackingMapper;

    @Master(reason = "保存物流轨迹")
    @Transactional(rollbackFor = Exception.class)
    public boolean save(TmsTracking entity) {
        return tmsTrackingMapper.insert(entity) > 0;
    }

    @Master(reason = "更新物流轨迹")
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(TmsTracking entity) {
        return tmsTrackingMapper.updateById(entity) > 0;
    }

    @Master(reason = "删除物流轨迹")
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(String id) {
        return tmsTrackingMapper.deleteById(id) > 0;
    }

    @Master(reason = "添加物流轨迹")
    @Transactional(rollbackFor = Exception.class)
    public TmsTracking addTracking(TmsTracking tracking) {
        log.info("添加物流轨迹: waybillNo={}, location={}, status={}", tracking.getWaybillNo(), tracking.getLocation(), tracking.getTrackStatus());
        if (tracking.getId() == null) {
            tracking.setId(java.util.UUID.randomUUID().toString());
        }
        if (tracking.getTrackTime() == null) {
            tracking.setTrackTime(LocalDateTime.now());
        }
        if (tracking.getCreateTime() == null) {
            tracking.setCreateTime(LocalDateTime.now());
        }
        boolean success = tmsTrackingMapper.insert(tracking) > 0;
        if (!success) {
            throw new RuntimeException("添加物流轨迹失败");
        }
        log.info("物流轨迹添加成功: id={}, waybillNo={}", tracking.getId(), tracking.getWaybillNo());
        return tracking;
    }
}

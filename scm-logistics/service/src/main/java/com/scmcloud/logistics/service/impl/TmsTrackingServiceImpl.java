package com.scmcloud.logistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.scmcloud.logistics.domain.entity.TmsTracking;
import com.scmcloud.logistics.mapper.TmsTrackingMapper;
import com.scmcloud.logistics.service.ITmsTrackingService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class TmsTrackingServiceImpl extends ServiceImpl<TmsTrackingMapper, TmsTracking> implements ITmsTrackingService {

    @Override
    public List<TmsTracking> listByWaybillId(String waybillId) {
        log.debug("根据运单ID查询物流轨迹: waybillId={}", waybillId);
        return lambdaQuery()
                .eq(TmsTracking::getWaybillId, waybillId)
                .orderByDesc(TmsTracking::getTrackTime)
                .list();
    }

    @Override
    public List<TmsTracking> listByWaybillNo(String waybillNo) {
        log.debug("根据运单号查询物流轨�? waybillNo={}", waybillNo);
        return lambdaQuery()
                .eq(TmsTracking::getWaybillNo, waybillNo)
                .orderByDesc(TmsTracking::getTrackTime)
                .list();
    }

    @Override
    public Page<TmsTracking> pageList(int page, int size, String waybillNo, String trackStatus) {
        log.debug("分页查询物流轨迹: page={}, size={}, waybillNo={}, trackStatus={}", page, size, waybillNo, trackStatus);

        LambdaQueryWrapper<TmsTracking> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(waybillNo)) {
            wrapper.eq(TmsTracking::getWaybillNo, waybillNo);
        }
        if (StringUtils.hasText(trackStatus)) {
            wrapper.eq(TmsTracking::getTrackStatus, trackStatus);
        }
        wrapper.orderByDesc(TmsTracking::getTrackTime);

        return page(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TmsTracking addTracking(TmsTracking tracking) {
        log.info("添加物流轨迹: waybillNo={}, location={}, status={}", tracking.getWaybillNo(), tracking.getLocation(), tracking.getTrackStatus());

        if (tracking.getId() == null) {
            tracking.setId(UUID.randomUUID().toString());
        }
        if (tracking.getTrackTime() == null) {
            tracking.setTrackTime(LocalDateTime.now());
        }
        if (tracking.getCreateTime() == null) {
            tracking.setCreateTime(LocalDateTime.now());
        }

        boolean success = save(tracking);
        if (!success) {
            throw new RuntimeException("添加物流轨迹失败");
        }

        log.info("物流轨迹添加成功: id={}, waybillNo={}", tracking.getId(), tracking.getWaybillNo());
        return tracking;
    }
}

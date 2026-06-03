package com.scmcloud.warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scmcloud.common.status.StatusValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.scmcloud.warehouse.domain.entity.WmsWavePicking;
import com.scmcloud.warehouse.mapper.WmsWavePickingMapper;
import com.scmcloud.warehouse.service.IWmsWavePickingService;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WmsWavePickingServiceImpl extends ServiceImpl<WmsWavePickingMapper, WmsWavePicking>
        implements IWmsWavePickingService {

    private final StatusValidator statusValidator;

    @Override
    public Page<WmsWavePicking> pageList(int page, int size, String warehouseId, Integer status) {
        LambdaQueryWrapper<WmsWavePicking> wrapper = Wrappers.lambdaQuery();

        if (StringUtils.hasText(warehouseId)) {
            wrapper.eq(WmsWavePicking::getWarehouseId, warehouseId);
        }
        if (status != null) {
            wrapper.eq(WmsWavePicking::getStatus, status);
        }
        wrapper.orderByDesc(WmsWavePicking::getCreateTime);

        return page(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean start(String waveId, String pickerId, String pickerName) {
        WmsWavePicking wave = getById(waveId);
        if (wave == null) {
            log.warn("波次拣货单不存在: id={}", waveId);
            return false;
        }
        statusValidator.validateTransition("WAVE_PICKING", "WAITING", "PICKING");

        wave.setStatus(1); // 1-拣货中        wave.setPickerId(pickerId);
        wave.setPickerName(pickerName);
        wave.setStartedAt(LocalDateTime.now());
        wave.setUpdateTime(LocalDateTime.now());
        wave.setUpdateBy(pickerId);

        boolean success = updateById(wave);
        if (success) {
            log.info("波次拣货已开始 id={}, waveNo={}, picker={}", waveId, wave.getWaveNo(), pickerName);
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean complete(String waveId, String operatorId) {
        WmsWavePicking wave = getById(waveId);
        if (wave == null) {
            log.warn("波次拣货单不存在: id={}", waveId);
            return false;
        }
        statusValidator.validateTransition("WAVE_PICKING", "PICKING", "COMPLETED");

        wave.setStatus(2); // 2-已完成        wave.setCompletedAt(LocalDateTime.now());
        wave.setUpdateTime(LocalDateTime.now());
        wave.setUpdateBy(operatorId);

        boolean success = updateById(wave);
        if (success) {
            log.info("波次拣货已完成 id={}, waveNo={}", waveId, wave.getWaveNo());
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(String waveId, String operatorId) {
        WmsWavePicking wave = getById(waveId);
        if (wave == null) {
            log.warn("波次拣货单不存在: id={}", waveId);
            return false;
        }
        String waveFromStatus;
        if (wave.getStatus() == 0) {
            waveFromStatus = "WAITING";
        } else if (wave.getStatus() == 1) {
            waveFromStatus = "PICKING";
        } else {
            waveFromStatus = "COMPLETED";
        }
        statusValidator.validateTransition("WAVE_PICKING", waveFromStatus, "CANCELLED");

        wave.setStatus(3); // 3-已取消        wave.setUpdateTime(LocalDateTime.now());
        wave.setUpdateBy(operatorId);

        boolean success = updateById(wave);
        if (success) {
            log.info("波次拣货已取消 id={}, waveNo={}", waveId, wave.getWaveNo());
        }
        return success;
    }
}

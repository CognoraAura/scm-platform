package com.scmcloud.warehouse.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.common.status.StatusValidator;
import com.scmcloud.warehouse.domain.entity.WmsWavePicking;
import com.scmcloud.warehouse.mapper.WmsWavePickingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WmsWavePickingCommandService {

    private final WmsWavePickingMapper wavePickingMapper;
    private final StatusValidator statusValidator;

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public int save(WmsWavePicking wave) {
        return wavePickingMapper.insert(wave);
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public int updateById(WmsWavePicking wave) {
        return wavePickingMapper.updateById(wave);
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(String id) {
        return wavePickingMapper.deleteById(id) > 0;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean start(String waveId, String pickerId, String pickerName) {
        WmsWavePicking wave = wavePickingMapper.selectById(waveId);
        if (wave == null) {
            log.warn("波次拣货单不存在: id={}", waveId);
            return false;
        }
        statusValidator.validateTransition("WAVE_PICKING", "WAITING", "PICKING");
        wave.setStatus(1);
        wave.setPickerId(pickerId);
        wave.setPickerName(pickerName);
        wave.setStartedAt(LocalDateTime.now());
        wave.setUpdateTime(LocalDateTime.now());
        wave.setUpdateBy(pickerId);
        boolean success = wavePickingMapper.updateById(wave) > 0;
        if (success) {
            log.info("波次拣货已开始: id={}, waveNo={}, picker={}", waveId, wave.getWaveNo(), pickerName);
        }
        return success;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean complete(String waveId, String operatorId) {
        WmsWavePicking wave = wavePickingMapper.selectById(waveId);
        if (wave == null) {
            log.warn("波次拣货单不存在: id={}", waveId);
            return false;
        }
        statusValidator.validateTransition("WAVE_PICKING", "PICKING", "COMPLETED");
        wave.setStatus(2);
        wave.setCompletedAt(LocalDateTime.now());
        wave.setUpdateTime(LocalDateTime.now());
        wave.setUpdateBy(operatorId);
        boolean success = wavePickingMapper.updateById(wave) > 0;
        if (success) {
            log.info("波次拣货已完成: id={}, waveNo={}", waveId, wave.getWaveNo());
        }
        return success;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(String waveId, String operatorId) {
        WmsWavePicking wave = wavePickingMapper.selectById(waveId);
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
        wave.setStatus(3);
        wave.setUpdateTime(LocalDateTime.now());
        wave.setUpdateBy(operatorId);
        boolean success = wavePickingMapper.updateById(wave) > 0;
        if (success) {
            log.info("波次拣货已取消: id={}, waveNo={}", waveId, wave.getWaveNo());
        }
        return success;
    }
}

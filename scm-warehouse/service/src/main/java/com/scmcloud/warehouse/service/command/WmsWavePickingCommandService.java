package com.scmcloud.warehouse.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
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
        if (wave.getStatus() != 0) {
            throw new IllegalStateException("波次拣货单状态不允许开始拣货，当前状态: " + wave.getStatus());
        }
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
        if (wave.getStatus() != 1) {
            throw new IllegalStateException("波次拣货单状态不允许完成，当前状态: " + wave.getStatus());
        }
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
        if (wave.getStatus() == 2) {
            throw new IllegalStateException("已完成的波次拣货单不能取消");
        }
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

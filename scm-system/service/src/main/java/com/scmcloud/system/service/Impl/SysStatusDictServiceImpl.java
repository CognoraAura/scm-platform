package com.scmcloud.system.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scmcloud.system.domain.entity.SysStatusDict;
import com.scmcloud.system.domain.entity.SysStatusTransition;
import com.scmcloud.system.mapper.SysStatusDictMapper;
import com.scmcloud.system.mapper.SysStatusTransitionMapper;
import com.scmcloud.system.service.ISysStatusDictService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysStatusDictServiceImpl implements ISysStatusDictService {

    private final SysStatusDictMapper statusDictMapper;
    private final SysStatusTransitionMapper transitionMapper;

    @Override
    public List<SysStatusDict> listStatusDict(String bizType) {
        LambdaQueryWrapper<SysStatusDict> wrapper = new LambdaQueryWrapper<SysStatusDict>()
                .eq(SysStatusDict::getBizType, bizType)
                .eq(SysStatusDict::getDeleted, false)
                .orderByAsc(SysStatusDict::getSortOrder);
        return statusDictMapper.selectList(wrapper);
    }

    @Override
    public SysStatusDict getStatusByCode(String bizType, String statusCode) {
        LambdaQueryWrapper<SysStatusDict> wrapper = new LambdaQueryWrapper<SysStatusDict>()
                .eq(SysStatusDict::getBizType, bizType)
                .eq(SysStatusDict::getStatusCode, statusCode)
                .eq(SysStatusDict::getDeleted, false);
        return statusDictMapper.selectOne(wrapper);
    }

    @Override
    public void createStatusDict(SysStatusDict entity) {
        statusDictMapper.insert(entity);
    }

    @Override
    public void updateStatusDict(SysStatusDict entity) {
        statusDictMapper.updateById(entity);
    }

    @Override
    public void deleteStatusDict(String id) {
        statusDictMapper.deleteById(id);
    }

    @Override
    public List<SysStatusTransition> listTransitions(String bizType) {
        LambdaQueryWrapper<SysStatusTransition> wrapper = new LambdaQueryWrapper<SysStatusTransition>()
                .eq(SysStatusTransition::getBizType, bizType)
                .eq(SysStatusTransition::getDeleted, false)
                .orderByAsc(SysStatusTransition::getSortOrder);
        return transitionMapper.selectList(wrapper);
    }

    @Override
    public List<SysStatusTransition> listTransitionsFrom(String bizType, String fromStatus) {
        LambdaQueryWrapper<SysStatusTransition> wrapper = new LambdaQueryWrapper<SysStatusTransition>()
                .eq(SysStatusTransition::getBizType, bizType)
                .eq(SysStatusTransition::getFromStatus, fromStatus)
                .eq(SysStatusTransition::getDeleted, false)
                .orderByAsc(SysStatusTransition::getSortOrder);
        return transitionMapper.selectList(wrapper);
    }

    @Override
    public boolean canTransition(String bizType, String fromStatus, String toStatus) {
        LambdaQueryWrapper<SysStatusTransition> wrapper = new LambdaQueryWrapper<SysStatusTransition>()
                .eq(SysStatusTransition::getBizType, bizType)
                .eq(SysStatusTransition::getFromStatus, fromStatus)
                .eq(SysStatusTransition::getToStatus, toStatus)
                .eq(SysStatusTransition::getEnabled, true)
                .eq(SysStatusTransition::getDeleted, false);
        return transitionMapper.selectCount(wrapper) > 0;
    }

    @Override
    public SysStatusTransition findTransition(String bizType, String fromStatus, String toStatus, String actionCode) {
        LambdaQueryWrapper<SysStatusTransition> wrapper = new LambdaQueryWrapper<SysStatusTransition>()
                .eq(SysStatusTransition::getBizType, bizType)
                .eq(SysStatusTransition::getFromStatus, fromStatus)
                .eq(SysStatusTransition::getToStatus, toStatus)
                .eq(SysStatusTransition::getActionCode, actionCode)
                .eq(SysStatusTransition::getDeleted, false);
        return transitionMapper.selectOne(wrapper);
    }

    @Override
    public void createTransition(SysStatusTransition entity) {
        transitionMapper.insert(entity);
    }

    @Override
    public void updateTransition(SysStatusTransition entity) {
        transitionMapper.updateById(entity);
    }

    @Override
    public void deleteTransition(String id) {
        transitionMapper.deleteById(id);
    }
}

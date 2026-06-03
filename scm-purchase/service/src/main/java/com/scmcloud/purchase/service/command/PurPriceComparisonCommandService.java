package com.scmcloud.purchase.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.purchase.domain.entity.PurPriceComparison;
import com.scmcloud.purchase.mapper.PurPriceComparisonMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scmcloud.common.status.StatusValidator;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurPriceComparisonCommandService {

    private final PurPriceComparisonMapper purPriceComparisonMapper;
    private final StatusValidator statusValidator;

    @Master(reason = "保存比价分析")
    @Transactional(rollbackFor = Exception.class)
    public boolean save(PurPriceComparison entity) {
        return purPriceComparisonMapper.insert(entity) > 0;
    }

    @Master(reason = "更新比价分析")
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(PurPriceComparison entity) {
        return purPriceComparisonMapper.updateById(entity) > 0;
    }

    @Master(reason = "删除比价分析")
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(String id) {
        return purPriceComparisonMapper.deleteById(id) > 0;
    }

    @Master(reason = "审批比价分析")
    @Transactional(rollbackFor = Exception.class)
    public boolean approve(String id, String approverId, String approverName) {
        PurPriceComparison comparison = purPriceComparisonMapper.selectById(id);
        if (comparison == null || comparison.getDeleted()) {
            throw new IllegalArgumentException("比价分析不存在: " + id);
        }
        statusValidator.validateTransition("PURCHASE", "PENDING_APPROVAL", "APPROVED");
        comparison.setStatus(2); // APPROVED
        comparison.setApprovedBy(approverId);
        comparison.setApprovedByName(approverName);
        comparison.setApprovedAt(LocalDateTime.now());
        comparison.setUpdateTime(LocalDateTime.now());
        return purPriceComparisonMapper.updateById(comparison) > 0;
    }
}

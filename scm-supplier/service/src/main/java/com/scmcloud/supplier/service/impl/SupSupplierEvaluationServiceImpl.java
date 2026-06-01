package com.scmcloud.supplier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scmcloud.supplier.domain.entity.SupSupplierEvaluation;
import com.scmcloud.supplier.mapper.SupSupplierEvaluationMapper;
import com.scmcloud.supplier.service.ISupSupplierEvaluationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
public class SupSupplierEvaluationServiceImpl
        extends ServiceImpl<SupSupplierEvaluationMapper, SupSupplierEvaluation>
        implements ISupSupplierEvaluationService {

    @Override
    public Page<SupSupplierEvaluation> pageList(int page, int size, String supplierId,
                                                String evaluationPeriod) {
        log.debug("分页查询供应商评�? page={}, size={}, supplierId={}", page, size, supplierId);

        LambdaQueryWrapper<SupSupplierEvaluation> wrapper = Wrappers.lambdaQuery();

        if (StringUtils.hasText(supplierId)) {
            wrapper.eq(SupSupplierEvaluation::getSupplierId, supplierId);
        }
        if (StringUtils.hasText(evaluationPeriod)) {
            wrapper.eq(SupSupplierEvaluation::getEvaluationPeriod, evaluationPeriod);
        }

        wrapper.orderByDesc(SupSupplierEvaluation::getEvaluatedAt);

        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public List<SupSupplierEvaluation> listBySupplierId(String supplierId) {
        if (!StringUtils.hasText(supplierId)) {
            return List.of();
        }
        log.debug("查询供应商的所有评�? supplierId={}", supplierId);
        return lambdaQuery()
                .eq(SupSupplierEvaluation::getSupplierId, supplierId)
                .orderByDesc(SupSupplierEvaluation::getEvaluatedAt)
                .list();
    }

    @Override
    public BigDecimal calculateAverageScore(String supplierId) {
        if (!StringUtils.hasText(supplierId)) {
            return BigDecimal.ZERO;
        }
        log.debug("计算供应商平均评�? supplierId={}", supplierId);

        List<SupSupplierEvaluation> evaluations = lambdaQuery()
                .eq(SupSupplierEvaluation::getSupplierId, supplierId)
                .select(SupSupplierEvaluation::getTotalScore)
                .list();

        if (evaluations.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal sum = evaluations.stream()
                .map(SupSupplierEvaluation::getTotalScore)
                .filter(score -> score != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long count = evaluations.stream()
                .filter(e -> e.getTotalScore() != null)
                .count();

        if (count == 0) {
            return BigDecimal.ZERO;
        }

        return sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }
}

package com.scmcloud.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scmcloud.finance.domain.entity.PlatformServiceFee;
import com.scmcloud.finance.mapper.PlatformServiceFeeMapper;
import com.scmcloud.finance.service.IPlatformServiceFeeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class PlatformServiceFeeServiceImpl extends ServiceImpl<PlatformServiceFeeMapper, PlatformServiceFee>
        implements IPlatformServiceFeeService {

    @Override
    public List<PlatformServiceFee> listPendingFees() {
        log.debug("鏌ヨ寰呬粯娆惧钩鍙版湇鍔¤垂");
        LambdaQueryWrapper<PlatformServiceFee> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(PlatformServiceFee::getStatus, 0)
                .orderByDesc(PlatformServiceFee::getCreateTime);
        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlatformServiceFee recordPayment(String id, BigDecimal paidAmount) {
        log.info("璁板綍骞冲彴鏈嶅姟璐逛粯锟?id={}, paidAmount={}", id, paidAmount);

        if (paidAmount == null || paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("浠樻閲戦蹇呴』澶т簬0");
        }

        PlatformServiceFee fee = getById(id);
        if (fee == null) {
            throw new IllegalArgumentException("骞冲彴鏈嶅姟璐硅褰曚笉瀛樺湪: " + id);
        }
        if (fee.getStatus() != 0) {
            throw new IllegalStateException("鍙湁寰呬粯娆剧姸鎬佺殑璁板綍鎵嶈兘浠樻, 褰撳墠鐘讹拷 " + fee.getStatus());
        }

        BigDecimal finalFee = fee.getFinalFee() != null ? fee.getFinalFee() : fee.getTotalFee();
        if (paidAmount.compareTo(finalFee) != 0) {
            throw new IllegalArgumentException(
                    String.format("浠樻閲戦涓庡簲浠橀噾棰濅笉涓€锟?paid=%s, final=%s", paidAmount, finalFee));
        }

        fee.setPaidAmount(paidAmount);
        fee.setPaidAt(LocalDateTime.now());
        fee.setStatus(1);
        fee.setUpdateTime(LocalDateTime.now());

        updateById(fee);
        log.info("骞冲彴鏈嶅姟璐逛粯娆炬垚锟?id={}", id);
        return fee;
    }
}

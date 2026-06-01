package com.scmcloud.finance.service;

import com.scmcloud.finance.domain.entity.PlatformServiceFee;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;

public interface IPlatformServiceFeeService extends IService<PlatformServiceFee> {

    List<PlatformServiceFee> listPendingFees();

    PlatformServiceFee recordPayment(String id, BigDecimal paidAmount);
}

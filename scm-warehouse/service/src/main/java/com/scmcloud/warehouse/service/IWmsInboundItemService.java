package com.scmcloud.warehouse.service;

import com.scmcloud.warehouse.domain.entity.WmsInboundItem;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IWmsInboundItemService extends IService<WmsInboundItem> {

    List<WmsInboundItem> listByInboundId(String inboundId);
}

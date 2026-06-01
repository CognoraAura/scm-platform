package com.scmcloud.warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.scmcloud.warehouse.domain.entity.WmsOutbound;
import com.scmcloud.warehouse.domain.entity.WmsOutboundItem;
import com.scmcloud.warehouse.mapper.WmsOutboundMapper;
import com.scmcloud.warehouse.service.IWmsOutboundItemService;
import com.scmcloud.warehouse.service.IWmsOutboundService;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class WmsOutboundServiceImpl extends ServiceImpl<WmsOutboundMapper, WmsOutbound>
        implements IWmsOutboundService {

    private final IWmsOutboundItemService outboundItemService;

    @Override
    public Page<WmsOutbound> pageList(int page, int size, String warehouseId, Integer outboundType, Integer status) {
        LambdaQueryWrapper<WmsOutbound> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(WmsOutbound::getDeleted, false);

        if (StringUtils.hasText(warehouseId)) {
            wrapper.eq(WmsOutbound::getWarehouseId, warehouseId);
        }
        if (outboundType != null) {
            wrapper.eq(WmsOutbound::getOutboundType, outboundType);
        }
        if (status != null) {
            wrapper.eq(WmsOutbound::getStatus, status);
        }
        wrapper.orderByDesc(WmsOutbound::getCreateTime);

        return page(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean ship(String outboundId, String operatorId, String operatorName) {
        WmsOutbound outbound = getById(outboundId);
        if (outbound == null) {
            log.warn("出库单不存在: id={}", outboundId);
            return false;
        }
        if (outbound.getStatus() != 0 && outbound.getStatus() != 1 && outbound.getStatus() != 2) {
            throw new IllegalStateException("出库单状态不允许出库，当前状�? " + outbound.getStatus());
        }

        List<WmsOutboundItem> items = outboundItemService.lambdaQuery()
                .eq(WmsOutboundItem::getOutboundId, outboundId)
                .eq(WmsOutboundItem::getDeleted, false)
                .list();

        int totalPicked = items.stream()
                .mapToInt(item -> item.getActualQuantity() != null ? item.getActualQuantity() : 0)
                .sum();

        if (totalPicked == 0) {
            throw new IllegalStateException("出库明细实际拣货数量不能全部�?");
        }

        outbound.setPickedQuantity(totalPicked);
        outbound.setStatus(3); // 3-已出�?
        outbound.setPickerId(operatorId);
        outbound.setPickerName(operatorName);
        outbound.setCompletedAt(LocalDateTime.now());
        outbound.setUpdateTime(LocalDateTime.now());
        outbound.setUpdateBy(operatorId);

        boolean success = updateById(outbound);
        if (success) {
            log.info("出库单已出库: id={}, outboundNo={}, pickedQty={}",
                    outboundId, outbound.getOutboundNo(), totalPicked);
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(String outboundId, String operatorId, String operatorName) {
        WmsOutbound outbound = getById(outboundId);
        if (outbound == null) {
            log.warn("出库单不存在: id={}", outboundId);
            return false;
        }
        if (outbound.getStatus() == 3) {
            throw new IllegalStateException("已出库的出库单不能取�?);
        }

        outbound.setStatus(4); // 4-已取�?
        outbound.setUpdateTime(LocalDateTime.now());
        outbound.setUpdateBy(operatorId);

        boolean success = updateById(outbound);
        if (success) {
            log.info("出库单已取消: id={}, outboundNo={}", outboundId, outbound.getOutboundNo());
        }
        return success;
    }
}

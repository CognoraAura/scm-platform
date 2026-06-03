package com.scmcloud.warehouse.service.command;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.common.status.StatusValidator;
import com.scmcloud.warehouse.domain.entity.WmsOutbound;
import com.scmcloud.warehouse.domain.entity.WmsOutboundItem;
import com.scmcloud.warehouse.mapper.WmsOutboundItemMapper;
import com.scmcloud.warehouse.mapper.WmsOutboundMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WmsOutboundCommandService {

    private final WmsOutboundMapper outboundMapper;
    private final WmsOutboundItemMapper outboundItemMapper;
    private final StatusValidator statusValidator;

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public int save(WmsOutbound outbound) {
        return outboundMapper.insert(outbound);
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public int updateById(WmsOutbound outbound) {
        return outboundMapper.updateById(outbound);
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean softDeleteById(String id) {
        WmsOutbound outbound = outboundMapper.selectById(id);
        if (outbound == null) {
            return false;
        }
        outbound.setDeleted(true);
        outbound.setUpdateTime(LocalDateTime.now());
        return outboundMapper.updateById(outbound) > 0;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean ship(String outboundId, String operatorId, String operatorName) {
        WmsOutbound outbound = outboundMapper.selectById(outboundId);
        if (outbound == null) {
            log.warn("出库单不存在: id={}", outboundId);
            return false;
        }
        String shipFromStatus;
        if (outbound.getStatus() == 0) {
            shipFromStatus = "WAITING";
        } else if (outbound.getStatus() == 1) {
            shipFromStatus = "PICKING";
        } else if (outbound.getStatus() == 2) {
            shipFromStatus = "PACKED";
        } else {
            shipFromStatus = "SHIPPED";
        }
        statusValidator.validateTransition("OUTBOUND", shipFromStatus, "SHIPPED");
        LambdaQueryWrapper<WmsOutboundItem> itemWrapper = Wrappers.lambdaQuery();
        itemWrapper.eq(WmsOutboundItem::getOutboundId, outboundId);
        itemWrapper.eq(WmsOutboundItem::getDeleted, false);
        List<WmsOutboundItem> items = outboundItemMapper.selectList(itemWrapper);
        int totalPicked = items.stream()
                .mapToInt(item -> item.getActualQuantity() != null ? item.getActualQuantity() : 0)
                .sum();
        if (totalPicked == 0) {
            throw new IllegalStateException("出库明细实际拣货数量不能全部为0");
        }
        outbound.setPickedQuantity(totalPicked);
        outbound.setStatus(3);
        outbound.setPickerId(operatorId);
        outbound.setPickerName(operatorName);
        outbound.setCompletedAt(LocalDateTime.now());
        outbound.setUpdateTime(LocalDateTime.now());
        outbound.setUpdateBy(operatorId);
        boolean success = outboundMapper.updateById(outbound) > 0;
        if (success) {
            log.info("出库单已出库: id={}, outboundNo={}, pickedQty={}",
                    outboundId, outbound.getOutboundNo(), totalPicked);
        }
        return success;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(String outboundId, String operatorId, String operatorName) {
        WmsOutbound outbound = outboundMapper.selectById(outboundId);
        if (outbound == null) {
            log.warn("出库单不存在: id={}", outboundId);
            return false;
        }
        String cancelFromStatus;
        if (outbound.getStatus() == 0) {
            cancelFromStatus = "WAITING";
        } else if (outbound.getStatus() == 1) {
            cancelFromStatus = "PICKING";
        } else if (outbound.getStatus() == 2) {
            cancelFromStatus = "PACKED";
        } else {
            cancelFromStatus = "SHIPPED";
        }
        statusValidator.validateTransition("OUTBOUND", cancelFromStatus, "CANCELLED");
        outbound.setStatus(4);
        outbound.setUpdateTime(LocalDateTime.now());
        outbound.setUpdateBy(operatorId);
        boolean success = outboundMapper.updateById(outbound) > 0;
        if (success) {
            log.info("出库单已取消: id={}, outboundNo={}", outboundId, outbound.getOutboundNo());
        }
        return success;
    }
}

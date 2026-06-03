package com.scmcloud.warehouse.service.command;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.common.status.StatusValidator;
import com.scmcloud.warehouse.domain.entity.WmsInbound;
import com.scmcloud.warehouse.domain.entity.WmsInboundItem;
import com.scmcloud.warehouse.mapper.WmsInboundItemMapper;
import com.scmcloud.warehouse.mapper.WmsInboundMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WmsInboundCommandService {

    private final WmsInboundMapper inboundMapper;
    private final WmsInboundItemMapper inboundItemMapper;
    private final StatusValidator statusValidator;

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public int save(WmsInbound inbound) {
        return inboundMapper.insert(inbound);
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public int updateById(WmsInbound inbound) {
        return inboundMapper.updateById(inbound);
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean softDeleteById(String id) {
        WmsInbound inbound = inboundMapper.selectById(id);
        if (inbound == null) {
            return false;
        }
        inbound.setDeleted(true);
        inbound.setUpdateTime(LocalDateTime.now());
        return inboundMapper.updateById(inbound) > 0;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean receive(String inboundId, String operatorId, String operatorName) {
        WmsInbound inbound = inboundMapper.selectById(inboundId);
        if (inbound == null) {
            log.warn("入库单不存在: id={}", inboundId);
            return false;
        }
        LambdaQueryWrapper<WmsInboundItem> itemWrapper = Wrappers.lambdaQuery();
        itemWrapper.eq(WmsInboundItem::getInboundId, inboundId);
        itemWrapper.eq(WmsInboundItem::getDeleted, false);
        List<WmsInboundItem> items = inboundItemMapper.selectList(itemWrapper);
        int totalReceived = items.stream()
                .mapToInt(item -> item.getActualQuantity() != null ? item.getActualQuantity() : 0)
                .sum();
        if (totalReceived == 0) {
            throw new IllegalStateException("入库明细实际收货数量不能全部为0");
        }
        boolean allReceived = items.stream()
                .allMatch(item -> item.getActualQuantity() != null && item.getActualQuantity().equals(item.getPlanQuantity()));
        String receiveFromStatus;
        if (inbound.getStatus() == 0) {
            receiveFromStatus = "WAITING";
        } else {
            receiveFromStatus = "PROCESSING";
        }
        statusValidator.validateTransition("INBOUND", receiveFromStatus, allReceived ? "CANCELLED" : "FINISHED");
        inbound.setReceivedQuantity(totalReceived);
        inbound.setStatus(allReceived ? 3 : 2);
        inbound.setOperatorId(operatorId);
        inbound.setOperatorName(operatorName);
        inbound.setUpdateTime(LocalDateTime.now());
        inbound.setUpdateBy(operatorId);
        if (allReceived) {
            inbound.setCompletedAt(LocalDateTime.now());
        }
        boolean success = inboundMapper.updateById(inbound) > 0;
        if (success) {
            log.info("入库单收货完成: id={}, inboundNo={}, status={}, receivedQty={}",
                    inboundId, inbound.getInboundNo(), inbound.getStatus(), totalReceived);
        }
        return success;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(String inboundId, String operatorId, String operatorName) {
        WmsInbound inbound = inboundMapper.selectById(inboundId);
        if (inbound == null) {
            log.warn("入库单不存在: id={}", inboundId);
            return false;
        }
        String inboundCancelFromStatus;
        if (inbound.getStatus() == 0) {
            inboundCancelFromStatus = "WAITING";
        } else if (inbound.getStatus() == 1) {
            inboundCancelFromStatus = "PROCESSING";
        } else {
            inboundCancelFromStatus = "FINISHED";
        }
        statusValidator.validateTransition("INBOUND", inboundCancelFromStatus, "CANCELLED");
        inbound.setStatus(4);
        inbound.setOperatorId(operatorId);
        inbound.setOperatorName(operatorName);
        inbound.setUpdateTime(LocalDateTime.now());
        inbound.setUpdateBy(operatorId);
        boolean success = inboundMapper.updateById(inbound) > 0;
        if (success) {
            log.info("入库单已取消: id={}, inboundNo={}", inboundId, inbound.getInboundNo());
        }
        return success;
    }
}

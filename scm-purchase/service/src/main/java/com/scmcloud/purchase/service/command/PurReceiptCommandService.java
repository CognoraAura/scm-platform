package com.scmcloud.purchase.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.purchase.domain.entity.PurReceipt;
import com.scmcloud.purchase.mapper.PurReceiptMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurReceiptCommandService {

    private final PurReceiptMapper purReceiptMapper;

    @Master(reason = "保存入库单")
    @Transactional(rollbackFor = Exception.class)
    public boolean save(PurReceipt entity) {
        return purReceiptMapper.insert(entity) > 0;
    }

    @Master(reason = "更新入库单")
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(PurReceipt entity) {
        return purReceiptMapper.updateById(entity) > 0;
    }

    @Master(reason = "删除入库单")
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(String id) {
        return purReceiptMapper.deleteById(id) > 0;
    }

    @Master(reason = "收货")
    @Transactional(rollbackFor = Exception.class)
    public boolean receive(String id, String receiverId, String receiverName) {
        PurReceipt receipt = purReceiptMapper.selectById(id);
        if (receipt == null || receipt.getDeleted()) {
            throw new IllegalArgumentException("入库单不存在: " + id);
        }
        if (receipt.getStatus() != 0) {
            throw new IllegalStateException("只有待收货的入库单才能收货");
        }
        receipt.setStatus(1);
        receipt.setReceiverId(receiverId);
        receipt.setReceiverName(receiverName);
        receipt.setReceivedAt(LocalDateTime.now());
        receipt.setUpdateTime(LocalDateTime.now());
        return purReceiptMapper.updateById(receipt) > 0;
    }

    @Master(reason = "质检")
    @Transactional(rollbackFor = Exception.class)
    public boolean qualityInspect(String id, String inspectorId, String inspectorName, Integer result, String remark) {
        PurReceipt receipt = purReceiptMapper.selectById(id);
        if (receipt == null || receipt.getDeleted()) {
            throw new IllegalArgumentException("入库单不存在: " + id);
        }
        if (receipt.getStatus() != 1) {
            throw new IllegalStateException("只有已收货的入库单才能质检");
        }
        receipt.setStatus(2);
        receipt.setQualityInspectorId(inspectorId);
        receipt.setQualityInspectorName(inspectorName);
        receipt.setQualityInspectedAt(LocalDateTime.now());
        receipt.setQualityResult(result);
        receipt.setQualityRemark(remark);
        receipt.setUpdateTime(LocalDateTime.now());
        return purReceiptMapper.updateById(receipt) > 0;
    }

    @Master(reason = "上架")
    @Transactional(rollbackFor = Exception.class)
    public boolean shelve(String id, String shelvedBy, String shelvedByName) {
        PurReceipt receipt = purReceiptMapper.selectById(id);
        if (receipt == null || receipt.getDeleted()) {
            throw new IllegalArgumentException("入库单不存在: " + id);
        }
        if (receipt.getStatus() != 2) {
            throw new IllegalStateException("只有已质检的入库单才能上架");
        }
        receipt.setStatus(3);
        receipt.setShelved(true);
        receipt.setShelvedBy(shelvedBy);
        receipt.setShelvedByName(shelvedByName);
        receipt.setShelvedAt(LocalDateTime.now());
        receipt.setUpdateTime(LocalDateTime.now());
        return purReceiptMapper.updateById(receipt) > 0;
    }
}

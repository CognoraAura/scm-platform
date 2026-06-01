package com.scmcloud.inventory.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scmcloud.inventory.domain.entity.Inventory;
import com.scmcloud.inventory.domain.entity.InvSnapshot;
import com.scmcloud.inventory.mapper.InvInventoryMapper;
import com.scmcloud.inventory.mapper.InvSnapshotMapper;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存快照定时同步任务
 *
 * <p>定时生成库存快照，用于数据分析和审计
 *
 * <p>执行频率: 每天凌晨 1 点执�?
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventorySnapshotJobHandler {
    private final InvInventoryMapper inventoryMapper;
    private final InvSnapshotMapper snapshotMapper;

    /**
     * 库存快照同步任务
     */
    @XxlJob("inventorySnapshotJobHandler")
    @Transactional(rollbackFor = Exception.class)
    public void execute() throws Exception {
        long startTime = System.currentTimeMillis();
        XxlJobHelper.log("📸 [库存快照] 开始执行任�?);

        try {
            // 1. 查询所有库存记�?
            List<Inventory> inventories = inventoryMapper.selectList(
                    new LambdaQueryWrapper<Inventory>()
                            .isNotNull(Inventory::getSkuId)
            );

            if (inventories.isEmpty()) {
                XxlJobHelper.log("�?[库存快照] 无库存数据，任务结束");
                return;
            }

            XxlJobHelper.log("📋 [库存快照] 发现库存记录: count={}", inventories.size());

            // 2. 生成快照
            int successCount = 0;
            int failCount = 0;
            LocalDateTime snapshotTime = LocalDateTime.now();

            for (Inventory inventory : inventories) {
                try {
                    InvSnapshot snapshot = new InvSnapshot();
                    snapshot.setSkuId(inventory.getSkuId());
                    snapshot.setWarehouseId(inventory.getWarehouseId());
                    snapshot.setAvailableStock(inventory.getAvailableStock());
                    snapshot.setLockedStock(inventory.getLockedStock());
                    snapshot.setSnapshotDate(snapshotTime.toLocalDate());

                    snapshotMapper.insert(snapshot);
                    successCount++;

                    if (successCount % 100 == 0) {
                        XxlJobHelper.log("  📊 已生成快�? {}/{}", successCount, inventories.size());
                    }
                } catch (Exception e) {
                    failCount++;
                    XxlJobHelper.log("  �?快照生成失败: skuId={}, error={}",
                            inventory.getSkuId(), e.getMessage());
                    log.error("库存快照生成失败", e);
                }
            }

            // 3. 统计结果
            long duration = System.currentTimeMillis() - startTime;
            XxlJobHelper.log("🎉 [库存快照] 任务完成: 总数={}, 成功={}, 失败={}, 耗时={}ms",
                    inventories.size(), successCount, failCount, duration);

            // 4. 设置任务结果
            if (failCount > 0) {
                XxlJobHelper.handleFail(String.format("部分快照生成失败: 总数=%d, 成功=%d, 失败=%d",
                        inventories.size(), successCount, failCount));
            } else {
                XxlJobHelper.handleSuccess(String.format("所有快照生成成�? 总数=%d, 耗时=%dms",
                        successCount, duration));
            }

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            XxlJobHelper.log("�?[库存快照] 任务异常: error={}, 耗时={}ms",
                    e.getMessage(), duration);
            log.error("库存快照任务执行失败", e);
            XxlJobHelper.handleFail("任务执行失败: " + e.getMessage());
            throw e;
        }
    }
}
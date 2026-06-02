package com.scmcloud.warehouse.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.warehouse.domain.entity.WmsWarehouse;
import com.scmcloud.warehouse.mapper.WmsWarehouseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WmsWarehouseCommandService {

    private final WmsWarehouseMapper warehouseMapper;

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public int save(WmsWarehouse warehouse) {
        return warehouseMapper.insert(warehouse);
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public int updateById(WmsWarehouse warehouse) {
        return warehouseMapper.updateById(warehouse);
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean softDeleteById(String id) {
        WmsWarehouse warehouse = warehouseMapper.selectById(id);
        if (warehouse == null) {
            return false;
        }
        warehouse.setDeleted(true);
        warehouse.setUpdateTime(LocalDateTime.now());
        return warehouseMapper.updateById(warehouse) > 0;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean enable(String id) {
        WmsWarehouse warehouse = warehouseMapper.selectById(id);
        if (warehouse == null) {
            log.warn("仓库不存在: id={}", id);
            return false;
        }
        warehouse.setEnabled(true);
        warehouse.setUpdateTime(LocalDateTime.now());
        boolean success = warehouseMapper.updateById(warehouse) > 0;
        if (success) {
            log.info("仓库已启用: id={}, name={}", id, warehouse.getWarehouseName());
        }
        return success;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean disable(String id) {
        WmsWarehouse warehouse = warehouseMapper.selectById(id);
        if (warehouse == null) {
            log.warn("仓库不存在: id={}", id);
            return false;
        }
        warehouse.setEnabled(false);
        warehouse.setUpdateTime(LocalDateTime.now());
        boolean success = warehouseMapper.updateById(warehouse) > 0;
        if (success) {
            log.info("仓库已停用: id={}, name={}", id, warehouse.getWarehouseName());
        }
        return success;
    }
}

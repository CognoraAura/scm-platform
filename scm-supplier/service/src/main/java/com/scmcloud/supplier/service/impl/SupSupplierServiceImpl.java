package com.scmcloud.supplier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scmcloud.supplier.domain.entity.SupSupplier;
import com.scmcloud.supplier.mapper.SupSupplierMapper;
import com.scmcloud.supplier.service.ISupSupplierService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class SupSupplierServiceImpl extends ServiceImpl<SupSupplierMapper, SupSupplier>
        implements ISupSupplierService {

    @Override
    public Page<SupSupplier> pageList(int page, int size, String keyword, Integer supplierType,
                                      Integer cooperationStatus, Boolean enabled) {
        log.debug("分页查询供应�? page={}, size={}, keyword={}", page, size, keyword);

        LambdaQueryWrapper<SupSupplier> wrapper = Wrappers.lambdaQuery();

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(SupSupplier::getSupplierName, keyword)
                    .or().like(SupSupplier::getSupplierCode, keyword)
                    .or().like(SupSupplier::getContactName, keyword)
            );
        }
        if (supplierType != null) {
            wrapper.eq(SupSupplier::getSupplierType, supplierType);
        }
        if (cooperationStatus != null) {
            wrapper.eq(SupSupplier::getCooperationStatus, cooperationStatus);
        }
        if (enabled != null) {
            wrapper.eq(SupSupplier::getEnabled, enabled);
        }

        wrapper.eq(SupSupplier::getDeleted, false);
        wrapper.orderByAsc(SupSupplier::getSortOrder);
        wrapper.orderByDesc(SupSupplier::getCreateTime);

        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public List<SupSupplier> listActive() {
        log.debug("查询所有启用且合作中的供应�?);
        return lambdaQuery()
                .eq(SupSupplier::getEnabled, true)
                .eq(SupSupplier::getCooperationStatus, 1)
                .eq(SupSupplier::getDeleted, false)
                .orderByAsc(SupSupplier::getSortOrder)
                .list();
    }

    @Override
    public List<SupSupplier> searchByName(String name) {
        if (!StringUtils.hasText(name)) {
            return List.of();
        }
        log.debug("按名称搜索供应商: name={}", name);
        return lambdaQuery()
                .like(SupSupplier::getSupplierName, name)
                .eq(SupSupplier::getDeleted, false)
                .last("LIMIT 20")
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean enableSupplier(String id) {
        log.info("启用供应�? id={}", id);
        SupSupplier supplier = getById(id);
        if (supplier == null) {
            log.warn("供应商不存在: id={}", id);
            return false;
        }
        supplier.setEnabled(true);
        supplier.setUpdateTime(LocalDateTime.now());
        return updateById(supplier);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean disableSupplier(String id) {
        log.info("停用供应�? id={}", id);
        SupSupplier supplier = getById(id);
        if (supplier == null) {
            log.warn("供应商不存在: id={}", id);
            return false;
        }
        supplier.setEnabled(false);
        supplier.setUpdateTime(LocalDateTime.now());
        return updateById(supplier);
    }
}

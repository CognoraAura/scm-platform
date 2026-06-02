package com.scmcloud.tenant.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.tenant.domain.entity.TenantPackage;
import com.scmcloud.tenant.mapper.TenantPackageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantPackageQueryService {

    private final TenantPackageMapper tenantPackageMapper;

    @Slave
    public TenantPackage getById(String id) {
        LambdaQueryWrapper<TenantPackage> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TenantPackage::getId, id)
                .eq(TenantPackage::getDeleted, false);
        return tenantPackageMapper.selectOne(wrapper);
    }

    @Slave
    public List<TenantPackage> listActive() {
        LambdaQueryWrapper<TenantPackage> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TenantPackage::getDeleted, false)
                .eq(TenantPackage::getEnabled, true)
                .orderByAsc(TenantPackage::getSortOrder);
        return tenantPackageMapper.selectList(wrapper);
    }

    @Slave
    public Page<TenantPackage> pageQuery(int page, int size, String packageName, Integer packageLevel, Boolean enabled) {
        LambdaQueryWrapper<TenantPackage> wrapper = Wrappers.lambdaQuery();

        if (StringUtils.hasText(packageName)) {
            wrapper.like(TenantPackage::getPackageName, packageName);
        }
        if (packageLevel != null) {
            wrapper.eq(TenantPackage::getPackageLevel, packageLevel);
        }
        if (enabled != null) {
            wrapper.eq(TenantPackage::getEnabled, enabled);
        }
        wrapper.eq(TenantPackage::getDeleted, false);
        wrapper.orderByAsc(TenantPackage::getSortOrder);

        return tenantPackageMapper.selectPage(new Page<>(page, size), wrapper);
    }
}

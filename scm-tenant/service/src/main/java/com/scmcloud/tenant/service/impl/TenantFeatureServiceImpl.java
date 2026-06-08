package com.scmcloud.tenant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import com.scmcloud.tenant.domain.entity.TenantFeature;
import com.scmcloud.tenant.mapper.TenantFeatureMapper;
import com.scmcloud.tenant.service.ITenantFeatureService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class TenantFeatureServiceImpl extends ServiceImpl<TenantFeatureMapper, TenantFeature> implements ITenantFeatureService {

    public TenantFeature createFeature(TenantFeature entity) {
        log.info("鍒涘缓绉熸埛鍔熻兘: tenantId={}, featureCode={}", entity.getTenantId(), entity.getFeatureCode());
        entity.setId(UUID.randomUUID().toString());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        if (entity.getEnabled() == null) {
            entity.setEnabled(true);
        }
        save(entity);
        log.info("绉熸埛鍔熻兘鍒涘缓鎴愬姛: id={}", entity.getId());
        return entity;
    }

    public TenantFeature getById(String id) {
        return lambdaQuery()
                .eq(TenantFeature::getId, id)
                .one();
    }

    public TenantFeature updateFeature(TenantFeature entity) {
        log.info("鏇存柊绉熸埛鍔熻兘: id={}", entity.getId());
        entity.setUpdateTime(LocalDateTime.now());
        updateById(entity);
        return entity;
    }

    public boolean deleteById(String id) {
        log.info("鍒犻櫎绉熸埛鍔熻兘: id={}", id);
        return removeById(id);
    }

    public boolean isFeatureEnabled(String tenantId, String featureCode) {
        log.debug("妫€鏌ュ姛鑳芥槸鍚﹀惎锟?tenantId={}, featureCode={}", tenantId, featureCode);

        TenantFeature feature = lambdaQuery()
                .eq(TenantFeature::getTenantId, tenantId)
                .eq(TenantFeature::getFeatureCode, featureCode)
                .one();

        if (feature == null) {
            log.debug("鍔熻兘涓嶅瓨锟?tenantId={}, featureCode={}", tenantId, featureCode);
            return false;
        }

        boolean enabled = Boolean.TRUE.equals(feature.getEnabled());
        log.debug("鍔熻兘妫€鏌ョ粨锟?tenantId={}, featureCode={}, enabled={}", tenantId, featureCode, enabled);
        return enabled;
    }

    public List<TenantFeature> listByTenantId(String tenantId) {
        return lambdaQuery()
                .eq(TenantFeature::getTenantId, tenantId)
                .orderByAsc(TenantFeature::getFeatureCode)
                .list();
    }

    public Page<TenantFeature> pageQuery(int page, int size, String tenantId, String featureCode, Boolean enabled) {
        log.debug("鍒嗛〉鏌ヨ绉熸埛鍔熻兘: page={}, size={}, tenantId={}, featureCode={}, enabled={}",
                page, size, tenantId, featureCode, enabled);

        LambdaQueryWrapper<TenantFeature> wrapper = Wrappers.lambdaQuery();

        if (tenantId != null) {
            wrapper.eq(TenantFeature::getTenantId, tenantId);
        }
        if (featureCode != null) {
            wrapper.eq(TenantFeature::getFeatureCode, featureCode);
        }
        if (enabled != null) {
            wrapper.eq(TenantFeature::getEnabled, enabled);
        }
        wrapper.orderByAsc(TenantFeature::getFeatureCode);

        Page<TenantFeature> pageParam = new Page<>(page, size);
        return page(pageParam, wrapper);
    }
}

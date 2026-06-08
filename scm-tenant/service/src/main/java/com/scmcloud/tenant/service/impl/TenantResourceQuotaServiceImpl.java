package com.scmcloud.tenant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import com.scmcloud.tenant.domain.entity.TenantResourceQuota;
import com.scmcloud.tenant.mapper.TenantResourceQuotaMapper;
import com.scmcloud.tenant.service.ITenantResourceQuotaService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class TenantResourceQuotaServiceImpl extends ServiceImpl<TenantResourceQuotaMapper, TenantResourceQuota> implements ITenantResourceQuotaService {

    public TenantResourceQuota createQuota(TenantResourceQuota entity) {
        log.info("鍒涘缓绉熸埛璧勬簮閰嶉: tenantId={}", entity.getTenantId());
        entity.setId(UUID.randomUUID().toString());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        save(entity);
        log.info("绉熸埛璧勬簮閰嶉鍒涘缓鎴愬姛: id={}", entity.getId());
        return entity;
    }

    public TenantResourceQuota getById(String id) {
        return lambdaQuery()
                .eq(TenantResourceQuota::getId, id)
                .one();
    }

    public TenantResourceQuota updateQuota(TenantResourceQuota entity) {
        log.info("鏇存柊绉熸埛璧勬簮閰嶉: id={}", entity.getId());
        entity.setUpdateTime(LocalDateTime.now());
        updateById(entity);
        return entity;
    }

    public boolean deleteById(String id) {
        log.info("鍒犻櫎绉熸埛璧勬簮閰嶉: id={}", id);
        return removeById(id);
    }

    public boolean checkQuota(String tenantId, String resourceType) {
        log.debug("妫€鏌ョ鎴烽厤锟?tenantId={}, resourceType={}", tenantId, resourceType);

        TenantResourceQuota quota = lambdaQuery()
                .eq(TenantResourceQuota::getTenantId, tenantId)
                .one();

        if (quota == null) {
            log.warn("绉熸埛閰嶉涓嶅瓨锟?tenantId={}", tenantId);
            return false;
        }

        return switch (resourceType) {
            case "USER" -> quota.getCurrentUsers() < quota.getMaxUsers();
            case "WAREHOUSE" -> quota.getCurrentWarehouses() < quota.getMaxWarehouses();
            case "SKU" -> quota.getCurrentSkus() < quota.getMaxSkus();
            case "ORDER" -> quota.getCurrentOrdersToday() < quota.getMaxOrdersPerDay();
            case "API" -> quota.getCurrentApiCallsToday() < quota.getMaxApiCallsPerDay();
            default -> {
                log.warn("鏈煡璧勬簮绫诲瀷: {}", resourceType);
                yield false;
            }
        };
    }

    public List<TenantResourceQuota> listByTenantId(String tenantId) {
        return lambdaQuery()
                .eq(TenantResourceQuota::getTenantId, tenantId)
                .list();
    }

    public Page<TenantResourceQuota> pageQuery(int page, int size, String tenantId) {
        log.debug("鍒嗛〉鏌ヨ绉熸埛璧勬簮閰嶉: page={}, size={}, tenantId={}", page, size, tenantId);

        LambdaQueryWrapper<TenantResourceQuota> wrapper = Wrappers.lambdaQuery();

        if (tenantId != null) {
            wrapper.eq(TenantResourceQuota::getTenantId, tenantId);
        }
        wrapper.orderByDesc(TenantResourceQuota::getCreateTime);

        Page<TenantResourceQuota> pageParam = new Page<>(page, size);
        return page(pageParam, wrapper);
    }
}

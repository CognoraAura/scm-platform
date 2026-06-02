package com.scmcloud.tenant.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.tenant.domain.entity.TenantResourceQuota;
import com.scmcloud.tenant.mapper.TenantResourceQuotaMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantResourceQuotaCommandService {

    private final TenantResourceQuotaMapper tenantResourceQuotaMapper;

    @Master(reason = "创建租户资源配额")
    @Transactional(rollbackFor = Exception.class)
    public TenantResourceQuota createQuota(TenantResourceQuota entity) {
        log.info("创建租户资源配额: tenantId={}", entity.getTenantId());
        entity.setId(UUID.randomUUID().toString());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        tenantResourceQuotaMapper.insert(entity);
        log.info("租户资源配额创建成功: id={}", entity.getId());
        return entity;
    }

    @Master(reason = "更新租户资源配额")
    @Transactional(rollbackFor = Exception.class)
    public TenantResourceQuota updateQuota(TenantResourceQuota entity) {
        log.info("更新租户资源配额: id={}", entity.getId());
        entity.setUpdateTime(LocalDateTime.now());
        tenantResourceQuotaMapper.updateById(entity);
        return entity;
    }

    @Master(reason = "删除租户资源配额")
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(String id) {
        log.info("删除租户资源配额: id={}", id);
        return tenantResourceQuotaMapper.deleteById(id) > 0;
    }
}

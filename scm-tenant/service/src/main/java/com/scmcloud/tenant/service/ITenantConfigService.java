package com.scmcloud.tenant.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.tenant.domain.entity.TenantConfig;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 租户配置�服务�
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
public interface ITenantConfigService extends IService<TenantConfig> {

    TenantConfig createConfig(TenantConfig entity);

    TenantConfig getById(String id);

    TenantConfig updateConfig(TenantConfig entity);

    boolean deleteById(String id);

    TenantConfig getConfigByTenantAndKey(String tenantId, String configKey);

    Page<TenantConfig> pageQuery(int page, int size, String tenantId, String configCategory, String configKey);
}

package com.scmcloud.tenant.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.tenant.domain.entity.TenantResourceQuota;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 绉熸埛璧勬簮閰嶉锟芥湇鍔★拷
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
public interface ITenantResourceQuotaService extends IService<TenantResourceQuota> {

    TenantResourceQuota createQuota(TenantResourceQuota entity);

    TenantResourceQuota getById(String id);

    TenantResourceQuota updateQuota(TenantResourceQuota entity);

    boolean deleteById(String id);

    boolean checkQuota(String tenantId, String resourceType);

    List<TenantResourceQuota> listByTenantId(String tenantId);

    Page<TenantResourceQuota> pageQuery(int page, int size, String tenantId);
}

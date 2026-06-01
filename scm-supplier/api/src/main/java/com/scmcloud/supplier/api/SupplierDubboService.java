package com.scmcloud.supplier.api;

import java.util.List;

import com.scmcloud.supplier.api.dto.EvaluationResult;
import com.scmcloud.supplier.api.dto.SupplierVO;
import com.scmcloud.supplier.api.request.EvaluationRequest;

/**
 * 供应商服�?Dubbo 接口
 *
 * <p>提供供应商查询、评估等核心功能，供其他微服务通过 RPC 调用�?
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
public interface SupplierDubboService {

    /**
     * 根据 ID 查询供应�?
     *
     * @param id 供应�?ID
     * @return 供应商信息，不存在时返回 null
     */
    SupplierVO getSupplierById(Long id);

    /**
     * 查询所有启用的供应�?
     *
     * @return 供应商列�?
     */
    List<SupplierVO> listActiveSuppliers();

    /**
     * 评估供应�?
     *
     * @param supplierId 供应�?ID
     * @param request 评估请求
     * @return 评估结果
     */
    EvaluationResult evaluateSupplier(Long supplierId, EvaluationRequest request);
}

package com.scmcloud.purchase.api;

import com.scmcloud.purchase.api.dto.PurchaseOrderVO;
import com.scmcloud.purchase.api.request.PurchaseRequestDTO;

/**
 * 采购服务 Dubbo 接口
 *
 * <p>提供采购申请、采购单查询、收货确认等核心功能，供其他微服务通过 RPC 调用�
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
public interface PurchaseDubboService {

    /**
     * 创建采购申请
     *
     * @param dto 采购申请信息
     * @return 采购单信�
     */
    PurchaseOrderVO createPurchaseRequest(PurchaseRequestDTO dto);

    /**
     * 根据 ID 查询采购�
     *
     * @param id 采购�ID
     * @return 采购单信息，不存在时返回 null
     */
    PurchaseOrderVO getPurchaseOrderById(Long id);

    /**
     * 确认收货
     *
     * @param receiptId 收货�ID
     */
    void confirmReceipt(Long receiptId);
}

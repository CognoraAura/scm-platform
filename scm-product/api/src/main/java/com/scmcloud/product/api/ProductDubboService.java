package com.scmcloud.product.api;

import com.scmcloud.product.api.dto.ProductSearchResult;
import com.scmcloud.product.api.dto.ProductVO;
import com.scmcloud.product.api.dto.SkuVO;
import java.util.List;

/**
 * 商品服务 Dubbo 接口
 *
 * <p>提供商品、SKU 查询等核心功能，供其他微服务通过 RPC 调用�?
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
public interface ProductDubboService {

    /**
     * 根据 ID 查询商品
     *
     * @param id 商品 ID
     * @return 商品信息，不存在时返�?null
     */
    ProductVO getProductById(Long id);

    /**
     * 根据 SKU ID 查询 SKU 信息
     *
     * @param skuId SKU ID
     * @return SKU 信息，不存在时返�?null
     */
    SkuVO getSkuById(Long skuId);

    /**
     * 批量查询商品
     *
     * @param ids 商品 ID 列表
     * @return 商品列表
     */
    List<ProductVO> batchGetProducts(List<Long> ids);

    /**
     * 搜索商品
     *
     * @param keyword 搜索关键�?
     * @param page 页码（从 1 开始）
     * @param size 每页数量
     * @return 搜索结果
     */
    ProductSearchResult searchProducts(String keyword, int page, int size);
}

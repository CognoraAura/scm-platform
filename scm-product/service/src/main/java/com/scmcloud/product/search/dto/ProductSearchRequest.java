package com.scmcloud.product.search.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品搜索请求 DTO
 *
 * <p>支持多条件组合搜�
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Data
public class ProductSearchRequest {

    private String keyword;

    private String categoryId;

    private String brandId;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private String sortBy;

    private String sortOrder;

    private Integer page = 1;

    private Integer size = 20;
}
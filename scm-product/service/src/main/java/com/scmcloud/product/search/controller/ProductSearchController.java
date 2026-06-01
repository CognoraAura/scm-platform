package com.scmcloud.product.search.controller;

import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.product.search.dto.ProductSearchRequest;
import com.scmcloud.product.search.dto.ProductSearchResponse;
import com.scmcloud.product.search.service.ProductSearchService;

/**
 * 商品搜索 Controller
 *
 * <p>提供商品搜索相关 API
 *
 * <p>功能列表�?
 * - 综合搜索（支持关键词、分类、品牌、价格区间、排序）
 * - 热门商品列表
 * - 最新商品列�?
 * - 按分类查询商�?
 * - 按品牌查询商�?
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/products/search")
@RequiredArgsConstructor
public class ProductSearchController {
    private final ProductSearchService productSearchService;

    /**
     * 综合搜索
     *
     * <p>支持多条件组合搜索：
     * - 关键词搜索（spuName, description, seoKeywords�?
     * - 分类筛�?
     * - 品牌筛�?
     * - 价格区间
     * - 多种排序（销量、价格、时间）
     */
    @PostMapping
    public ApiResponse<Page<ProductSearchResponse>> search(
            @RequestBody ProductSearchRequest request) {

        log.info("📥 [API] 收到商品搜索请求: {}", request);

        Page<ProductSearchResponse> result = productSearchService.search(request);

        log.info("📤 [API] 返回商品搜索结果: 总数={}, 页码={}/{}",
                result.getTotalElements(), result.getNumber() + 1, result.getTotalPages());

        return ApiResponse.success(result);
    }

    /**
     * 热门商品
     *
     * <p>按销量排序的热门商品列表（缓�?5 分钟�?
     */
    @GetMapping("/hot")
    public ApiResponse<Page<ProductSearchResponse>> getHotProducts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

        log.info("📥 [API] 查询热门商品: page={}, size={}", page, size);

        Page<ProductSearchResponse> result = productSearchService.getHotProducts(page, size);

        log.info("📤 [API] 返回热门商品: 总数={}", result.getTotalElements());

        return ApiResponse.success(result);
    }

    /**
     * 最新商�?
     *
     * <p>按发布时间排序的最新商品列表（缓存 5 分钟�?
     */
    @GetMapping("/latest")
    public ApiResponse<Page<ProductSearchResponse>> getLatestProducts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

        log.info("📥 [API] 查询最新商�? page={}, size={}", page, size);

        Page<ProductSearchResponse> result = productSearchService.getLatestProducts(page, size);

        log.info("📤 [API] 返回最新商�? 总数={}", result.getTotalElements());

        return ApiResponse.success(result);
    }

    /**
     * 按分类查询商�?
     *
     * <p>查询指定分类下的所有商品，按销量排�?
     */
    @GetMapping("/category/{categoryId}")
    public ApiResponse<Page<ProductSearchResponse>> findByCategory(
            @PathVariable String categoryId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

        log.info("📥 [API] 按分类查询商�? categoryId={}, page={}, size={}", categoryId, page, size);

        Page<ProductSearchResponse> result = productSearchService.findByCategory(categoryId, page, size);

        log.info("📤 [API] 返回分类商品: 总数={}", result.getTotalElements());

        return ApiResponse.success(result);
    }

    /**
     * 按品牌查询商�?
     *
     * <p>查询指定品牌下的所有商品，按销量排�?
     */
    @GetMapping("/brand/{brandId}")
    public ApiResponse<Page<ProductSearchResponse>> findByBrand(
            @PathVariable String brandId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

        log.info("📥 [API] 按品牌查询商�? brandId={}, page={}, size={}", brandId, page, size);

        Page<ProductSearchResponse> result = productSearchService.findByBrand(brandId, page, size);

        log.info("📤 [API] 返回品牌商品: 总数={}", result.getTotalElements());

        return ApiResponse.success(result);
    }

    /**
     * 快速搜索（GET 方式，用于简单场景）
     *
     * <p>适用于简单的关键词搜索，不需要复杂筛�?
     */
    @GetMapping
    public ApiResponse<Page<ProductSearchResponse>> quickSearch(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "sales") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {

        log.info("📥 [API] 快速搜�? keyword={}, page={}, size={}, sortBy={}, sortOrder={}",
                keyword, page, size, sortBy, sortOrder);

        ProductSearchRequest request = new ProductSearchRequest();
        request.setKeyword(keyword);
        request.setPage(page);
        request.setSize(size);
        request.setSortBy(sortBy);
        request.setSortOrder(sortOrder);

        Page<ProductSearchResponse> result = productSearchService.search(request);

        log.info("📤 [API] 返回快速搜索结�? 总数={}", result.getTotalElements());

        return ApiResponse.success(result);
    }
}
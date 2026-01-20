package scm.product.search.controller;

import com.frog.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import scm.product.search.dto.ProductSearchRequest;
import scm.product.search.dto.ProductSearchResponse;
import scm.product.search.service.ProductSearchService;

/**
 * 商品搜索 Controller
 *
 * <p>提供商品搜索相关 API
 *
 * <p>功能列表：
 * - 综合搜索（支持关键词、分类、品牌、价格区间、排序）
 * - 热门商品列表
 * - 最新商品列表
 * - 按分类查询商品
 * - 按品牌查询商品
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/products/search")
@RequiredArgsConstructor
@Tag(name = "商品搜索", description = "商品搜索 API - 支持全文搜索、分类筛选、品牌筛选、价格区间查询")
public class ProductSearchController {
    private final ProductSearchService productSearchService;

    /**
     * 综合搜索
     *
     * <p>支持多条件组合搜索：
     * - 关键词搜索（spuName, description, seoKeywords）
     * - 分类筛选
     * - 品牌筛选
     * - 价格区间
     * - 多种排序（销量、价格、时间）
     */
    @PostMapping
    @Operation(
            summary = "综合搜索",
            description = "支持关键词、分类、品牌、价格区间等多条件组合搜索，支持多种排序方式"
    )
    public ApiResponse<Page<ProductSearchResponse>> search(
            @RequestBody @Parameter(description = "搜索条件") ProductSearchRequest request) {

        log.info("📥 [API] 收到商品搜索请求: {}", request);

        Page<ProductSearchResponse> result = productSearchService.search(request);

        log.info("📤 [API] 返回商品搜索结果: 总数={}, 页码={}/{}",
                result.getTotalElements(), result.getNumber() + 1, result.getTotalPages());

        return ApiResponse.success(result);
    }

    /**
     * 热门商品
     *
     * <p>按销量排序的热门商品列表（缓存 5 分钟）
     */
    @GetMapping("/hot")
    @Operation(
            summary = "热门商品",
            description = "按销量排序的热门商品列表，数据缓存 5 分钟"
    )
    public ApiResponse<Page<ProductSearchResponse>> getHotProducts(
            @Parameter(description = "页码（从 1 开始）", example = "1")
            @RequestParam(defaultValue = "1") Integer page,

            @Parameter(description = "每页数量（最大 100）", example = "20")
            @RequestParam(defaultValue = "20") Integer size) {

        log.info("📥 [API] 查询热门商品: page={}, size={}", page, size);

        Page<ProductSearchResponse> result = productSearchService.getHotProducts(page, size);

        log.info("📤 [API] 返回热门商品: 总数={}", result.getTotalElements());

        return ApiResponse.success(result);
    }

    /**
     * 最新商品
     *
     * <p>按发布时间排序的最新商品列表（缓存 5 分钟）
     */
    @GetMapping("/latest")
    @Operation(
            summary = "最新商品",
            description = "按发布时间排序的最新商品列表，数据缓存 5 分钟"
    )
    public ApiResponse<Page<ProductSearchResponse>> getLatestProducts(
            @Parameter(description = "页码（从 1 开始）", example = "1")
            @RequestParam(defaultValue = "1") Integer page,

            @Parameter(description = "每页数量（最大 100）", example = "20")
            @RequestParam(defaultValue = "20") Integer size) {

        log.info("📥 [API] 查询最新商品: page={}, size={}", page, size);

        Page<ProductSearchResponse> result = productSearchService.getLatestProducts(page, size);

        log.info("📤 [API] 返回最新商品: 总数={}", result.getTotalElements());

        return ApiResponse.success(result);
    }

    /**
     * 按分类查询商品
     *
     * <p>查询指定分类下的所有商品，按销量排序
     */
    @GetMapping("/category/{categoryId}")
    @Operation(
            summary = "按分类查询",
            description = "查询指定分类下的所有商品，按销量排序"
    )
    public ApiResponse<Page<ProductSearchResponse>> findByCategory(
            @Parameter(description = "分类 ID", example = "cat_001")
            @PathVariable String categoryId,

            @Parameter(description = "页码（从 1 开始）", example = "1")
            @RequestParam(defaultValue = "1") Integer page,

            @Parameter(description = "每页数量（最大 100）", example = "20")
            @RequestParam(defaultValue = "20") Integer size) {

        log.info("📥 [API] 按分类查询商品: categoryId={}, page={}, size={}", categoryId, page, size);

        Page<ProductSearchResponse> result = productSearchService.findByCategory(categoryId, page, size);

        log.info("📤 [API] 返回分类商品: 总数={}", result.getTotalElements());

        return ApiResponse.success(result);
    }

    /**
     * 按品牌查询商品
     *
     * <p>查询指定品牌下的所有商品，按销量排序
     */
    @GetMapping("/brand/{brandId}")
    @Operation(
            summary = "按品牌查询",
            description = "查询指定品牌下的所有商品，按销量排序"
    )
    public ApiResponse<Page<ProductSearchResponse>> findByBrand(
            @Parameter(description = "品牌 ID", example = "brand_001")
            @PathVariable String brandId,

            @Parameter(description = "页码（从 1 开始）", example = "1")
            @RequestParam(defaultValue = "1") Integer page,

            @Parameter(description = "每页数量（最大 100）", example = "20")
            @RequestParam(defaultValue = "20") Integer size) {

        log.info("📥 [API] 按品牌查询商品: brandId={}, page={}, size={}", brandId, page, size);

        Page<ProductSearchResponse> result = productSearchService.findByBrand(brandId, page, size);

        log.info("📤 [API] 返回品牌商品: 总数={}", result.getTotalElements());

        return ApiResponse.success(result);
    }

    /**
     * 快速搜索（GET 方式，用于简单场景）
     *
     * <p>适用于简单的关键词搜索，不需要复杂筛选
     */
    @GetMapping
    @Operation(
            summary = "快速搜索",
            description = "简单的关键词搜索，适用于搜索框快速查询"
    )
    public ApiResponse<Page<ProductSearchResponse>> quickSearch(
            @Parameter(description = "搜索关键词", example = "iPhone 15 Pro")
            @RequestParam(required = false) String keyword,

            @Parameter(description = "页码（从 1 开始）", example = "1")
            @RequestParam(defaultValue = "1") Integer page,

            @Parameter(description = "每页数量（最大 100）", example = "20")
            @RequestParam(defaultValue = "20") Integer size,

            @Parameter(description = "排序字段（sales-销量, price-价格, time-时间）", example = "sales")
            @RequestParam(defaultValue = "sales") String sortBy,

            @Parameter(description = "排序方向（asc-升序, desc-降序）", example = "desc")
            @RequestParam(defaultValue = "desc") String sortOrder) {

        log.info("📥 [API] 快速搜索: keyword={}, page={}, size={}, sortBy={}, sortOrder={}",
                keyword, page, size, sortBy, sortOrder);

        ProductSearchRequest request = new ProductSearchRequest();
        request.setKeyword(keyword);
        request.setPage(page);
        request.setSize(size);
        request.setSortBy(sortBy);
        request.setSortOrder(sortOrder);

        Page<ProductSearchResponse> result = productSearchService.search(request);

        log.info("📤 [API] 返回快速搜索结果: 总数={}", result.getTotalElements());

        return ApiResponse.success(result);
    }
}
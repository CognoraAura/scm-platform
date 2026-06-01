package com.scmcloud.product.search.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import com.scmcloud.product.search.document.ProductDocument;

import java.math.BigDecimal;

/**
 * 商品搜索 Repository
 *
 * <p>基于 Spring Data Elasticsearch 的商品搜索接�?
 *
 * <p>支持功能�?
 * - 全文搜索（spuName, description, seoKeywords�?
 * - 分类过滤
 * - 品牌过滤
 * - 价格区间过滤
 * - 状态过滤（仅返回上架商品）
 * - 多种排序（销量、价格、发布时间、更新时间）
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, String> {

    /**
     * �?SPU 名称搜索（上架商品）
     *
     * @param spuName  SPU 名称（支持模糊匹配）
     * @param status   商品状态（1-上架�?
     * @param pageable 分页参数
     * @return 商品列表
     */
    Page<ProductDocument> findBySpuNameAndStatus(String spuName, Integer status, Pageable pageable);

    /**
     * 按分类搜索（上架商品�?
     *
     * @param categoryId 分类 ID
     * @param status     商品状态（1-上架�?
     * @param pageable   分页参数
     * @return 商品列表
     */
    Page<ProductDocument> findByCategoryIdAndStatus(String categoryId, Integer status, Pageable pageable);

    /**
     * 按品牌搜索（上架商品�?
     *
     * @param brandId  品牌 ID
     * @param status   商品状态（1-上架�?
     * @param pageable 分页参数
     * @return 商品列表
     */
    Page<ProductDocument> findByBrandIdAndStatus(String brandId, Integer status, Pageable pageable);

    /**
     * 按价格区间搜索（上架商品�?
     *
     * @param minPrice 最低价�?
     * @param maxPrice 最高价�?
     * @param status   商品状态（1-上架�?
     * @param pageable 分页参数
     * @return 商品列表
     */
    Page<ProductDocument> findByMinPriceGreaterThanEqualAndMaxPriceLessThanEqualAndStatus(
            BigDecimal minPrice, BigDecimal maxPrice, Integer status, Pageable pageable);

    /**
     * 全文搜索（spuName + description + seoKeywords�?
     *
     * <p>使用 Elasticsearch Query DSL 进行多字段搜�?
     *
     * @param keyword  搜索关键�?
     * @param status   商品状态（1-上架�?
     * @param pageable 分页参数
     * @return 商品列表
     */
    @Query("{\"bool\": {\"must\": [{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"spuName^3\", \"description^2\", " +
            "\"seoKeywords\"], \"type\": \"best_fields\"}}], \"filter\": [{\"term\": {\"status\": \"?1\"}}]}}")
    Page<ProductDocument> fullTextSearch(String keyword, Integer status, Pageable pageable);

    /**
     * 高级搜索（支持分类、品牌、价格区间、关键词组合过滤�?
     *
     * @param keyword    搜索关键词（可选）
     * @param categoryId 分类 ID（可选）
     * @param brandId    品牌 ID（可选）
     * @param minPrice   最低价格（可选）
     * @param maxPrice   最高价格（可选）
     * @param status     商品状态（1-上架�?
     * @param pageable   分页参数
     * @return 商品列表
     */
    @Query("""
            {
              "bool": {
                "must": [
                  {"multi_match": {"query": "?0", "fields": ["spuName^3", "description^2", "seoKeywords"], "type": "best_fields"}}
                ],
                "filter": [
                  {"term": {"categoryId": "?1"}},
                  {"term": {"brandId": "?2"}},
                  {"range": {"minPrice": {"gte": "?3"}}},
                  {"range": {"maxPrice": {"lte": "?4"}}},
                  {"term": {"status": "?5"}}
                ]
              }
            }
            """)
    Page<ProductDocument> advancedSearch(
            String keyword,
            String categoryId,
            String brandId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer status,
            Pageable pageable
    );

    /**
     * 热门商品（按销量排序）
     *
     * @param status   商品状态（1-上架�?
     * @param pageable 分页参数
     * @return 商品列表
     */
    Page<ProductDocument> findByStatusOrderByTotalSalesDesc(Integer status, Pageable pageable);

    /**
     * 最新商品（按发布时间排序）
     *
     * @param status   商品状态（1-上架�?
     * @param pageable 分页参数
     * @return 商品列表
     */
    Page<ProductDocument> findByStatusOrderByPublishedAtDesc(Integer status, Pageable pageable);
}
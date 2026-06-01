package com.scmcloud.product.search.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品搜索文档
 *
 * <p>用于 Elasticsearch 全文搜索的商品文�?
 *
 * <p>索引设计�?
 * - 使用 IK 分词器进行中文分�?
 * - ik_max_word: 最细粒度分词，用于搜索字段
 * - ik_smart: 粗粒度分词，用于聚合字段
 * - 5 分片�? 副本，支持水平扩�?
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Data
@Document(indexName = "scm_product")
@Setting(shards = 5)
public class ProductDocument {

    /**
     * SPU ID（主键）
     */
    @Id
    private String id;

    /**
     * SPU 编码
     */
    @Field(type = FieldType.Keyword)
    private String spuCode;

    /**
     * SPU 名称（使�?IK 分词器，支持全文搜索�?
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_max_word")
    private String spuName;

    /**
     * 分类 ID
     */
    @Field(type = FieldType.Keyword)
    private String categoryId;

    /**
     * 分类名称（用于显示）
     */
    @Field(type = FieldType.Keyword)
    private String categoryName;

    /**
     * 品牌 ID
     */
    @Field(type = FieldType.Keyword)
    private String brandId;

    /**
     * 品牌名称（用于显示和过滤�?
     */
    @Field(type = FieldType.Keyword)
    private String brandName;

    /**
     * 商品描述（支持全文搜索）
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String description;

    /**
     * 主图 URL
     */
    @Field(type = FieldType.Keyword, index = false)
    private String mainImage;

    /**
     * 最低价�?
     */
    @Field(type = FieldType.Double)
    private BigDecimal minPrice;

    /**
     * 最高价�?
     */
    @Field(type = FieldType.Double)
    private BigDecimal maxPrice;

    /**
     * 总库�?
     */
    @Field(type = FieldType.Integer)
    private Integer totalStock;

    /**
     * 总销量（用于排序�?
     */
    @Field(type = FieldType.Integer)
    private Integer totalSales;

    /**
     * 排序权重
     */
    @Field(type = FieldType.Integer)
    private Integer sortOrder;

    /**
     * SEO 标题
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String seoTitle;

    /**
     * SEO 关键�?
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String seoKeywords;

    /**
     * SEO 描述
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String seoDescription;

    /**
     * 状�? 0-草稿, 1-上架, 2-下架, 3-删除
     */
    @Field(type = FieldType.Integer)
    private Integer status;

    /**
     * 发布时间
     */
    @Field(type = FieldType.Date)
    private LocalDateTime publishedAt;

    /**
     * 创建时间
     */
    @Field(type = FieldType.Date)
    private LocalDateTime createTime;

    /**
     * 更新时间（用于排序）
     */
    @Field(type = FieldType.Date)
    private LocalDateTime updateTime;
}
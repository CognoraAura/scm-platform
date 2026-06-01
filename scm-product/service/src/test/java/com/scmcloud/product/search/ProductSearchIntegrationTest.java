package com.scmcloud.product.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import com.scmcloud.product.search.document.ProductDocument;
import com.scmcloud.product.search.dto.ProductSearchRequest;
import com.scmcloud.product.search.dto.ProductSearchResponse;
import com.scmcloud.product.search.repository.ProductSearchRepository;
import com.scmcloud.product.search.service.ProductSearchService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 商品搜索集成测试
 *
 * <p>测试场景�?
 * 1. 全文搜索 - 关键词匹�?
 * 2. 分类筛�?- 精确匹配
 * 3. 品牌筛�?- 精确匹配
 * 4. 价格区间查询 - 范围过滤
 * 5. 多条件组合搜�?
 * 6. 排序功能 - 销�?价格/时间
 * 7. 分页功能
 * 8. 热门商品/最新商�?
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@RequiredArgsConstructor
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("商品搜索集成测试")
public class ProductSearchIntegrationTest {

    private final ProductSearchService productSearchService;

    private final ProductSearchRepository productSearchRepository;

    private static final String TEST_CATEGORY_ID = "cat_test_001";
    private static final String TEST_BRAND_ID = "brand_test_001";

    /**
     * 准备测试数据
     */
    @BeforeEach
    public void setup() {
        log.info("========================================");
        log.info("准备商品搜索测试数据");
        log.info("========================================");

        // 清理测试数据
        productSearchRepository.deleteAll();

        // 插入测试商品
        createTestProduct("prod_001", "iPhone 15 Pro Max", TEST_CATEGORY_ID, TEST_BRAND_ID,
                new BigDecimal("9999.00"), new BigDecimal("13999.00"), 1000, 5000, 100);

        createTestProduct("prod_002", "iPhone 15 Pro", TEST_CATEGORY_ID, TEST_BRAND_ID,
                new BigDecimal("7999.00"), new BigDecimal("9999.00"), 800, 4000, 90);

        createTestProduct("prod_003", "MacBook Pro 16", TEST_CATEGORY_ID, "brand_test_002",
                new BigDecimal("19999.00"), new BigDecimal("29999.00"), 500, 2000, 80);

        createTestProduct("prod_004", "AirPods Pro", "cat_test_002", TEST_BRAND_ID,
                new BigDecimal("1999.00"), new BigDecimal("1999.00"), 2000, 10000, 70);

        createTestProduct("prod_005", "iPad Pro", TEST_CATEGORY_ID, TEST_BRAND_ID,
                new BigDecimal("6999.00"), new BigDecimal("15999.00"), 600, 3000, 60);

        // 等待 Elasticsearch 索引刷新
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("�?测试数据准备完成: 共插�?5 个商�?);
    }

    /**
     * 场景 1: 全文搜索 - 关键词匹�?
     */
    @Test
    @Order(1)
    @DisplayName("场景1: 全文搜索 - iPhone 关键�?)
    public void testFullTextSearch_Keyword() {
        log.info("========================================");
        log.info("测试场景 1: 全文搜索 - iPhone 关键�?);
        log.info("========================================");

        ProductSearchRequest request = new ProductSearchRequest();
        request.setKeyword("iPhone");
        request.setPage(1);
        request.setSize(20);

        Page<ProductSearchResponse> result = productSearchService.search(request);

        assertNotNull(result, "搜索结果不应为空");
        assertTrue(result.getTotalElements() >= 2, "应该至少找到 2 �?iPhone 商品");

        log.info("�?全文搜索验证通过: 找到 {} 个商�?, result.getTotalElements());

        log.info("========================================");
        log.info("场景 1 测试通过 �?);
        log.info("========================================");
    }

    /**
     * 场景 2: 分类筛�?
     */
    @Test
    @Order(2)
    @DisplayName("场景2: 分类筛�?)
    public void testCategoryFilter() {
        log.info("========================================");
        log.info("测试场景 2: 分类筛�?);
        log.info("========================================");

        Page<ProductSearchResponse> result = productSearchService.findByCategory(TEST_CATEGORY_ID, 1, 20);

        assertNotNull(result, "搜索结果不应为空");
        assertEquals(4, result.getTotalElements(), "应该找到 4 个商品（同一分类�?);

        // 验证所有商品都属于指定分类
        result.getContent().forEach(product -> {
            assertEquals(TEST_CATEGORY_ID, product.getCategoryId(), "商品分类应该匹配");
        });

        log.info("�?分类筛选验证通过: 找到 {} 个商�?, result.getTotalElements());

        log.info("========================================");
        log.info("场景 2 测试通过 �?);
        log.info("========================================");
    }

    /**
     * 场景 3: 品牌筛�?
     */
    @Test
    @Order(3)
    @DisplayName("场景3: 品牌筛�?)
    public void testBrandFilter() {
        log.info("========================================");
        log.info("测试场景 3: 品牌筛�?);
        log.info("========================================");

        Page<ProductSearchResponse> result = productSearchService.findByBrand(TEST_BRAND_ID, 1, 20);

        assertNotNull(result, "搜索结果不应为空");
        assertEquals(4, result.getTotalElements(), "应该找到 4 个商品（同一品牌�?);

        // 验证所有商品都属于指定品牌
        result.getContent().forEach(product -> {
            assertEquals(TEST_BRAND_ID, product.getBrandId(), "商品品牌应该匹配");
        });

        log.info("�?品牌筛选验证通过: 找到 {} 个商�?, result.getTotalElements());

        log.info("========================================");
        log.info("场景 3 测试通过 �?);
        log.info("========================================");
    }

    /**
     * 场景 4: 价格区间查询
     */
    @Test
    @Order(4)
    @DisplayName("场景4: 价格区间查询")
    public void testPriceRangeFilter() {
        log.info("========================================");
        log.info("测试场景 4: 价格区间查询�?000-10000�?);
        log.info("========================================");

        ProductSearchRequest request = new ProductSearchRequest();
        request.setMinPrice(new BigDecimal("5000.00"));
        request.setMaxPrice(new BigDecimal("10000.00"));
        request.setPage(1);
        request.setSize(20);

        Page<ProductSearchResponse> result = productSearchService.search(request);

        assertNotNull(result, "搜索结果不应为空");
        assertTrue(result.getTotalElements() >= 1, "应该至少找到 1 个商�?);

        log.info("�?价格区间查询验证通过: 找到 {} 个商�?, result.getTotalElements());

        log.info("========================================");
        log.info("场景 4 测试通过 �?);
        log.info("========================================");
    }

    /**
     * 场景 5: 多条件组合搜�?
     */
    @Test
    @Order(5)
    @DisplayName("场景5: 多条件组合搜索（关键�?分类+品牌�?)
    public void testAdvancedSearch() {
        log.info("========================================");
        log.info("测试场景 5: 多条件组合搜�?);
        log.info("========================================");

        ProductSearchRequest request = new ProductSearchRequest();
        request.setKeyword("iPhone");
        request.setCategoryId(TEST_CATEGORY_ID);
        request.setBrandId(TEST_BRAND_ID);
        request.setPage(1);
        request.setSize(20);

        Page<ProductSearchResponse> result = productSearchService.search(request);

        assertNotNull(result, "搜索结果不应为空");
        assertTrue(result.getTotalElements() >= 2, "应该找到至少 2 个商�?);

        log.info("�?多条件组合搜索验证通过: 找到 {} 个商�?, result.getTotalElements());

        log.info("========================================");
        log.info("场景 5 测试通过 �?);
        log.info("========================================");
    }

    /**
     * 场景 6: 排序功能 - 按销量降�?
     */
    @Test
    @Order(6)
    @DisplayName("场景6: 排序功能 - 按销量降�?)
    public void testSortBySales() {
        log.info("========================================");
        log.info("测试场景 6: 排序功能 - 按销量降�?);
        log.info("========================================");

        ProductSearchRequest request = new ProductSearchRequest();
        request.setSortBy("sales");
        request.setSortOrder("desc");
        request.setPage(1);
        request.setSize(20);

        Page<ProductSearchResponse> result = productSearchService.search(request);

        assertNotNull(result, "搜索结果不应为空");
        assertTrue(result.getTotalElements() >= 5, "应该找到 5 个商�?);

        // 验证排序正确性（销量降序）
        ProductSearchResponse first = result.getContent().get(0);
        assertEquals(10000, first.getTotalSales(), "第一个商品销量应该最高（10000�?);

        log.info("�?排序功能验证通过: 第一个商品销�?{}", first.getTotalSales());

        log.info("========================================");
        log.info("场景 6 测试通过 �?);
        log.info("========================================");
    }

    /**
     * 场景 7: 热门商品列表
     */
    @Test
    @Order(7)
    @DisplayName("场景7: 热门商品列表")
    public void testHotProducts() {
        log.info("========================================");
        log.info("测试场景 7: 热门商品列表");
        log.info("========================================");

        Page<ProductSearchResponse> result = productSearchService.getHotProducts(1, 10);

        assertNotNull(result, "热门商品列表不应为空");
        assertTrue(result.getTotalElements() >= 5, "应该找到 5 个商�?);

        log.info("�?热门商品列表验证通过: 找到 {} 个商�?, result.getTotalElements());

        log.info("========================================");
        log.info("场景 7 测试通过 �?);
        log.info("========================================");
    }

    /**
     * 场景 8: 最新商品列�?
     */
    @Test
    @Order(8)
    @DisplayName("场景8: 最新商品列�?)
    public void testLatestProducts() {
        log.info("========================================");
        log.info("测试场景 8: 最新商品列�?);
        log.info("========================================");

        Page<ProductSearchResponse> result = productSearchService.getLatestProducts(1, 10);

        assertNotNull(result, "最新商品列表不应为�?);
        assertTrue(result.getTotalElements() >= 5, "应该找到 5 个商�?);

        log.info("�?最新商品列表验证通过: 找到 {} 个商�?, result.getTotalElements());

        log.info("========================================");
        log.info("场景 8 测试通过 �?);
        log.info("========================================");
    }

    /**
     * 清理测试数据
     */
    @AfterEach
    public void cleanup() {
        log.info("清理商品搜索测试数据...");
        productSearchRepository.deleteAll();
        log.info("�?测试数据清理完成");
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建测试商品
     */
    private void createTestProduct(String id, String spuName, String categoryId, String brandId,
                                     BigDecimal minPrice, BigDecimal maxPrice,
                                     Integer totalStock, Integer totalSales, Integer sortOrder) {
        ProductDocument product = new ProductDocument();
        product.setId(id);
        product.setSpuCode("SPU_" + id);
        product.setSpuName(spuName);
        product.setCategoryId(categoryId);
        product.setCategoryName("测试分类");
        product.setBrandId(brandId);
        product.setBrandName("测试品牌");
        product.setDescription(spuName + " 详细描述");
        product.setMainImage("https://example.com/images/" + id + ".jpg");
        product.setMinPrice(minPrice);
        product.setMaxPrice(maxPrice);
        product.setTotalStock(totalStock);
        product.setTotalSales(totalSales);
        product.setSortOrder(sortOrder);
        product.setStatus(1);  // 上架
        product.setPublishedAt(LocalDateTime.now());
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());

        productSearchRepository.save(product);
    }
}
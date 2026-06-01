package com.scmcloud.product.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import com.scmcloud.product.search.document.ProductDocument;
import com.scmcloud.product.search.repository.ProductSearchRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 商品数据同步消费�?
 *
 * <p>监听 Kafka 中的 PostgreSQL 变更事件（通过 Debezium），实时同步�?Elasticsearch
 *
 * <p>工作流程�?
 * 1. PostgreSQL 中的商品数据发生变更（INSERT/UPDATE/DELETE�?
 * 2. Debezium Connector 捕获 PostgreSQL WAL 日志
 * 3. Debezium 将变更事件发送到 Kafka Topic
 * 4. 本消费者监�?Kafka Topic，接收变更事�?
 * 5. 解析事件，将数据同步�?Elasticsearch
 *
 * <p>支持的操作：
 * - c (create): 新增商品 �?插入 ES 文档
 * - u (update): 更新商品 �?更新 ES 文档
 * - d (delete): 删除商品 �?删除 ES 文档
 * - r (read): 初始快照 �?插入 ES 文档
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductSyncConsumer {
    private final ProductSearchRepository productSearchRepository;
    private final ObjectMapper objectMapper;

    /**
     * 监听商品 SPU 变更事件
     *
     * <p>Topic 命名规则：{server_name}.{schema}.{table}
     * 例如：scm_product_server.public.prod_spu
     */
    @KafkaListener(
            topics = "${kafka.product.sync.topic:scm_product_server.public.prod_spu}",
            groupId = "${kafka.product.sync.group:product-sync-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeProductChange(String message) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("📥 [数据同步] 收到商品变更事件: length={} bytes", message.length());
            log.debug("📥 [数据同步] 事件详情: {}", message);

            // 解析 Debezium 事件
            JsonNode event = objectMapper.readTree(message);
            String operation = event.get("op").asText();

            log.info("🔄 [数据同步] 操作类型: {}", operation);

            switch (operation) {
                case "c", "r" -> handleCreate(event);     // Create or Read (initial snapshot)
                case "u" -> handleUpdate(event);          // Update
                case "d" -> handleDelete(event);          // Delete
                default -> log.warn("⚠️  [数据同步] 未知操作类型: {}", operation);
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("�?[数据同步] 同步完成: 操作={}, 耗时={}ms", operation, duration);

        } catch (Exception e) {
            log.error("�?[数据同步] 处理变更事件失败: {}", e.getMessage(), e);
            // 可以选择将失败的消息发送到 DLQ（Dead Letter Queue�?
        }
    }

    /**
     * 处理新增操作（Create/Read�?
     */
    private void handleCreate(JsonNode event) {
        JsonNode after = event.get("after");
        if (after == null) {
            log.warn("⚠️  [数据同步-Create] after 字段为空，跳过处�?);
            return;
        }

        ProductDocument document = buildProductDocument(after);
        productSearchRepository.save(document);

        log.info("�?[数据同步-Create] 新增商品成功: id={}, spuName={}",
                document.getId(), document.getSpuName());
    }

    /**
     * 处理更新操作（Update�?
     */
    private void handleUpdate(JsonNode event) {
        JsonNode after = event.get("after");
        if (after == null) {
            log.warn("⚠️  [数据同步-Update] after 字段为空，跳过处�?);
            return;
        }

        ProductDocument document = buildProductDocument(after);
        productSearchRepository.save(document);

        log.info("�?[数据同步-Update] 更新商品成功: id={}, spuName={}",
                document.getId(), document.getSpuName());
    }

    /**
     * 处理删除操作（Delete�?
     */
    private void handleDelete(JsonNode event) {
        JsonNode before = event.get("before");
        if (before == null) {
            log.warn("⚠️  [数据同步-Delete] before 字段为空，跳过处�?);
            return;
        }

        String id = before.get("id").asText();
        productSearchRepository.deleteById(id);

        log.info("�?[数据同步-Delete] 删除商品成功: id={}", id);
    }

    /**
     * 构建 ProductDocument
     *
     * <p>�?Debezium 事件中提取商品数据，构建 ES 文档
     */
    private ProductDocument buildProductDocument(JsonNode data) {
        ProductDocument document = new ProductDocument();

        // 基础字段
        document.setId(getTextValue(data, "id"));
        document.setSpuCode(getTextValue(data, "spu_code"));
        document.setSpuName(getTextValue(data, "spu_name"));
        document.setCategoryId(getTextValue(data, "category_id"));
        document.setBrandId(getTextValue(data, "brand_id"));

        // 描述字段
        document.setDescription(getTextValue(data, "description"));
        document.setMainImage(getTextValue(data, "main_image"));

        // 价格字段
        document.setMinPrice(getDecimalValue(data, "min_price"));
        document.setMaxPrice(getDecimalValue(data, "max_price"));

        // 库存和销�?
        document.setTotalStock(getIntValue(data, "total_stock"));
        document.setTotalSales(getIntValue(data, "total_sales"));

        // SEO 字段
        document.setSeoTitle(getTextValue(data, "seo_title"));
        document.setSeoKeywords(getTextValue(data, "seo_keywords"));
        document.setSeoDescription(getTextValue(data, "seo_description"));

        // 状态和排序
        document.setStatus(getIntValue(data, "status"));
        document.setSortOrder(getIntValue(data, "sort_order"));

        // 时间字段
        document.setPublishedAt(getTimestampValue(data, "published_at"));
        document.setCreateTime(getTimestampValue(data, "create_time"));
        document.setUpdateTime(getTimestampValue(data, "update_time"));

        return document;
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取文本�?
     */
    private String getTextValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return (field != null && !field.isNull()) ? field.asText() : null;
    }

    /**
     * 获取整数�?
     */
    private Integer getIntValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return (field != null && !field.isNull()) ? field.asInt() : null;
    }

    /**
     * 获取 BigDecimal �?
     */
    private BigDecimal getDecimalValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field != null && !field.isNull()) {
            try {
                return new BigDecimal(field.asText());
            } catch (NumberFormatException e) {
                log.warn("⚠️  [数据同步] 解析 BigDecimal 失败: field={}, value={}", fieldName, field.asText());
                return null;
            }
        }
        return null;
    }

    /**
     * 获取时间戳�?
     *
     * <p>Debezium 默认使用微秒时间戳（1000000 微秒 = 1 秒）
     */
    private LocalDateTime getTimestampValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field != null && !field.isNull()) {
            try {
                // Debezium 时间戳是微秒，需要除�?1000000
                long microseconds = field.asLong();
                long milliseconds = microseconds / 1000;
                return LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(milliseconds),
                        ZoneId.systemDefault()
                );
            } catch (Exception e) {
                log.warn("⚠️  [数据同步] 解析时间戳失�? field={}, value={}", fieldName, field.asText());
                return null;
            }
        }
        return null;
    }
}
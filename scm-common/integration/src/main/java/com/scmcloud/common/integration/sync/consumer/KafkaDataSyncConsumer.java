package com.scmcloud.common.integration.sync.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scmcloud.common.integration.sync.event.DataSyncEvent;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;

/**
 * Kafka 数据同步消费�?
 * <p>
 * 特性：
 * - 手动提交 offset
 * - 分布式追踪上下文传播
 * - 委托�?RetryableEventProcessor 处理
 *
 * @author Deng
 * @since 2025-12-16
 */
@Slf4j
@RequiredArgsConstructor
public class KafkaDataSyncConsumer {
    private final RetryableEventProcessor processor;
    private final ObjectMapper objectMapper;
    private final Tracer tracer;

    /**
     * 消费数据同步事件
     * <p>
     * 使用 pattern 匹配所�?datasync.* 主题
     */
    @KafkaListener(
            topicPattern = "${datasync.topic-prefix:datasync}.*",
            groupId = "${datasync.consumer.group-id:datasync-consumer}",
            containerFactory = "dataSyncKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String topic = record.topic();
        String key = record.key();
        String payload = record.value();

        log.debug("[DataSync] Received message: topic={}, key={}, partition={}, offset={}",
                topic, key, record.partition(), record.offset());

        try {
            // 1. 反序列化事件
            DataSyncEvent event = objectMapper.readValue(payload, DataSyncEvent.class);

            // 2. 恢复追踪上下�?
            Span span = createSpanWithRemoteContext(event);

            try (Scope ignored = span.makeCurrent()) {
                // 3. 委托给处理器
                processor.process(event);

                // 4. 手动提交 offset
                ack.acknowledge();

            } finally {
                span.end();
            }

        } catch (Exception e) {
            log.error("[DataSync] Failed to process message: topic={}, key={}, error={}",
                    topic, key, e.getMessage(), e);
            // 不提�?offset，消息将被重新消�?
        }
    }

    /**
     * 从事件中恢复追踪上下�?
     */
    private Span createSpanWithRemoteContext(DataSyncEvent event) {
        if (event.getTraceId() != null && event.getSpanId() != null) {
            // 恢复远程上下�?
            SpanContext remoteContext = SpanContext.createFromRemoteParent(event.getTraceId(), event.getSpanId(),
                    TraceFlags.getSampled(), TraceState.getDefault()
            );

            return tracer.spanBuilder("datasync.consume")
                    .setParent(Context.current().with(Span.wrap(remoteContext)))
                    .setAttribute("aggregate.type", event.getAggregateType())
                    .setAttribute("event.id", event.getEventId())
                    .startSpan();
        } else {
            return tracer.spanBuilder("datasync.consume")
                    .setAttribute("aggregate.type", event.getAggregateType())
                    .setAttribute("event.id", event.getEventId())
                    .startSpan();
        }
    }
}

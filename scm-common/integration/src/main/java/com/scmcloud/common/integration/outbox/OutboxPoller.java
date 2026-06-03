package com.scmcloud.common.integration.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scmcloud.common.integration.messaging.KafkaMessagePublisher;
import com.scmcloud.common.integration.model.MessageEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Polls the outbox table for unpublished events and publishes to Kafka.
 * Runs on a fixed schedule. Uses SELECT ... FOR UPDATE SKIP LOCKED
 * to support multiple instances without duplicate publishing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(KafkaMessagePublisher.class)
public class OutboxPoller {

    private final OutboxService outboxService;
    private final KafkaMessagePublisher kafkaPublisher;
    private final ObjectMapper objectMapper;

    private static final int BATCH_SIZE = 50;
    private static final String OUTBOX_TOPIC = "domain.events";

    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms:1000}")
    public void pollAndPublish() {
        // 1. Publish pending events
        List<OutboxEvent> pending = outboxService.findPendingEvents(BATCH_SIZE);
        for (OutboxEvent event : pending) {
            publishEvent(event);
        }

        // 2. Retry failed events
        List<OutboxEvent> retryable = outboxService.findRetryableEvents(BATCH_SIZE);
        for (OutboxEvent event : retryable) {
            publishEvent(event);
        }

        // 3. Cleanup old published events (run once per hour, checked by interval)
        if (!pending.isEmpty() || !retryable.isEmpty()) {
            log.debug("Outbox: processed {} pending, {} retryable", pending.size(), retryable.size());
        }
    }

    private void publishEvent(OutboxEvent event) {
        try {
            MessageEnvelope<String> envelope = MessageEnvelope.of(
                    event.getEventType(),
                    "outbox",
                    event.getPayload()
            ).toBuilder()
                    .tenantId(event.getTenantId() != null ? event.getTenantId().toString() : null)
                    .build();

            kafkaPublisher.send(OUTBOX_TOPIC, event.getAggregateId(), envelope);
            outboxService.markPublished(event.getId());
            log.debug("Published outbox event: type={}, id={}", event.getEventType(), event.getId());
        } catch (Exception e) {
            log.warn("Failed to publish outbox event id={}: {}", event.getId(), e.getMessage());
            outboxService.markFailed(event.getId(), e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanup() {
        int deleted = outboxService.cleanup(7);
        if (deleted > 0) {
            log.info("Outbox cleanup: removed {} old published events", deleted);
        }
    }
}

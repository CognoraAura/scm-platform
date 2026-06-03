package com.scmcloud.common.integration.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scmcloud.common.domain.event.DomainEvent;
import com.scmcloud.common.domain.event.DomainEventPublisher;
import com.scmcloud.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for writing domain events to the outbox table.
 * Events are written in the same DB transaction as the aggregate mutation.
 *
 * <p>Usage in a command service:</p>
 * <pre>
 * outboxService.save(orderCreatedEvent);
 * // ... entity mutation happens in the same @Transactional method
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventMapper outboxEventMapper;
    private final ObjectMapper objectMapper;

    /**
     * Save a domain event to the outbox. Must be called within a @Transactional boundary.
     */
    public void save(DomainEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = OutboxEvent.create(
                    event.getEventType(),
                    event.getClass().getSimpleName(),
                    event.getEventId(),
                    payload,
                    event.getTenantId()
            );
            outboxEventMapper.insert(outboxEvent);
            log.debug("Saved domain event to outbox: type={}, id={}", event.getEventType(), event.getEventId());
        } catch (Exception e) {
            log.error("Failed to save domain event to outbox: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save domain event to outbox", e);
        }
    }

    /**
     * Fetch pending events for publishing.
     */
    public List<OutboxEvent> findPendingEvents(int batchSize) {
        return outboxEventMapper.findPending(batchSize);
    }

    /**
     * Fetch failed events eligible for retry.
     */
    public List<OutboxEvent> findRetryableEvents(int batchSize) {
        return outboxEventMapper.findRetryable(batchSize);
    }

    /**
     * Mark an event as published.
     */
    public void markPublished(String eventId) {
        outboxEventMapper.markPublished(eventId);
    }

    /**
     * Mark an event as failed with error message.
     */
    public void markFailed(String eventId, String error) {
        outboxEventMapper.markFailed(eventId, error);
    }

    /**
     * Cleanup old published events (retain for 7 days).
     */
    public int cleanup(int retentionDays) {
        return outboxEventMapper.deleteOldPublished(retentionDays);
    }

    @Mapper
    public interface OutboxEventMapper {

        @Insert("INSERT INTO outbox_event (id, event_type, aggregate_type, aggregate_id, payload, tenant_id, " +
                "status, retry_count, max_retries, created_at) " +
                "VALUES (#{id}, #{eventType}, #{aggregateType}, #{aggregateId}, #{payload}::jsonb, #{tenantId}, " +
                "#{status}, #{retryCount}, #{maxRetries}, #{createdAt})")
        int insert(OutboxEvent event);

        @Select("SELECT * FROM outbox_event WHERE status = 'PENDING' ORDER BY created_at ASC LIMIT #{limit} FOR UPDATE SKIP LOCKED")
        List<OutboxEvent> findPending(@Param("limit") int limit);

        @Select("SELECT * FROM outbox_event WHERE status = 'FAILED' AND retry_count < max_retries " +
                "AND next_retry_at <= CURRENT_TIMESTAMP ORDER BY next_retry_at ASC LIMIT #{limit} FOR UPDATE SKIP LOCKED")
        List<OutboxEvent> findRetryable(@Param("limit") int limit);

        @Update("UPDATE outbox_event SET status = 'PUBLISHED', published_at = CURRENT_TIMESTAMP WHERE id = #{eventId}")
        int markPublished(@Param("eventId") String eventId);

        @Update("UPDATE outbox_event SET status = 'FAILED', last_error = #{error}, " +
                "retry_count = retry_count + 1, " +
                "next_retry_at = CURRENT_TIMESTAMP + (2 ^ LEAST(retry_count + 1, 5)) * INTERVAL '1 second' " +
                "WHERE id = #{eventId}")
        int markFailed(@Param("eventId") String eventId, @Param("error") String error);

        @Update("DELETE FROM outbox_event WHERE status = 'PUBLISHED' " +
                "AND published_at < CURRENT_TIMESTAMP - (#{days} || ' days')::interval")
        int deleteOldPublished(@Param("days") int days);
    }
}

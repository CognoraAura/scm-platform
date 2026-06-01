package com.scmcloud.common.integration.sync.consumer;

import com.scmcloud.common.integration.sync.config.DataSyncProperties;
import com.scmcloud.common.integration.sync.event.DataSyncEvent;
import com.scmcloud.common.integration.sync.handler.DataSyncHandler;
import com.scmcloud.common.integration.sync.publisher.DataSyncPublisher;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * 可重试的事件处理�?
 * <p>
 * 特性：
 * - 指数退避重�?
 * - 幂等消费
 * - 死信队列
 * - 指标监控
 *
 * @author Deng
 * @since 2025-12-16
 */
@Slf4j
public class RetryableEventProcessor {
    private final Map<String, DataSyncHandler> handlers = new ConcurrentHashMap<>();
    private final IdempotentChecker idempotentChecker;
    private final DataSyncPublisher publisher;
    private final DataSyncProperties properties;

    // Metrics
    private final Counter processSuccessCounter;
    private final Counter processFailureCounter;
    private final Counter retryCounter;
    private final Timer processTimer;

    public RetryableEventProcessor(IdempotentChecker idempotentChecker, DataSyncPublisher publisher,
                                   DataSyncProperties properties, MeterRegistry meterRegistry) {
        this.idempotentChecker = idempotentChecker;
        this.publisher = publisher;
        this.properties = properties;

        // Initialize metrics
        this.processSuccessCounter = Counter.builder("datasync.process.success")
                .description("Number of successfully processed events")
                .register(meterRegistry);
        this.processFailureCounter = Counter.builder("datasync.process.failure")
                .description("Number of failed event processing")
                .register(meterRegistry);
        this.retryCounter = Counter.builder("datasync.process.retry")
                .description("Number of retry attempts")
                .register(meterRegistry);
        this.processTimer = Timer.builder("datasync.process.duration")
                .description("Time taken to process events")
                .register(meterRegistry);
    }

    /**
     * 注册处理�?
     */
    public void registerHandler(DataSyncHandler handler) {
        handlers.put(handler.getAggregateType(), handler);
        log.info("[DataSync] Registered handler for: {}", handler.getAggregateType());
    }

    /**
     * 处理事件（带重试�?
     */
    public void process(DataSyncEvent event) {
        String eventId = event.getEventId();

        // 1. 幂等检�?
        if (!idempotentChecker.tryAcquire(eventId)) {
            log.debug("[DataSync] Skipping duplicate event: {}", eventId);
            return;
        }

        // 2. 查找处理�?
        DataSyncHandler handler = handlers.get(event.getAggregateType());
        if (handler == null) {
            log.warn("[DataSync] No handler for aggregate type: {}", event.getAggregateType());
            idempotentChecker.markCompleted(eventId);
            return;
        }

        // 3. 执行处理（带重试�?
        processTimer.record(() -> processWithRetry(event, handler));
    }

    private void processWithRetry(DataSyncEvent event, DataSyncHandler handler) {
        DataSyncProperties.RetryConfig retryConfig = properties.getRetry();
        String eventId = event.getEventId();
        Exception lastException = null;

        for (int attempt = 0; attempt <= retryConfig.getMaxAttempts(); attempt++) {
            try {
                if (attempt > 0) {
                    // 计算退避时间并等待
                    long backoffMs = calculateBackoff(attempt, retryConfig);
                    log.info("[DataSync] Retry attempt {} for event {}, waiting {}ms",
                            attempt, eventId, backoffMs);
                    backoffWait(backoffMs);
                    retryCounter.increment();
                }

                // 执行处理
                handler.handle(event);

                // 成功
                idempotentChecker.markCompleted(eventId);
                processSuccessCounter.increment();
                log.debug("[DataSync] Successfully processed event: {}", eventId);
                return;

            } catch (DataSyncHandler.DataSyncException e) {
                lastException = e;
                if (!e.isRetryable()) {
                    log.error("[DataSync] Non-retryable error for event {}: {}",
                            eventId, e.getMessage());
                    break;
                }
                log.warn("[DataSync] Retryable error for event {}: {}",
                        eventId, e.getMessage());

            } catch (Exception e) {
                lastException = e;
                log.warn("[DataSync] Error processing event {}: {}",
                        eventId, e.getMessage());
            }

            // 检查线程中�?
            if (Thread.currentThread().isInterrupted()) {
                log.warn("[DataSync] Thread interrupted, stopping retry for event: {}", eventId);
                break;
            }
        }

        // 所有重试失败，发送到死信队列
        handleFailure(event, lastException);
    }

    /**
     * 退避等待（使用 LockSupport 避免 InterruptedException�?
     */
    private void backoffWait(long milliseconds) {
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(milliseconds));
    }

    private long calculateBackoff(int attempt, DataSyncProperties.RetryConfig config) {
        long interval = (long) (config.getInitialIntervalMs() * Math.pow(config.getMultiplier(), attempt - 1));

        return Math.min(interval, config.getMaxIntervalMs());
    }

    private void handleFailure(DataSyncEvent event, Exception exception) {
        String eventId = event.getEventId();
        String reason = exception != null ? exception.getMessage() : "Unknown error";

        // 释放幂等锁，允许后续重新处理
        idempotentChecker.markFailed(eventId);

        // 发送到死信队列
        event.incrementRetry(reason);
        publisher.publishToDeadLetter(event, reason);

        processFailureCounter.increment();
        log.error("[DataSync] Event processing failed after all retries: eventId={}, reason={}",
                eventId, reason);
    }
}

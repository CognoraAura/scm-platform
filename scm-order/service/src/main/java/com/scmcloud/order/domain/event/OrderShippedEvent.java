package com.scmcloud.order.domain.event;

import com.scmcloud.common.domain.event.DomainEvent;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Published when an order is shipped.
 * Triggers tracking updates and notification.
 */
@Getter
public class OrderShippedEvent extends DomainEvent {

    private final Long orderId;
    private final String orderNo;
    private final UUID userId;
    private final String waybillNo;
    private final String carrier;
    private final LocalDateTime shippedAt;
    private final LocalDateTime estimatedArrival;

    public OrderShippedEvent(UUID tenantId, Long orderId, String orderNo, UUID userId,
                             String waybillNo, String carrier, LocalDateTime estimatedArrival) {
        super(tenantId);
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.userId = userId;
        this.waybillNo = waybillNo;
        this.carrier = carrier;
        this.shippedAt = LocalDateTime.now();
        this.estimatedArrival = estimatedArrival;
    }

    @Override
    public String getEventType() {
        return "order.shipped";
    }
}

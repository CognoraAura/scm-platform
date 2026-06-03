package com.scmcloud.order.domain.event;

import com.scmcloud.common.domain.event.DomainEvent;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Published when an order is cancelled.
 * Triggers inventory release, refund processing, and notification.
 */
@Getter
public class OrderCancelledEvent extends DomainEvent {

    private final Long orderId;
    private final String orderNo;
    private final UUID userId;
    private final String cancelReason;
    private final BigDecimal refundAmount;
    private final String reservationId;

    public OrderCancelledEvent(UUID tenantId, Long orderId, String orderNo, UUID userId,
                               String cancelReason, BigDecimal refundAmount, String reservationId) {
        super(tenantId);
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.userId = userId;
        this.cancelReason = cancelReason;
        this.refundAmount = refundAmount;
        this.reservationId = reservationId;
    }

    @Override
    public String getEventType() {
        return "order.cancelled";
    }
}

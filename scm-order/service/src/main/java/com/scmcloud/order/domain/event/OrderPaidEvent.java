package com.scmcloud.order.domain.event;

import com.scmcloud.common.domain.event.DomainEvent;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Published when an order payment is confirmed.
 * Triggers settlement calculation and notification.
 */
@Getter
public class OrderPaidEvent extends DomainEvent {

    private final Long orderId;
    private final String orderNo;
    private final UUID userId;
    private final BigDecimal paidAmount;
    private final String paymentNo;
    private final Integer paymentMethod;
    private final LocalDateTime paidAt;

    public OrderPaidEvent(UUID tenantId, Long orderId, String orderNo, UUID userId,
                          BigDecimal paidAmount, String paymentNo, Integer paymentMethod) {
        super(tenantId);
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.userId = userId;
        this.paidAmount = paidAmount;
        this.paymentNo = paymentNo;
        this.paymentMethod = paymentMethod;
        this.paidAt = LocalDateTime.now();
    }

    @Override
    public String getEventType() {
        return "order.paid";
    }
}

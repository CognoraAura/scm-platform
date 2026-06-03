package com.scmcloud.order.domain.event;

import com.scmcloud.common.domain.event.DomainEvent;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Published when a new order is created.
 * Carries the essential order data for downstream consumers (finance, logistics, notify, audit).
 */
@Getter
public class OrderCreatedEvent extends DomainEvent {

    private final Long orderId;
    private final String orderNo;
    private final UUID userId;
    private final BigDecimal totalAmount;
    private final BigDecimal payableAmount;
    private final Integer quantity;
    private final String warehouseId;
    private final String skuId;

    public OrderCreatedEvent(UUID tenantId, Long orderId, String orderNo, UUID userId,
                             BigDecimal totalAmount, BigDecimal payableAmount,
                             Integer quantity, String warehouseId, String skuId) {
        super(tenantId);
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.payableAmount = payableAmount;
        this.quantity = quantity;
        this.warehouseId = warehouseId;
        this.skuId = skuId;
    }

    @Override
    public String getEventType() {
        return "order.created";
    }
}

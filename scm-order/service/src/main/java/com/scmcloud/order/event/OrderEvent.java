package com.scmcloud.order.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public abstract class OrderEvent {
    private final UUID eventId;
    private final UUID orderId;
    private final Instant timestamp;
    private final String eventType;

    protected OrderEvent(UUID orderId, String eventType) {
        this.eventId = UUID.randomUUID();
        this.orderId = orderId;
        this.timestamp = Instant.now();
        this.eventType = eventType;
    }

    public UUID getEventId() { return eventId; }
    public UUID getOrderId() { return orderId; }
    public Instant getTimestamp() { return timestamp; }
    public String getEventType() { return eventType; }
}

class OrderCreatedEvent extends OrderEvent {
    private final String customerName;
    private final BigDecimal totalAmount;

    public OrderCreatedEvent(UUID orderId, String customerName, BigDecimal totalAmount) {
        super(orderId, "ORDER_CREATED");
        this.customerName = customerName;
        this.totalAmount = totalAmount;
    }

    public String getCustomerName() { return customerName; }
    public BigDecimal getTotalAmount() { return totalAmount; }
}

class OrderStatusChangedEvent extends OrderEvent {
    private final String oldStatus;
    private final String newStatus;

    public OrderStatusChangedEvent(UUID orderId, String oldStatus, String newStatus) {
        super(orderId, "ORDER_STATUS_CHANGED");
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    public String getOldStatus() { return oldStatus; }
    public String getNewStatus() { return newStatus; }
}

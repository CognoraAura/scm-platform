package com.scmcloud.order.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = OrderCreatedEvent.class, name = "ORDER_CREATED"),
    @JsonSubTypes.Type(value = OrderStatusChangedEvent.class, name = "ORDER_STATUS_CHANGED")
})
public abstract class OrderEvent {
    private final UUID eventId;
    private final UUID orderId;
    private final Instant timestamp;
    private final String eventType;

    protected OrderEvent(UUID orderId, String eventType) {
        this(UUID.randomUUID(), orderId, Instant.now(), eventType);
    }

    @JsonCreator
    protected OrderEvent(@JsonProperty("eventId") UUID eventId,
                          @JsonProperty("orderId") UUID orderId,
                          @JsonProperty("timestamp") Instant timestamp,
                          @JsonProperty("eventType") String eventType) {
        this.eventId = eventId;
        this.orderId = orderId;
        this.timestamp = timestamp;
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

    @JsonCreator
    public OrderCreatedEvent(@JsonProperty("eventId") UUID eventId,
                              @JsonProperty("orderId") UUID orderId,
                              @JsonProperty("timestamp") Instant timestamp,
                              @JsonProperty("eventType") String eventType,
                              @JsonProperty("customerName") String customerName,
                              @JsonProperty("totalAmount") BigDecimal totalAmount) {
        super(eventId, orderId, timestamp, eventType);
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

    @JsonCreator
    public OrderStatusChangedEvent(@JsonProperty("eventId") UUID eventId,
                                    @JsonProperty("orderId") UUID orderId,
                                    @JsonProperty("timestamp") Instant timestamp,
                                    @JsonProperty("eventType") String eventType,
                                    @JsonProperty("oldStatus") String oldStatus,
                                    @JsonProperty("newStatus") String newStatus) {
        super(eventId, orderId, timestamp, eventType);
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    public String getOldStatus() { return oldStatus; }
    public String getNewStatus() { return newStatus; }
}

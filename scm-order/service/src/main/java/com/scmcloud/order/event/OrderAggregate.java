package com.scmcloud.order.event;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OrderAggregate {
    private UUID orderId;
    private String customerName;
    private BigDecimal totalAmount;
    private String status;
    private List<OrderEvent> uncommittedEvents = new ArrayList<>();

    public static OrderAggregate create(UUID orderId, String customerName, BigDecimal totalAmount) {
        OrderAggregate aggregate = new OrderAggregate();
        OrderCreatedEvent event = new OrderCreatedEvent(orderId, customerName, totalAmount);
        aggregate.apply(event);
        aggregate.uncommittedEvents.add(event);
        return aggregate;
    }

    public void changeStatus(String newStatus) {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(orderId, this.status, newStatus);
        apply(event);
        uncommittedEvents.add(event);
    }

    public void apply(OrderEvent event) {
        if (event instanceof OrderCreatedEvent e) {
            this.orderId = e.getOrderId();
            this.customerName = e.getCustomerName();
            this.totalAmount = e.getTotalAmount();
            this.status = "PENDING";
        } else if (event instanceof OrderStatusChangedEvent e) {
            this.status = e.getNewStatus();
        }
    }

    public List<OrderEvent> getUncommittedEvents() {
        return uncommittedEvents;
    }

    public void clearUncommittedEvents() {
        uncommittedEvents.clear();
    }
}

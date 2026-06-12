package com.scmcloud.order.event;

import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public class OrderEventStore {

    private final OrderEventMapper eventMapper;

    public OrderEventStore(OrderEventMapper eventMapper) {
        this.eventMapper = eventMapper;
    }

    public void append(OrderEvent event) {
        eventMapper.insert(event);
    }

    public List<OrderEvent> getEvents(UUID orderId) {
        return eventMapper.findByOrderIdOrderByTimestamp(orderId);
    }

    public List<OrderEvent> getEvents(UUID orderId, int offset, int limit) {
        return eventMapper.findByOrderIdOrderByTimestamp(orderId, offset, limit);
    }
}

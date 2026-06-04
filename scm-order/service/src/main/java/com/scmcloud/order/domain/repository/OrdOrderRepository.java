package com.scmcloud.order.domain.repository;

import com.scmcloud.common.domain.event.DomainEvent;
import com.scmcloud.common.domain.event.DomainEventPublisher;
import com.scmcloud.common.integration.outbox.OutboxService;
import com.scmcloud.order.domain.entity.OrdOrder;
import com.scmcloud.order.domain.entity.OrdOrderItem;
import com.scmcloud.order.mapper.OrdOrderItemMapper;
import com.scmcloud.order.mapper.OrdOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Repository for the OrdOrder aggregate root.
 * Handles persistence of the aggregate and publishing of domain events.
 *
 * <p>Domain events are saved to the outbox table in the same transaction
 * as the aggregate mutation, ensuring exactly-once delivery semantics.</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class OrdOrderRepository {

    private final OrdOrderMapper orderMapper;
    private final OrdOrderItemMapper orderItemMapper;
    private final OutboxService outboxService;
    private final DomainEventPublisher eventPublisher;

    /**
     * Save the order aggregate and publish domain events.
     * Events are written to the outbox table in the same transaction.
     */
    @Transactional(rollbackFor = Exception.class)
    public void save(OrdOrder order) {
        // 1. Save or update the order
        if (order.getId() == null) {
            orderMapper.insert(order);
        } else {
            orderMapper.updateById(order);
        }

        // 2. Save order items
        if (order.getItems() != null) {
            for (OrdOrderItem item : order.getItems()) {
                if (item.getId() == null) {
                    item.setOrderId(order.getId());
                    orderItemMapper.insert(item);
                } else {
                    orderItemMapper.updateById(item);
                }
            }
        }

        // 3. Publish domain events via outbox
        if (order.hasDomainEvents()) {
            List<DomainEvent> events = order.pullDomainEvents();
            for (DomainEvent event : events) {
                outboxService.save(event);
                log.debug("Saved domain event to outbox: type={}, orderId={}",
                        event.getEventType(), order.getId());
            }
        }
    }

    /**
     * Find order by ID.
     */
    public OrdOrder findById(Long id) {
        return orderMapper.selectById(id);
    }

    /**
     * Find order by order number.
     */
    public OrdOrder findByOrderNo(String orderNo) {
        return orderMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrdOrder>()
                        .eq(OrdOrder::getOrderNo, orderNo)
                        .eq(OrdOrder::getDeleted, false)
        );
    }

    /**
     * Find orders by user ID.
     */
    public List<OrdOrder> findByUserId(String userId) {
        return orderMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrdOrder>()
                        .eq(OrdOrder::getUserId, userId)
                        .eq(OrdOrder::getDeleted, false)
                        .orderByDesc(OrdOrder::getCreateTime)
        );
    }
}

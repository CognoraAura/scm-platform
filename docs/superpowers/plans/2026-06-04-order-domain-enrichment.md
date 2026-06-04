# Order Domain Enrichment - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform the anemic OrdOrder entity into a rich aggregate root with proper domain behavior, state machine, and domain events.

**Architecture:** DDD aggregate pattern with domain events via transactional outbox. Order aggregate enforces all lifecycle invariants. Downstream services react via Kafka events.

**Tech Stack:** Java 21, Spring Boot 4, MyBatis-Plus, Kafka, Transactional Outbox

---

## Current State Assessment

**Already Implemented:**
- `OrdOrder` entity has basic domain methods (pay, cancel, ship, complete, etc.)
- `OrderStatus` enum with transition validation
- `OutboxService` for transactional event publishing
- `DomainEvent` base class and `DomainEventPublisher`

**Needs Implementation:**
- Rich aggregate root with factory method and invariant enforcement
- Domain events (OrderCreatedEvent, OrderPaidEvent, etc.)
- Repository that persists aggregate + publishes events atomically
- Kafka consumers for downstream services

---

## File Structure

```
scm-order/
  service/src/main/java/com/scmcloud/order/
    domain/
      entity/
        OrdOrder.java                  ← MODIFY - Enrich as aggregate root
        OrdOrderItem.java              ← MODIFY - Add aggregate support
        OrderStatus.java               ← VERIFY - Already exists
      event/
        OrderCreatedEvent.java         ← MODIFY - Extend DomainEvent
        OrderPaidEvent.java            ← CREATE
        OrderCancelledEvent.java       ← MODIFY - Extend DomainEvent
        OrderShippedEvent.java         ← CREATE
        OrderCompletedEvent.java       ← CREATE
      repository/
        OrdOrderRepository.java        ← CREATE - Aggregate repository
    service/
      impl/
        OrdOrderServiceImpl.java       ← MODIFY - Use aggregate
      command/
        OrdOrderCommandService.java    ← MODIFY - Use aggregate

scm-common/
  integration/src/main/java/com/scmcloud/common/
    domain/event/
      DomainEvent.java                 ← VERIFY - Already exists
      DomainEventPublisher.java        ← VERIFY - Already exists
```

---

### Task 1: Verify DomainEvent Infrastructure

**Files:**
- Verify: `scm-common/integration/src/main/java/com/scmcloud/common/domain/event/DomainEvent.java`
- Verify: `scm-common/integration/src/main/java/com/scmcloud/common/domain/event/DomainEventPublisher.java`

- [ ] **Step 1: Read DomainEvent base class**

Read `DomainEvent.java` and verify it has:
- `eventId` (UUID)
- `eventType` (String)
- `tenantId` (UUID)
- `occurredAt` (Instant)
- `aggregateId` (String)
- `aggregateType` (String)

Status: Expected to exist

- [ ] **Step 2: Read DomainEventPublisher**

Read `DomainEventPublisher.java` and verify it has:
- `publish(DomainEvent event)` method

Status: Expected to exist

- [ ] **Step 3: If missing, create them**

If `DomainEvent` doesn't exist:
```java
package com.scmcloud.common.domain.event;

import lombok.Getter;
import java.time.Instant;
import java.util.UUID;

@Getter
public abstract class DomainEvent {
    private final String eventId;
    private final String eventType;
    private final String tenantId;
    private final Instant occurredAt;
    private final String aggregateId;
    private final String aggregateType;

    protected DomainEvent(String aggregateId, String aggregateType, String tenantId) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = this.getClass().getSimpleName();
        this.tenantId = tenantId;
        this.occurredAt = Instant.now();
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
    }
}
```

- [ ] **Step 4: Commit if changes made**

```bash
git add scm-common/
git commit -m "feat(domain): verify/create DomainEvent infrastructure"
```

---

### Task 2: Enrich OrdOrder as Aggregate Root

**Files:**
- Modify: `scm-order/service/src/main/java/com/scmcloud/order/domain/entity/OrdOrder.java`

- [ ] **Step 1: Read current OrdOrder**

Read `OrdOrder.java` and understand existing structure.

Current state: Already has basic domain methods (pay, cancel, ship, etc.) but:
- No factory method
- No domain event registration
- No aggregate encapsulation

- [ ] **Step 2: Add domain event collection**

Add to `OrdOrder`:
```java
import com.scmcloud.common.domain.event.DomainEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Add these fields
@TableField(exist = false)
private List<DomainEvent> domainEvents = new ArrayList<>();

// Add these methods
public void registerEvent(DomainEvent event) {
    this.domainEvents.add(event);
}

public List<DomainEvent> getDomainEvents() {
    return Collections.unmodifiableList(domainEvents);
}

public void clearDomainEvents() {
    this.domainEvents.clear();
}
```

- [ ] **Step 3: Add factory method**

```java
/**
 * Factory method to create a new order.
 * Enforces invariants and registers OrderCreatedEvent.
 */
public static OrdOrder create(CreateOrderContext ctx) {
    OrdOrder order = new OrdOrder();
    order.orderNo = generateOrderNo();
    order.userId = ctx.getUserId();
    order.username = ctx.getUsername();
    order.orderType = ctx.getOrderType();
    order.orderSource = ctx.getOrderSource();
    order.status = OrderStatus.PENDING_PAYMENT.getCode();
    order.version = 0;
    order.deleted = false;
    order.createTime = LocalDateTime.now();

    // Add items
    for (OrderItemData itemData : ctx.getItems()) {
        order.addItem(itemData);
    }

    // Calculate amounts
    order.calculateAmounts();

    // Register domain event
    order.registerEvent(new OrderCreatedEvent(
            order.orderNo,
            order.tenantId,
            order.userId,
            order.totalAmount,
            order.items.stream().map(i -> new OrderItemSnapshot(
                    i.getSkuId(), i.getSkuName(), i.getQuantity(), i.getUnitPrice()
            )).collect(Collectors.toList())
    ));

    return order;
}

private static String generateOrderNo() {
    // Generate order number: ORD + timestamp + random
    return "ORD" + System.currentTimeMillis() + String.format("%04d", new Random().nextInt(10000));
}
```

- [ ] **Step 4: Enrich existing methods to register events**

Modify `pay()`:
```java
public OrdOrder pay(BigDecimal amount, String paymentNo) {
    transitionTo(OrderStatus.PAID);
    this.paidAmount = amount;
    this.paidAt = LocalDateTime.now();
    this.paymentNo = paymentNo;

    // Register domain event
    registerEvent(new OrderPaidEvent(
            this.orderNo,
            this.tenantId,
            paymentNo,
            amount,
            this.paidAt
    ));

    return this;
}
```

Modify `cancel()`:
```java
public OrdOrder cancel(String reason) {
    OrderStatus current = getStatusEnum();
    if (!current.isCancellable()) {
        throw new IllegalStateException(
                "Cannot cancel order " + orderNo + " in status " + current);
    }
    transitionTo(OrderStatus.CANCELLED);
    this.cancelledAt = LocalDateTime.now();
    this.cancelReason = reason;

    // Register domain event
    registerEvent(new OrderCancelledEvent(
            this.orderNo,
            this.tenantId,
            reason,
            this.cancelledAt
    ));

    return this;
}
```

Modify `ship()`:
```java
public OrdOrder ship(String waybillNo, String carrier) {
    transitionTo(OrderStatus.SHIPPED);
    this.waybillNo = waybillNo;
    this.carrier = carrier;
    this.shippedAt = LocalDateTime.now();

    // Register domain event
    registerEvent(new OrderShippedEvent(
            this.orderNo,
            this.tenantId,
            waybillNo,
            carrier,
            this.shippedAt
    ));

    return this;
}
```

Modify `complete()`:
```java
public OrdOrder complete() {
    transitionTo(OrderStatus.COMPLETED);
    this.completedAt = LocalDateTime.now();

    // Register domain event
    registerEvent(new OrderCompletedEvent(
            this.orderNo,
            this.tenantId,
            this.completedAt
    ));

    return this;
}
```

- [ ] **Step 5: Commit**

```bash
git add scm-order/service/src/main/java/com/scmcloud/order/domain/entity/OrdOrder.java
git commit -m "feat(order): enrich OrdOrder as aggregate root with domain events"
```

---

### Task 3: Create Domain Event Classes

**Files:**
- Modify: `scm-order/service/src/main/java/com/scmcloud/order/domain/event/OrderCreatedEvent.java`
- Modify: `scm-order/service/src/main/java/com/scmcloud/order/domain/event/OrderCancelledEvent.java`
- Create: `scm-order/service/src/main/java/com/scmcloud/order/domain/event/OrderPaidEvent.java`
- Create: `scm-order/service/src/main/java/com/scmcloud/order/domain/event/OrderShippedEvent.java`
- Create: `scm-order/service/src/main/java/com/scmcloud/order/domain/event/OrderCompletedEvent.java`

- [ ] **Step 1: Create OrderPaidEvent**

```java
package com.scmcloud.order.domain.event;

import com.scmcloud.common.domain.event.DomainEvent;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class OrderPaidEvent extends DomainEvent {
    private final String paymentNo;
    private final BigDecimal paidAmount;
    private final LocalDateTime paidAt;

    public OrderPaidEvent(String orderNo, String tenantId, String paymentNo,
                          BigDecimal paidAmount, LocalDateTime paidAt) {
        super(orderNo, "OrdOrder", tenantId);
        this.paymentNo = paymentNo;
        this.paidAmount = paidAmount;
        this.paidAt = paidAt;
    }
}
```

- [ ] **Step 2: Create OrderShippedEvent**

```java
package com.scmcloud.order.domain.event;

import com.scmcloud.common.domain.event.DomainEvent;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class OrderShippedEvent extends DomainEvent {
    private final String waybillNo;
    private final String carrier;
    private final LocalDateTime shippedAt;

    public OrderShippedEvent(String orderNo, String tenantId, String waybillNo,
                             String carrier, LocalDateTime shippedAt) {
        super(orderNo, "OrdOrder", tenantId);
        this.waybillNo = waybillNo;
        this.carrier = carrier;
        this.shippedAt = shippedAt;
    }
}
```

- [ ] **Step 3: Create OrderCompletedEvent**

```java
package com.scmcloud.order.domain.event;

import com.scmcloud.common.domain.event.DomainEvent;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class OrderCompletedEvent extends DomainEvent {
    private final LocalDateTime completedAt;

    public OrderCompletedEvent(String orderNo, String tenantId, LocalDateTime completedAt) {
        super(orderNo, "OrdOrder", tenantId);
        this.completedAt = completedAt;
    }
}
```

- [ ] **Step 4: Update existing events to extend DomainEvent**

Modify `OrderCreatedEvent.java`:
```java
package com.scmcloud.order.domain.event;

import com.scmcloud.common.domain.event.DomainEvent;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
public class OrderCreatedEvent extends DomainEvent {
    private final BigDecimal totalAmount;
    private final List<OrderItemSnapshot> items;

    public OrderCreatedEvent(String orderNo, String tenantId, String userId,
                             BigDecimal totalAmount, List<OrderItemSnapshot> items) {
        super(orderNo, "OrdOrder", tenantId);
        this.totalAmount = totalAmount;
        this.items = items;
    }

    @Getter
    public static class OrderItemSnapshot {
        private final String skuId;
        private final String skuName;
        private final Integer quantity;
        private final BigDecimal unitPrice;

        public OrderItemSnapshot(String skuId, String skuName, Integer quantity, BigDecimal unitPrice) {
            this.skuId = skuId;
            this.skuName = skuName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add scm-order/service/src/main/java/com/scmcloud/order/domain/event/
git commit -m "feat(order): create domain event classes for order lifecycle"
```

---

### Task 4: Create OrdOrderRepository

**Files:**
- Create: `scm-order/service/src/main/java/com/scmcloud/order/domain/repository/OrdOrderRepository.java`

- [ ] **Step 1: Create aggregate repository**

```java
package com.scmcloud.order.domain.repository;

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

import java.util.Optional;

/**
 * Repository for OrdOrder aggregate.
 * Persists aggregate root + children and publishes domain events.
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
     * Save aggregate and publish domain events atomically.
     */
    @Transactional
    public OrdOrder save(OrdOrder order) {
        // 1. Save or update aggregate root
        if (order.getId() == null) {
            orderMapper.insert(order);
        } else {
            orderMapper.updateById(order);
        }

        // 2. Save or update items
        if (order.getItems() != null) {
            for (OrdOrderItem item : order.getItems()) {
                item.setOrderId(order.getId());
                if (item.getId() == null) {
                    orderItemMapper.insert(item);
                } else {
                    orderItemMapper.updateById(item);
                }
            }
        }

        // 3. Publish domain events via outbox (same transaction)
        order.getDomainEvents().forEach(event -> {
            outboxService.save(event);
            log.debug("Saved domain event to outbox: {}", event.getEventType());
        });

        // 4. Clear events after publishing
        order.clearDomainEvents();

        return order;
    }

    /**
     * Find order by ID with items.
     */
    @Transactional(readOnly = true)
    public Optional<OrdOrder> findById(Long id) {
        OrdOrder order = orderMapper.selectById(id);
        if (order != null) {
            // Load items
            var items = orderItemMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<OrdOrderItem>()
                            .eq("order_id", id)
                            .eq("deleted", false)
            );
            order.setItems(items);
        }
        return Optional.ofNullable(order);
    }

    /**
     * Find order by order number.
     */
    @Transactional(readOnly = true)
    public Optional<OrdOrder> findByOrderNo(String orderNo) {
        OrdOrder order = orderMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<OrdOrder>()
                        .eq("order_no", orderNo)
                        .eq("deleted", false)
        );
        if (order != null) {
            var items = orderItemMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<OrdOrderItem>()
                            .eq("order_id", order.getId())
                            .eq("deleted", false)
            );
            order.setItems(items);
        }
        return Optional.ofNullable(order);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add scm-order/service/src/main/java/com/scmcloud/order/domain/repository/OrdOrderRepository.java
git commit -m "feat(order): create aggregate repository with event publishing"
```

---

### Task 5: Create Kafka Consumers for Downstream Services

**Files:**
- Create consumers in Finance, Logistics, Notify, Audit services

- [ ] **Step 1: Create OrderEventConsumer in Finance service**

```java
package com.scmcloud.finance.consumer;

import com.scmcloud.order.domain.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    @KafkaListener(topics = "order-events", groupId = "finance-service")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for order: {}", event.getAggregateId());
        
        // Calculate freight based on order items
        // This is where freight calculation logic goes
        
        log.info("Freight calculated for order: {}", event.getAggregateId());
    }
}
```

- [ ] **Step 2: Create OrderEventConsumer in Logistics service**

```java
package com.scmcloud.logistics.consumer;

import com.scmcloud.order.domain.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    @KafkaListener(topics = "order-events", groupId = "logistics-service")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for order: {}", event.getAggregateId());
        
        // Create waybill for order shipment
        
        log.info("Waybill created for order: {}", event.getAggregateId());
    }
}
```

- [ ] **Step 3: Create OrderEventConsumer in Notify service**

```java
package com.scmcloud.notify.consumer;

import com.scmcloud.order.domain.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    @KafkaListener(topics = "order-events", groupId = "notify-service")
    public void handleOrderEvent(DomainEvent event) {
        log.info("Received {} for order: {}", event.getEventType(), event.getAggregateId());
        
        if (event instanceof OrderCreatedEvent) {
            // Send order confirmation notification
        } else if (event instanceof OrderPaidEvent) {
            // Send payment confirmation notification
        } else if (event instanceof OrderCancelledEvent) {
            // Send cancellation notification
        } else if (event instanceof OrderShippedEvent) {
            // Send shipment notification
        }
    }
}
```

- [ ] **Step 4: Create OrderEventConsumer in Audit service**

```java
package com.scmcloud.audit.consumer;

import com.scmcloud.common.domain.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    @KafkaListener(topics = "order-events", groupId = "audit-service")
    public void handleOrderEvent(DomainEvent event) {
        log.info("Received {} for audit: {}", event.getEventType(), event.getAggregateId());
        
        // Log order event to audit trail
        
        log.info("Audit log created for event: {}", event.getEventType());
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add scm-finance/ scm-logistics/ scm-notify/ scm-audit/
git commit -m "feat(order): add Kafka consumers for downstream services"
```

---

### Task 6: Update OrdOrderServiceImpl to Use Aggregate

**Files:**
- Modify: `scm-order/service/src/main/java/com/scmcloud/order/service/impl/OrdOrderServiceImpl.java`

- [ ] **Step 1: Read current OrdOrderServiceImpl**

Read the service implementation to understand current structure.

- [ ] **Step 2: Refactor createOrder to use aggregate**

```java
@Service
@RequiredArgsConstructor
public class OrdOrderServiceImpl implements IOrdOrderService {

    private final OrdOrderRepository orderRepository;
    // ... other dependencies

    @Override
    @Transactional
    public OrdOrder createOrder(CreateOrderRequest request) {
        // 1. Build context
        CreateOrderContext ctx = CreateOrderContext.builder()
                .userId(request.getUserId())
                .username(request.getUsername())
                .orderType(request.getOrderType())
                .orderSource(request.getOrderSource())
                .items(request.getItems())
                .build();

        // 2. Create aggregate (enforces invariants, registers events)
        OrdOrder order = OrdOrder.create(ctx);

        // 3. Save aggregate (persists + publishes events atomically)
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public OrdOrder payOrder(Long orderId, PaymentResult result) {
        // 1. Load aggregate
        OrdOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // 2. Execute domain behavior (enforces invariants, registers events)
        order.pay(result.getAmount(), result.getPaymentNo());

        // 3. Save aggregate
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public OrdOrder cancelOrder(Long orderId, String reason) {
        // 1. Load aggregate
        OrdOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // 2. Execute domain behavior
        order.cancel(reason);

        // 3. Save aggregate
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public OrdOrder shipOrder(Long orderId, String waybillNo, String carrier) {
        // 1. Load aggregate
        OrdOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // 2. Execute domain behavior
        order.ship(waybillNo, carrier);

        // 3. Save aggregate
        return orderRepository.save(order);
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add scm-order/service/src/main/java/com/scmcloud/order/service/impl/OrdOrderServiceImpl.java
git commit -m "refactor(order): use aggregate pattern in OrderServiceImpl"
```

---

### Task 7: Integration Test - Order Lifecycle

**Files:**
- Create: `scm-order/service/src/test/java/com/scmcloud/order/domain/OrdOrderAggregateTest.java`

- [ ] **Step 1: Create unit test for aggregate**

```java
package com.scmcloud.order.domain;

import com.scmcloud.order.domain.entity.OrdOrder;
import com.scmcloud.order.domain.entity.OrderStatus;
import com.scmcloud.order.domain.event.OrderCreatedEvent;
import com.scmcloud.order.domain.event.OrderPaidEvent;
import com.scmcloud.order.domain.event.OrderCancelledEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrdOrderAggregateTest {

    @Test
    void shouldCreateOrderWithEvent() {
        // Given
        CreateOrderContext ctx = CreateOrderContext.builder()
                .userId("user-123")
                .username("testuser")
                .orderType(1)
                .orderSource("WEB")
                .items(List.of(new OrderItemData("sku-1", "Product 1", 2, new BigDecimal("100.00"))))
                .build();

        // When
        OrdOrder order = OrdOrder.create(ctx);

        // Then
        assertNotNull(order);
        assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatusEnum());
        assertEquals(1, order.getDomainEvents().size());
        assertInstanceOf(OrderCreatedEvent.class, order.getDomainEvents().get(0));
    }

    @Test
    void shouldPayOrderWithEvent() {
        // Given
        OrdOrder order = createTestOrder();

        // When
        order.pay(new BigDecimal("200.00"), "PAY-123");

        // Then
        assertEquals(OrderStatus.PAID, order.getStatusEnum());
        assertEquals("PAY-123", order.getPaymentNo());
        assertTrue(order.getDomainEvents().stream()
                .anyMatch(e -> e instanceof OrderPaidEvent));
    }

    @Test
    void shouldCancelOrderWithEvent() {
        // Given
        OrdOrder order = createTestOrder();

        // When
        order.cancel("Customer request");

        // Then
        assertEquals(OrderStatus.CANCELLED, order.getStatusEnum());
        assertEquals("Customer request", order.getCancelReason());
        assertTrue(order.getDomainEvents().stream()
                .anyMatch(e -> e instanceof OrderCancelledEvent));
    }

    @Test
    void shouldNotCancelShippedOrder() {
        // Given
        OrdOrder order = createTestOrder();
        order.pay(new BigDecimal("200.00"), "PAY-123");
        order.ship("WB-123", "Carrier");

        // When & Then
        assertThrows(IllegalStateException.class, () -> order.cancel("Too late"));
    }

    private OrdOrder createTestOrder() {
        CreateOrderContext ctx = CreateOrderContext.builder()
                .userId("user-123")
                .username("testuser")
                .orderType(1)
                .orderSource("WEB")
                .items(List.of(new OrderItemData("sku-1", "Product 1", 2, new BigDecimal("100.00"))))
                .build();
        return OrdOrder.create(ctx);
    }
}
```

- [ ] **Step 2: Run the test**

```bash
mvn test -pl scm-order/service -Dtest=OrdOrderAggregateTest -f com.scm.parent/pom.xml
```

Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add scm-order/service/src/test/java/com/scmcloud/order/domain/OrdOrderAggregateTest.java
git commit -m "test(order): add unit tests for order aggregate"
```

---

### Task 8: Build Verification

- [ ] **Step 1: Run full build**

```bash
mvn clean install -DskipTests -f com.scm.parent/pom.xml
```

Expected: BUILD SUCCESS

- [ ] **Step 2: Run tests**

```bash
mvn test -f com.scm.parent/pom.xml
```

Expected: All tests pass

- [ ] **Step 3: Verify no regressions**

Check that existing functionality still works.

---

## Summary

| Task | Description | Status |
|------|-------------|--------|
| T035 | Verify DomainEvent infrastructure | PENDING |
| T036 | Enrich OrdOrder as aggregate root | PENDING |
| T037 | Create domain event classes | PENDING |
| T038 | Create OrdOrderRepository | PENDING |
| T039 | Create Kafka consumers | PENDING |
| T040 | Update OrdOrderServiceImpl | PENDING |
| T041 | Integration tests | PENDING |
| T042 | Build verification | PENDING |

**Total Estimated Effort:** 8-10 days

**Priority:** P1 - Start in Week 9

**Dependencies:**
- Transactional outbox (already exists)
- Kafka infrastructure (already exists)

Here is my independent formal audit, drawing directly from your codebase (docs 1–162). I'll lead with a structural overview, then work through all 14 questions, and close with the required output sections.

------

## Audit — 14 Questions

### 1. Domain area division

**Verdict: structurally sound at the macro level; one critical internal fracture.**

The 15-service boundary maps coherently to supply chain value-chain stages: `product → inventory → order → warehouse → logistics` forms a clean directional graph. Cross-cutting concerns (auth, system, tenant, audit, notify, approval) are correctly separated. No circular Dubbo dependencies were found in the API interfaces.

The fracture is `scm-common`. Reading `pom.xml` (doc 26) reveals it contains data access (`mybatis-plus`, `shardingsphere`), web security (`JWT`, Spring Security filters), integration messaging (Kafka + RabbitMQ), monitoring, and two security sub-modules — seven conceptually independent domains crammed into one artifact. Every service transitively depends on it. Any change rebuilds all 15 deployables.

### 2. Microservice split

**Verdict: business services well-sized; three cross-cutting services are gratuitously thin.**

`scm-purchase` justifiably has 15 DB tables and a complex procurement approval workflow. `scm-order` manages 5 partitioned tables with a state machine lifecycle. `scm-warehouse` has wave-picking and complex inbound/outbound flows. These earn their JVM processes.

`scm-approval` (one Dubbo interface, `ApprovalDubboService` with 4 methods), `scm-audit` (one Dubbo interface), and `scm-notify` (one Dubbo interface, `NotifyDubboService` with 2 methods) do not. Each one adds: a JVM cold-start, a Nacos registration, a Dubbo registry entry, an independent CI/CD pipeline, and a PostgreSQL database — for what amounts to a few hundred lines of business logic.

### 3. Excessive fragmentation

**Yes — in the cross-cutting layer only.**

The three thin services should be folded into `scm-system` as Spring `@Configuration`-bounded modules. Their Dubbo API interfaces (`AuditDubboService`, `NotifyDubboService`, `ApprovalDubboService`) can and should remain as separate JAR artifacts — that separation of contract from deployment is the correct pattern. What must change is the deployment unit, not the API contract.

### 4. Aggregate boundary

**Verdict: there are no aggregates. Every entity is an independent database row.**

Examining the DB schemas (docs 139–162), `ord_order` and `ord_order_item` are inserted by independent service calls with no parent-enforces-children semantics. `wms_inbound` and `wms_inbound_item` are similarly disconnected. `prod_spu` has no control over `prod_sku` lifecycle. `InventoryTccService` (doc 104) wraps stock deduction at the DB function level, not through an aggregate root. Nothing in the Java layer prevents saving an `OrdOrder` with zero items, or a `ProdSku` pointing to a non-existent `ProdSpu`.

The `inv_inventory` constraint `chk_inv_stock_balance` (`total_stock = available_stock + locked_stock + damaged_stock`) in doc 154 is the only invariant enforcement — and it lives at the database level, not in Java. That is the entire domain invariant catalogue for 15 services.

### 5. Is DDD real?

**No. This is Transaction Script architecture with `domain/` folder naming.**

The evidence is unambiguous: every entity is a Lombok `@Data` POJO. Domain logic lives in `*ServiceImpl` classes that extend MyBatis-Plus `ServiceImpl<Mapper, Entity>`. There are no value objects — `Money` is `BigDecimal`, `Address` is `JSONB`, `Quantity` is `Integer`. There are no domain events. The only behavior on any entity is `WebauthnCredential.isAvailable()`, `isCounterValid()`, and `updateUsage()` (doc 17) — which are actually good examples of what the entire domain layer should look like, but they exist as isolated exceptions.

The `SysAuthServiceImpl` (doc 15) is the closest thing to a domain service — its login flow, MFA verification, TOTP replay protection, and device tracking are correctly placed in the auth bounded context. But even it calls MyBatis-Plus directly rather than through a repository interface.

### 6. Is CQRS worth full promotion?

**Partial CQRS in `scm-system` is the right call. Full event-sourced CQRS is not recommended.**

The read/write split for cross-database user-permission queries makes sense. Full CQRS with separate read models would introduce: eventual-consistency windows that are unacceptable on inventory deduction and order creation, projection rebuilding complexity, and an event store that doesn't yet exist. The current Seata AT/TCC approach gives synchronous consistency where it matters. CQRS should be applied only to reporting and analytics surfaces — never to the order-inventory transaction path.

### 7. Multi-tenancy design

**Verdict: correct architecture; four implementation gaps are production-blockers.**

The design is thoughtful: `TenantContextHolder` (ThreadLocal), `TenantFilter` (HTTP extraction), `TenantInterceptor` (MyBatis SQL injection), `@DS` annotation routing, and a full `scm-tenant` module with `TenantPackage`, `TenantSubscription`, `TenantResourceQuota`, and `TenantFeature`. The migration script (doc 141) adds `tenant_id` columns to all business tables correctly.

The four gaps:

**Gap 1 (P0):** `TenantInterceptor` is a MyBatis plugin that appends `WHERE tenant_id = ?` to SELECT statements. If any business table is missing the `tenant_id` column, this silently generates invalid SQL — the error surfaces only at runtime under load.

**Gap 2 (P0):** INSERT statements are not intercepted. Developers must manually set `tenant_id` on every entity before saving. This is a guaranteed source of data leakage under normal team velocity.

**Gap 3 (P0):** `ThreadLocal` loses tenant context in `@Async` methods, `CompletableFuture.supplyAsync()`, and Dubbo's thread pool. A cross-service call from Order to Inventory via Dubbo (doc 96 → doc 124) arrives with a null tenant context in the receiving service.

**Gap 4 (P0):** The two-level cache (`TwoLevelCacheManager`) uses keys that do not include `tenant_id`. A cached permission tree for tenant A is served to tenant B if they share a user with the same ID.

### 8. Seata suitability

**Verdict: AT + TCC coexistence is correct for this scale. Two precision bugs need fixing.**

The `InventoryTccService` (doc 104) correctly models the three phases. `inv_tcc_reservation` (doc 158) has `business_key` for idempotency, proper `TRYING/CONFIRMED/CANCELLED` states, and separate timestamps for each phase. The `undo_log` tables are correctly provisioned per-database (doc 139).

**Bug 1:** `inv_tcc_reservation` uses `BIGSERIAL PRIMARY KEY` with a `COMMENT` clause (doc 158, line `id BIGSERIAL PRIMARY KEY COMMENT '...'`). PostgreSQL does not support inline `COMMENT` in `CREATE TABLE` — this DDL will fail on PostgreSQL. The audit confirms the production database is PostgreSQL (doc 125, `org.postgresql.Driver`).

**Bug 2:** The Sentinel degrade rule (doc 128) targets `"resource": "userService"` with `"grade": 0` (slow call ratio) and `"count": 1000` (1-second response threshold). At 1000ms, this is a last-resort circuit breaker, not a proactive one. The practical P99 for a Dubbo call should degrade at 200–500ms.

**Scale concern:** At million-order volume, Seata AT's global row-lock on `inv_inventory` creates contention. The TCC reservation path avoids this correctly, but any AT-mode branch touching the same inventory row will block. The transactional outbox + Kafka saga pattern should replace Seata AT on the hot order-inventory path at scale.

### 9. Kafka and RabbitMQ responsibility

**Verdict: conceptually sound separation; one duplication and one missing idempotency check.**

Kafka is used for high-throughput ordered streams: PostgreSQL CDC via Debezium, cross-database data sync (`KafkaDataSyncPublisher`). RabbitMQ is used for reliable transactional delivery: login audit events (`UserLoginEventProducer`), notification dispatch. The `MessageEnvelope<T>` wrapper (traceId, tenantId, version) and `ReliableMessagePublisher` with publisher confirms are production-grade patterns.

**Duplication:** Both `UserLoginEventProducer` (RabbitMQ) and a Kafka producer handle login events. One must be removed. Given the audit trail requirement (exactly-once delivery matters more than throughput), RabbitMQ with publisher confirms is the correct choice.

**Missing check:** The `InstrumentedKafkaConsumer` does not call `IdempotencyChecker`, while the RabbitMQ consumer (`InstrumentedMessageConsumer`) does. Kafka consumers can receive duplicates (rebalance, at-least-once semantics). This gap must be closed.

### 10. PostgreSQL + ShardingSphere

**Verdict: PostgreSQL is the right choice. ShardingSphere is dead code and must be removed.**

`application-sharding.yaml` (doc 3) and `application-readwrite.yaml` (doc 4) reference `com.mysql.cj.jdbc.Driver` and `jdbc:mysql://192.168.18.133:3306/...`. Production auth service (doc 125) uses `org.postgresql.Driver` with `jdbc:postgresql://`. These configs are orphaned MySQL artifacts. The production routing is done by baomidou `dynamic-datasource` with `@DS` annotations — proven, simple, correct for the current scale.

PostgreSQL range partitioning on `create_time` (orders, audit logs, purchase orders, logistics tracking — all correctly partitioned in docs 150–162) gives the same query pruning that ShardingSphere horizontal sharding would provide, without the middleware complexity. Remove ShardingSphere entirely. Add it back only after you can demonstrate a write bottleneck on a single PostgreSQL instance, which at realistic SCM volumes (sub-million orders/day) is unlikely to occur within 3 years.

### 11. SaaS readiness

**Verdict: the skeleton is correct; the muscle is missing.**

The tenant module, feature flags, quota management, and database-level isolation design are all present and well-structured. The `fn_check_tenant_quota` PostgreSQL function (doc 162) handles quota enforcement atomically. The subscription and package models (`tenant_package`, `tenant_subscription`) cover the billing dimension.

What blocks SaaS go-live: the four gaps from question 7, plus no tenant provisioning API (creating a new tenant requires manual DB seeding, Nacos config entry, and default role setup), no metering integration against `TenantResourceQuota`, and no tenant-scoped cache key strategy.

### 12. Million-order scale readiness

**Verdict: not ready. Three architectural constraints form a hard ceiling.**

**Constraint 1 — Seata AT global lock:** At 10,000 orders/minute, concurrent AT-mode branches locking the same `inv_inventory` row will queue. Redis Lua atomic deduction (correctly implemented) bypasses this for the reservation step, but any non-TCC branch reintroduces lock contention.

**Constraint 2 — Synchronous Dubbo chain:** The order creation path calls inventory synchronously over Dubbo inside a Seata global transaction. End-to-end latency = sum of all branch latencies. Any slow branch (network jitter, cold JVM, GC pause) blocks the entire order creation thread.

**Constraint 3 — `TenantInterceptor` SQL parsing:** JSqlParser parses every SQL statement at runtime to inject `WHERE tenant_id = ?`. At 10,000 queries/second this adds measurable CPU overhead. The correct approach is compile-time query parameterization via MyBatis `@Param`, not runtime SQL rewriting.

The Redis + Lua stock deduction, PostgreSQL range partitioning, and TCC reservation pattern are all correctly designed for scale. The bottleneck is the synchronous transaction boundary, not the data layer.

### 13. Biggest technical debt

**The entire domain layer is absent — 80+ anemic POJOs with zero behavior.**

This is not a stylistic preference. The practical consequences are: order cancellation logic exists in at least four places (`OrderTccServiceImpl`, `OrderTimeoutCancelJobHandler`, `OrderDubboServiceImpl`, and wherever the refund flow touches it). Adding a new invariant (e.g. "orders over ¥50,000 require manager approval before cancellation") requires finding and modifying all four, with no compile-time guarantee of completeness. Unit testing any business rule requires a running Spring context. The state machine for order status is implicit — encoded in if-else chains across service impls rather than in `OrdOrder.canTransitionTo(OrderStatus next)`.

### 14. Most dangerous structural decision

**`TenantInterceptor` injecting `WHERE tenant_id = ?` at the SQL layer, with no compile-time guarantee that target tables have the column.**

If any business table is missing `tenant_id` — or if a developer writes a native query that bypasses MyBatis — the interceptor either produces a SQL error (visible failure) or, worse, is bypassed silently (invisible data leakage across tenants). At SaaS scale, a single tenant data leak is a GDPR incident. The correct fix is mandatory entity-level enforcement: a `TenantAwareEntity` base class with a non-null `tenant_id` field, validated by a MyBatis `MetaObjectHandler` before every INSERT.

---

## Advantages

The infrastructure layer is genuinely production-grade and shows architectural sophistication:

The security posture is exceptional for a v1 SCM platform. mTLS between gateway and auth (`auth-keystore.p12`, `truststore.p12` in doc 2), HMAC-SHA256-V2 API signature with nonce replay protection (doc 97), JWT refresh token rotation, TOTP with replay protection via Redis `SETNX` (doc 15), WebAuthn/passkey with webauthn4j signature counter validation (doc 23) — these are not commonly found together in enterprise Java projects.

The observability stack is comprehensive: distributed tracing (SkyWalking), structured metrics (Prometheus + Grafana with business dashboards in doc 34), centralized logging with PII masking in Logstash (doc 84 masks phone numbers and ID cards), and Sentinel circuit breaking with Nacos-backed rule push (doc 125). This gives an operations team genuine visibility.

The messaging infrastructure is solid: `MessageEnvelope<T>` provides CloudEvents-like envelope semantics, `ReliableMessagePublisher` implements publisher confirms with retry, `RetryableEventProcessor` has Redis idempotency and exponential backoff with DLQ, and `KafkaDataSyncPublisher` has OpenTelemetry tracing and Prometheus counters per event type.

The inventory hot path is correctly designed: Redis Lua atomic stock deduction, TCC for reservation, 15-minute auto-expiry via Redis TTL, and `fn_deduct_stock` as a PostgreSQL fallback with optimistic locking.

Two-level caching (`TwoLevelCacheManager`: Caffeine L1 + Redis L2 with pub/sub cross-instance invalidation) prevents cache stampede on permission trees and product catalogs.

## Risks

| # | Risk | Category | Severity |
|---|---|---|---|
| R1 | `TenantInterceptor` vs tables without `tenant_id` column | Data integrity | Critical |
| R2 | INSERT does not auto-populate `tenant_id` | Data leakage | Critical |
| R3 | `ThreadLocal` tenant context lost in async + Dubbo | Multi-tenancy | Critical |
| R4 | Tenant-unaware cache keys — cross-tenant cache poisoning | Data leakage | Critical |
| R5 | Anemic domain — business rules scattered, no invariants | Architecture | High |
| R6 | Seata AT global lock contention at scale | Performance | High |
| R7 | `scm-common` god module — any change triggers full rebuild | Build/deploy | High |
| R8 | ShardingSphere MySQL configs in a PostgreSQL system | Operational confusion | Medium |
| R9 | `inv_tcc_reservation` DDL uses MySQL `COMMENT` syntax — fails on PostgreSQL | Correctness | Medium |
| R10 | Duplicate `UserLoginEvent` on both Kafka and RabbitMQ | Correctness | Medium |
| R11 | Kafka consumer has no idempotency check | Reliability | Medium |
| R12 | Sentinel degrade threshold at 1000ms — too late | Resilience | Medium |
| R13 | Three thin services (approval, audit, notify) — disproportionate operational cost | Operations | Low |

## Technical Debt

| Debt item | Effort | Unlock |
|---|---|---|
| Tenant context propagation (`TaskDecorator` + Dubbo filter + cache key prefix) | 2–3 weeks | SaaS go-live |
| `TenantAwareEntity` base class + `MetaObjectHandler` auto-populate `tenant_id` | 1 week | SaaS go-live |
| Verify all business tables carry `tenant_id` (automated schema check in CI) | 3 days | SaaS go-live |
| Decompose `scm-common` into `scm-security`, `scm-messaging`, `scm-cache`, `scm-data-rw` | 3–4 weeks | Build speed + ownership |
| Remove ShardingSphere + dead MySQL configs | 3 days | Reduced confusion |
| Fix `inv_tcc_reservation` DDL (`COMMENT` → `COMMENT ON COLUMN`) | 1 day | Seata TCC correctness |
| Merge approval/audit/notify into `scm-system` as modules | 2–3 weeks | Operational simplicity |
| Resolve duplicate `UserLoginEvent` — keep RabbitMQ path | 1 day | Correctness |
| Add idempotency to Kafka consumer path | 1 week | Reliability |
| Introduce `OrdOrder` as a rich aggregate root with `cancel()`, `pay()`, `ship()` | 6–8 weeks | Domain clarity |
| Introduce value objects: `Money`, `Quantity`, `TenantId` | 2 weeks | Type safety |
| Transactional outbox pattern for event publishing | 4–6 weeks | Reliability at scale |
| Replace Seata AT on order-inventory hot path with Kafka saga | 2–3 months | Million-order scale |

## Architecture Maturity Rating

| Dimension | Score | Notes |
|---|---|---|
| Service boundary definition | 3.8 / 5 | Business services clean; cross-cutting over-split |
| Domain modeling | 1.2 / 5 | 80+ anemic POJOs; WebAuthn entity is a positive outlier |
| Data architecture | 3.5 / 5 | Good partitioning; tenant gaps are critical |
| Distributed transactions | 3.8 / 5 | AT + TCC well-structured; PostgreSQL DDL bug; scale ceiling |
| Messaging | 3.6 / 5 | Strong infrastructure; duplication and missing idempotency |
| Caching | 3.5 / 5 | Two-level with invalidation; no tenant scoping |
| Security | 4.4 / 5 | Best-in-class for this stage; mTLS + WebAuthn + TOTP |
| Observability | 4.2 / 5 | Three-layer coverage; PII masking; business dashboards |
| Multi-tenancy | 2.1 / 5 | Design is right; implementation has four P0 gaps |
| Scalability | 2.0 / 5 | Single-database, synchronous AT, anemic model = hard ceiling |
| Operational maturity | 3.0 / 5 | Docker Compose + CI; no tenant provisioning automation |

**Overall: 3.2 / 5 — "Developing."** The infrastructure and security layers are ahead of their class. The domain layer and multi-tenancy implementation are the two areas holding the platform below production-SaaS readiness.

---

## P0 / P1 / P2 Priority Issue List

**P0 — Must resolve before any SaaS tenant onboarding**

| # | Issue | Effort |
|---|---|---|
| P0-1 | Add `tenant_id NOT NULL` to every business table that is missing it; add CI schema validation | 1–2 weeks |
| P0-2 | Implement `TenantAwareEntity` base class; `MyMetaObjectHandler` auto-sets `tenant_id` on INSERT | 1 week |
| P0-3 | Implement `TenantAwareMDCTaskDecorator` for all `@Async` thread pools | 1 week |
| P0-4 | Implement Dubbo `Filter` that reads `RpcContext` attachment and restores `TenantContextHolder` | 1 week |
| P0-5 | Prefix all cache keys with `tenantId:` in `TwoLevelCacheManager` | 3 days |
| P0-6 | Fix `inv_tcc_reservation` DDL: replace MySQL `COMMENT` column syntax with `COMMENT ON COLUMN` | 1 day |

**P1 — Resolve within 90 days**

| # | Issue | Effort |
|---|---|---|
| P1-1 | Decompose `scm-common` into four focused libraries | 3–4 weeks |
| P1-2 | Merge `scm-approval`, `scm-audit`, `scm-notify` deployables into `scm-system` | 2–3 weeks |
| P1-3 | Delete all ShardingSphere YAML configs and `shardingsphere-jdbc` dependency | 3 days |
| P1-4 | Remove duplicate `UserLoginEvent` producer — keep RabbitMQ path, delete Kafka path | 1 day |
| P1-5 | Add `IdempotencyChecker` call to `InstrumentedKafkaConsumer` | 1 week |
| P1-6 | Tighten Sentinel degrade threshold from 1000ms to 300ms for Dubbo calls | 1 day |
| P1-7 | Add `MybatisPlusConfig` PostgreSQL dialect (`DbType.POSTGRE_SQL`) — currently MySQL | 1 day |
| P1-8 | Implement tenant provisioning API: creates DB schema, seeds roles, registers Nacos config | 3–4 weeks |

**P2 — Resolve within 12 months**

| # | Issue | Effort |
|---|---|---|
| P2-1 | `OrdOrder` as rich aggregate root: `cancel()`, `pay()`, `ship()`, `canTransitionTo()` | 6–8 weeks |
| P2-2 | Value objects: `Money(BigDecimal amount, Currency currency)`, `Quantity`, `TenantId` | 2 weeks |
| P2-3 | Domain events: `OrderCreatedEvent`, `OrderCancelledEvent`, `StockDeductedEvent` | 4 weeks |
| P2-4 | Transactional outbox: events written in same transaction as entity, polled to Kafka | 4–6 weeks |
| P2-5 | Replace Seata AT on order-inventory path with Kafka saga + compensation | 2–3 months |
| P2-6 | Metering: instrument `TenantResourceQuota` usage counters against actual API calls | 3–4 weeks |
| P2-7 | Cache warming on service startup for permission trees and role hierarchies | 1–2 weeks |

---

## 12-Month Development Roadmap

**Quarter 1 (months 1–3): make multi-tenancy production-safe**

The five P0 items and tenant provisioning (P1-8) are existential for SaaS. None of the other investments matter if tenant data leaks or the tenant context is silently dropped in async flows. Delivery sequence: P0-6 (one day, unblock Seata TCC) → P0-3 and P0-4 together (async + Dubbo propagation, two weeks) → P0-2 (MetaObjectHandler, one week) → P0-1 (schema audit and CI gate, two weeks) → P0-5 (cache key prefix, three days) → P1-8 (tenant provisioning API, four weeks). Ship nothing to a real tenant until all P0 items are green in CI.

**Quarter 2 (months 4–6): reduce structural complexity**

Decompose `scm-common` (P1-1) using the strangler-fig pattern: extract `scm-security-core` first (it is the most stable), then `scm-messaging`, then `scm-cache`. Each extraction is a Maven module refactor with no behavioral change. Merge the three thin deployables (P1-2) by converting them to Spring `@Configuration` classes inside `scm-system-service`, keeping their Dubbo API JAR contracts untouched. Delete ShardingSphere (P1-3) in the same sprint. By the end of Q2 the deployable count drops from 15 to 12 and the common module count drops from 7 to 4.

**Quarter 3 (months 7–9): begin domain enrichment**

Start with `OrdOrder` (P2-1). Move `cancel()`, `pay()`, `confirmReceipt()`, and `isTransitionValid(from, to)` into the entity. This is not a big-bang rewrite — each method is extracted from `OrdOrderServiceImpl` one at a time, with the service calling `order.cancel()` instead of duplicating the guard logic. Add `Money` and `Quantity` value objects (P2-2) while touching the order domain. Introduce domain events (P2-3) using Spring `ApplicationEventPublisher` — start with `OrderCreatedEvent` and `OrderCancelledEvent`. The event listeners can initially do what the direct service calls do today; the decoupling value comes later.

Implement the transactional outbox (P2-4) in parallel: a single `outbox` table written in the same DB transaction as the entity, a scheduled poller publishing to Kafka. This eliminates the dual-write risk on `KafkaDataSyncPublisher` and `UserLoginEventProducer`.

**Quarter 4 (months 10–12): scale and SaaS hardening**

Evaluate Seata AT removal on the order-inventory path (P2-5). If load testing shows global lock contention above 5,000 orders/hour, migrate to a Kafka saga: `OrderService` publishes `OrderCreatedEvent` to the outbox → `InventoryService` deducts stock and publishes `StockDeductedEvent` → `OrderService` confirms or compensates. This is a 2–3 month engineering effort and should only be started if the Seata bottleneck is proven by measurement, not assumed.

Instrument `TenantResourceQuota` (P2-6): every API request increments `current_api_calls_today` via Redis `INCR` with daily expiry; a Lua script checks the quota atomically before incrementing. Plug this into a Spring `HandlerInterceptor`. This enables real-time quota enforcement and the metering data needed for usage-based billing.

Close the year with load testing at 5× projected peak volume. The partition strategy, Redis throughput, and the new saga boundary all need empirical validation before declaring million-order readiness.

All three of your corrections are valid. Let me update the audit position on each.---

## Correction 1 — services stay independent

My metric was wrong. Controller count is a current-state measure. You are designing to growth trajectory.

**Notify.** Template management, channel routing (SMS/Email/DingTalk/WeCom/Webhook/Telegram), retry scheduling, quiet-hour enforcement, batching, and delivery receipts make this an orchestration service in its own right. Peak notification volume during a flash sale will exceed order volume by an order of magnitude — every order generates 3–5 downstream notifications. Co-deploying it into `scm-system` would make it the bottleneck it needs to avoid becoming.

**Audit.** Connecting to an external SIEM (Splunk, IBM QRadar) or running behavioral anomaly detection on the event stream requires the service to speak SIEM protocols (CEF, LEEF, syslog-TLS) independently of business services. A security audit that lives inside `scm-system` cannot be independently locked down, independently scaled for burst ingestion, or independently given a separate data retention policy.

**Approval.** The moment you embed Flowable or Camunda, the service owns a BPMN runtime with its own thread pool, job executor, and history database. That runtime should not share a JVM with RBAC or user management. Independent deployment also lets you upgrade the workflow engine version without touching `scm-system`.

**Revised position:** keep all three as independent deployables. The correct efficiency measure is not merging JVMs — it is **single-machine deployment in dev/staging** where all three run as a local Docker Compose stack without Kubernetes overhead. Production scales each independently based on its actual traffic profile.

---

## Correction 2 — RPC chain depth is the primary structural danger

You are right that this outranks the anemic model in real operational risk. A deep synchronous Dubbo chain in a supply chain system is not a code quality issue — it is a **reliability topology problem**.

**The failure math.** If each service has 99.9% availability, a 6-hop synchronous chain has 0.999⁶ = 99.4% availability. At 99.5% per service and 8 hops: 0.995⁸ = 96.1%. An order system with 96% availability is not a supply chain system — it is a liability.

**The latency math.** Dubbo over a LAN at p99 is roughly 5–15ms per hop under load. 6 hops = 30–90ms of pure RPC overhead before any business logic runs. Add Seata coordinator round-trips (one per branch registration, one per phase-two commit) and you are at 100–200ms for a transaction that should complete in 20ms.

**The Seata scope problem.** A Seata global transaction that spans 6 services holds row locks in all 6 databases simultaneously until the final commit or rollback. Lock scope = chain depth. A stuck downstream service (GC pause, slow query) holds locks in every upstream database. This is not a Seata problem — it is a chain depth problem that Seata makes visible.

### The 3-hop rule — concrete implementation

**Rule:** no service may make a synchronous Dubbo call to a service that is itself making a synchronous Dubbo call. Maximum synchronous depth from the entry point is 3 hops.

**Enforcement at the framework level** — add this as a Dubbo server-side `Filter`:

```java
@Activate(group = CommonConstants.PROVIDER)
public class HopCountFilter implements Filter {
    private static final String HOP_COUNT_KEY = "rpc-hop-count";
    private static final int MAX_HOPS = 3;

    @Override
    public Result invoke(Invoker<?> invoker, Invocation inv) throws RpcException {
        String raw = RpcContext.getServiceContext()
                               .getAttachment(HOP_COUNT_KEY);
        int hops = raw == null ? 1 : Integer.parseInt(raw) + 1;

        if (hops > MAX_HOPS) {
            throw new RpcException("RPC chain depth exceeded: " + hops
                + " hops. Max allowed: " + MAX_HOPS
                + ". Caller: " + inv.getServiceName());
        }

        // propagate incremented count to downstream
        RpcContext.getClientAttachment()
                  .setAttachment(HOP_COUNT_KEY, String.valueOf(hops));
        return invoker.invoke(inv);
    }
}
```

Register in `META-INF/dubbo/org.apache.dubbo.rpc.Filter`.

**What happens beyond hop 3.** The transaction commits or rolls back, then an `OrderCreatedEvent` goes to Kafka via the transactional outbox. Finance calculates freight, Logistics creates the waybill, Notify dispatches confirmation, Audit logs the operation — all asynchronously and independently. A failure in any of these does not roll back the order. Where exactly-once guarantees are needed (e.g. freight calculation must happen exactly once), the consumer uses the existing `IdempotencyChecker` pattern already in the messaging layer.

**Practical chain design for order placement:**

```
Hop 0  Gateway → Order
Hop 1  Order   → Inventory  (TCC reserve — must be sync, Seata scope)
Hop 2  Order   → Warehouse  (locate stock for the reservation)
────────────────────────────────────── commit Seata tx, emit event
Async  Kafka   → Finance    (freight, settlement)
Async  Kafka   → Logistics  (waybill creation)
Async  Kafka   → Notify     (order confirmation)
Async  Kafka   → Audit      (operation log)
```

Product detail (name, price snapshot, supplier) must be denormalized into `ord_order_item` at creation time — the snapshot pattern (already in the schema, `sku_name`, `attributes`, `selling_price` columns in `ord_order_item`) handles this. There is no need for a hop to Product or Supplier during order creation. The data was already fetched when the user viewed the product page.

---

## Correction 3 — UUID deserves attention

You are right to flag this. The specific risk depends on which UUID strategy is actually in use.

**What the codebase is doing.** The `UUIDv7Util.generate()` call appears in `SysAuthController` (doc 11). UUID v7 is time-ordered (Unix timestamp in the high bits), which eliminates the primary fragmentation concern with UUID v4 — inserts are monotonically increasing within a time window, so B+tree page splits are sequential rather than random. This is materially different from UUID v4.

**Where UUID v7 still loses to Snowflake BIGINT:**

| Property | UUID v7 | Snowflake BIGINT |
|---|---|---|
| Storage per value | 16 bytes | 8 bytes |
| Index node density | ~half of BIGINT | Full |
| VACUUM pressure at 100M rows | Moderate | Low |
| FK join cost | Higher (128-bit compare) | Lower (64-bit compare) |
| Human readability in logs | Poor | Acceptable |
| Global uniqueness without coordination | Yes | Requires sequence node ID |

**The real problem to watch.** `webauthn_credential` (doc 151) uses `credential_id VARCHAR(512)` as a unique key — not the UUID primary key — with up to 512 bytes per value. At scale, that index is more dangerous than any UUID primary key. Similarly, the `undo_log` table (doc 139) uses `xid VARCHAR(128)` as a composite primary key component — that index is written and deleted under every distributed transaction.

**Recommendation.** Do not do a wholesale migration — the cost exceeds the benefit at current scale. Instead:

1. Continue UUID v7 for all new tables. The time-ordering property keeps fragmentation acceptable.
2. Switch the three highest-write tables — `ord_order`, `inv_log`, `sys_audit_log` — to `BIGINT GENERATED BY DEFAULT AS IDENTITY` (PostgreSQL native) or Snowflake if you ever see VACUUM lag on those specific tables. These are append-only or near-append-only, so foreign key reference count is low and the migration is cheaper there than on tables with heavy FK relationships.
3. Set `autovacuum_vacuum_scale_factor = 0.01` and `autovacuum_analyze_scale_factor = 0.005` on the three partitioned high-write tables now, regardless of ID type. The default PostgreSQL autovacuum thresholds (20% of table) are calibrated for small tables and will fall behind at 50M+ rows.
4. Monitor `pg_stat_user_tables.n_dead_tup` and `pg_stat_bgwriter.checkpoint_write_time` before deciding whether a migration is warranted. Let measurement drive the decision rather than assumption.

---

## Revised risk register (top 5 reordered)

| Rank | Risk | Was |
|---|---|---|
| 1 | **RPC chain depth** — synchronous Dubbo chains beyond 3 hops; cascading timeout risk | Not listed |
| 2 | Tenant context lost in async and Dubbo threads | P0-3, P0-4 |
| 3 | INSERT does not auto-populate `tenant_id` | P0-2 |
| 4 | Tenant-unaware cache keys | P0-5 |
| 5 | Anemic domain model | P2-1 |

The chain depth problem moves to rank 1 because it is a **runtime reliability failure** at any meaningful order volume — not a design quality issue that degrades gradually. The others are correctness gaps that can be patched before a customer hits them. A 6-hop synchronous chain under Black Friday load will produce cascading timeouts on day one of real traffic.
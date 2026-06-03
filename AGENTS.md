# AGENTS.md

## Build & Run

Parent POM: `com.scm.parent/pom.xml` — never use repo root pom. Always `-f com.scm.parent/pom.xml`.

```bash
mvn clean install -f com.scm.parent/pom.xml                     # full build
mvn clean install -DskipTests -f com.scm.parent/pom.xml          # fast
mvn clean package -pl scm-order/service -am -f com.scm.parent/pom.xml  # single module
mvn test -pl scm-order/service -f com.scm.parent/pom.xml         # single module tests
mvn test -Dtest=OrderServiceTest -pl scm-order/service -f com.scm.parent/pom.xml  # single test
mvn verify -f com.scm.parent/pom.xml                             # full CI check (tests + jacoco)
```

Services start order: `docker-compose up -d` → Gateway (8761) → Auth (8106) → System (8081) → business services (any order).

## Key Ports

| Service | Port |
|---------|------|
| Gateway / Sentinel | 8761 / 8858 |
| Auth | 8106 |
| System | 8081 |
| Product/Inventory/Order/Warehouse/Logistics/Supplier | 8201-8206 |

## Package Naming

Actual base: `com.scmcloud.{module}` — e.g., `com.scmcloud.order.controller`, `com.scmcloud.common.response.ApiResponse`.
GroupId: `com.scmcloud` (not `com.frog`).

## Module Layout

- **Business services**: `scm-{name}/api/` (Dubbo RPC interfaces) + `scm-{name}/service/` (implementation)
- **Flat modules (no api/service split)**: `scm-auth`, `scm-gateway`
- **Common**: `scm-common/` — `core`, `data`, `web`, `monitoring`, `integration`, `security/core`, `security/api`

## Critical Patterns

### Multi-Tenant Routing
`@DS("user"|"org"|"permission")` (baomidou dynamic-datasource) → routes to `db_user`, `db_org`, `db_permission`. Context in `TenantContextHolder`.

### Read-Write Separation
`@Master` / `@Slave` annotations from `scm-common/data`. Query services → `@Slave`, command services → `@Master`.

### Distributed Transactions
Seata AT mode via `@GlobalTransactional`. Every database needs `undo_log` table (see `scripts/db/microservices/020_undo_log_tables.sql`).

### CQRS in scm-system
Cross-database ops use CQRS services under `service/query/` and `service/command/`. **Do NOT use** the deprecated `CrossDatabaseQueryService`.

### Inventory Hot Path
Redis Lua scripts for atomic stock deduction — never query PostgreSQL in hot path. Stock reservations auto-release after 15 minutes.

### Caching
Two-level: Caffeine (L1, 5-min TTL) → Redis (L2, 30-min TTL). Inventory: Redis only, short TTL (~30s).

### Order State Machine
Spring State Machine: `PENDING_PAYMENT → PAID → PENDING_SHIP → SHIPPED → IN_TRANSIT → DELIVERED → COMPLETED`, with `CANCELLED` from `PENDING_PAYMENT` or `PAID`.

### Partitioned Tables (PostgreSQL)
UNIQUE constraints MUST include the partition key column:
- `ord_order(order_no, create_time)`, `inv_reservation(reservation_no, reserved_at)`, `sup_purchase_order(purchase_no, create_time)`
- App-level uniqueness enforcement via Redis (e.g., order number generation preventing duplicates across partitions).

### Idempotency
Critical ops (inventory deduction, order creation) use request IDs stored in Redis with 24h expiry.

## Database

```bash
# Windows
set PGPASSWORD=admin123 && cd scripts\db && init-all-databases.bat
# Linux/Mac
export PGPASSWORD=admin123 && cd scripts/db && ./init-all-databases.sh
```

Scripts: `scripts/db/microservices/` (001-021 SQL). Default: `admin` / `admin123`.

## Environment Variables

All have local-dev defaults. Key vars: `NACOS_SERVER`, `DB_HOST/PORT/USERNAME/PASSWORD`, `REDIS_HOST/PORT/PASSWORD`, `SEATA_SERVER_ADDR`.

## CI/CD

`.github/workflows/maven-build.yml`: Build → Test → JaCoCo → SonarCloud → OWASP → Docker build. Triggers on push to `master` or `develop`.

## What NOT to Do

- No `javax.servlet` — use `jakarta.servlet` (enforced by maven-enforcer-plugin)
- No deprecated `CrossDatabaseQueryService` — use CQRS services (`service/query/`, `service/command/`)
- No PostgreSQL inventory queries in hot paths — use Redis
- No UNIQUE constraints on partitioned tables without the partition key
- No starting business services before Gateway + Auth + System are running

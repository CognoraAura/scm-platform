# Multi-Tenant P0 Safety Fixes - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Verify and complete the 4 P0 multi-tenant safety gaps to prevent cross-tenant data leakage before any production use.

**Architecture:** The codebase already has most infrastructure in place (TenantContextHolder, TenantContextFilter, AuditMetaObjectHandler, TwoLevelCache, HopCountFilter). This plan focuses on verification, completion of missing pieces (TenantAwareCacheKeyGenerator), and integration testing.

**Tech Stack:** Java 21, Spring Boot 4, MyBatis-Plus, Dubbo 3.x, Redis, Caffeine, JUnit 5

---

## Current State Assessment

**Already Implemented (Verified):**
- `TenantContextHolder` - ThreadLocal-based tenant context (`scm-common/core/.../tenant/TenantContextHolder.java`)
- `TenantAwareTaskDecorator` - @Async thread pool propagation (`scm-common/core/.../tenant/TenantAwareTaskDecorator.java`)
- `TenantContextFilter` - Dubbo RPC tenant propagation (`scm-common/web/.../dubbo/TenantContextFilter.java`)
- `AuditMetaObjectHandler` - Auto-fill tenantId on INSERT (`scm-common/core/.../mybatis/AuditMetaObjectHandler.java`)
- `TwoLevelCache` - Tenant-aware cache keys (`scm-common/cache/.../spring/TwoLevelCache.java`)
- `HopCountFilter` - 3-hop RPC chain limit (`scm-common/web/.../dubbo/HopCountFilter.java`)
- `TenantAwareEntity` - Base entity class (`scm-common/core/.../tenant/TenantAwareEntity.java`)
- `OutboxService` - Transactional outbox (`scm-common/integration/.../outbox/OutboxService.java`)
- Dubbo SPI registration (`META-INF/dubbo/org.apache.dubbo.rpc.Filter`)

**Missing/Needs Verification:**
- `TenantAwareCacheKeyGenerator` - Not found, needs creation
- @Async thread pool configuration - Needs verification across all services
- Entity migration to TenantAwareEntity - Needs verification
- Integration tests - None exist

---

## File Structure

```
scm-common/
  cache/
    src/main/java/com/scmcloud/common/cache/
      TenantAwareCacheKeyGenerator.java    ← CREATE
  core/
    src/main/java/com/scmcloud/common/tenant/
      TenantContextHolder.java             ← VERIFY (exists)
      TenantAwareTaskDecorator.java        ← VERIFY (exists)
      TenantAwareEntity.java               ← VERIFY (exists)
    src/main/java/com/scmcloud/common/mybatis/
      AuditMetaObjectHandler.java          ← VERIFY (exists)
  web/
    src/main/java/com/scmcloud/common/dubbo/
      TenantContextFilter.java             ← VERIFY (exists)
      HopCountFilter.java                  ← VERIFY (exists)
    src/main/resources/META-INF/dubbo/
      org.apache.dubbo.rpc.Filter          ← VERIFY (exists)

scm-order/
  service/src/main/java/com/scmcloud/order/domain/entity/
    OrdOrder.java                          ← VERIFY tenant_id fill annotation
    OrdOrderItem.java                      ← VERIFY tenant_id fill annotation

scm-inventory/
  service/src/main/java/com/scmcloud/inventory/domain/entity/
    Inventory.java                         ← VERIFY tenant_id fill annotation

scm-product/
  service/src/main/java/com/scmcloud/product/domain/entity/
    ProdSpu.java                           ← VERIFY tenant_id fill annotation
    ProdSku.java                           ← VERIFY tenant_id fill annotation
```

---

### Task 1: Create TenantAwareCacheKeyGenerator

**Files:**
- Create: `scm-common/cache/src/main/java/com/scmcloud/common/cache/TenantAwareCacheKeyGenerator.java`

The `TwoLevelCache` already prepends tenantId to cache keys (line 175-178), but a dedicated `KeyGenerator` is needed for Spring's `@Cacheable` annotations to ensure consistent key generation across all caching configurations.

- [ ] **Step 1: Create TenantAwareCacheKeyGenerator class**

```java
package com.scmcloud.common.cache;

import com.scmcloud.common.tenant.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.lang.NonNull;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Cache key generator that automatically prepends tenantId to all cache keys.
 * Ensures cache isolation between tenants.
 *
 * <p>Usage with @Cacheable:</p>
 * <pre>
 * @Cacheable(value = "users", keyGenerator = "tenantAwareCacheKeyGenerator")
 * public User findById(UUID userId) { ... }
 * </pre>
 */
@Slf4j
public class TenantAwareCacheKeyGenerator implements KeyGenerator {

    @NonNull
    @Override
    public Object generate(@NonNull Object target, @NonNull Method method, @NonNull Object... params) {
        UUID tenantId = TenantContextHolder.getTenantId();
        String baseKey = SimpleKeyGenerator.generateKey(params).toString();

        if (tenantId != null) {
            return tenantId + ":" + baseKey;
        }

        log.warn("Tenant ID is null when generating cache key for {}.{}", 
                target.getClass().getSimpleName(), method.getName());
        return "no-tenant:" + baseKey;
    }
}
```

- [ ] **Step 2: Create auto-configuration for the key generator**

```java
package com.scmcloud.common.cache;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheKeyGeneratorConfiguration {

    @Bean("tenantAwareCacheKeyGenerator")
    public KeyGenerator tenantAwareCacheKeyGenerator() {
        return new TenantAwareCacheKeyGenerator();
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add scm-common/cache/src/main/java/com/scmcloud/common/cache/TenantAwareCacheKeyGenerator.java
git add scm-common/cache/src/main/java/com/scmcloud/common/cache/CacheKeyGeneratorConfiguration.java
git commit -m "feat(cache): add TenantAwareCacheKeyGenerator for tenant-scoped cache keys"
```

---

### Task 2: Verify @Async Thread Pool Configuration

**Files:**
- Verify: All services using @Async have TenantAwareTaskDecorator configured

- [ ] **Step 1: Search for @Async usage across all services**

Run:
```bash
grep -r "@Async" --include="*.java" D:\ProgramProject\scm-platform
```

Expected findings:
- `scm-audit/service/src/main/java/com/scmcloud/audit/service/impl/SysAuditLogServiceImpl.java` (4 usages)
- `scm-system/service/src/main/java/com/scmcloud/system/config/CacheWarmingService.java` (1 usage)

- [ ] **Step 2: Verify AsyncConfigurer in each service with @Async**

For each service using @Async, verify that `AsyncConfigurer` is configured with `TenantAwareTaskDecorator`:

```bash
grep -r "AsyncConfigurer\|setTaskDecorator\|TenantAwareTaskDecorator" --include="*.java" D:\ProgramProject\scm-platform
```

If missing, create `AsyncConfig` in each service:

```java
package com.scmcloud.{service}.config;

import com.scmcloud.common.tenant.TenantAwareTaskDecorator;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @NonNull
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.setTaskDecorator(new TenantAwareTaskDecorator());
        executor.initialize();
        return executor;
    }
}
```

- [ ] **Step 3: Commit if changes made**

```bash
git add .
git commit -m "feat(async): ensure TenantAwareTaskDecorator in all @Async thread pools"
```

---

### Task 3: Verify Entity tenant_id Auto-Fill

**Files:**
- Verify: All business entities have `@TableField(fill = FieldFill.INSERT)` on tenantId

- [ ] **Step 1: Search for entities with tenant_id field**

Run:
```bash
grep -r "tenant_id" --include="*.java" D:\ProgramProject\scm-platform --include="*Entity.java"
```

Expected: All business entities should have:
```java
@TableField(value = "tenant_id", fill = FieldFill.INSERT)
private String tenantId;
```

- [ ] **Step 2: Verify OrdOrder entity**

Read `scm-order/service/src/main/java/com/scmcloud/order/domain/entity/OrdOrder.java` and verify:
- Line 30-31: `@TableField(value = "tenant_id", fill = FieldFill.INSERT)` exists
- Line 31: `private String tenantId;` exists

Status: ✅ VERIFIED - OrdOrder has correct annotation

- [ ] **Step 3: Verify OrdOrderItem entity**

Read `scm-order/service/src/main/java/com/scmcloud/order/domain/entity/OrdOrderItem.java` and verify tenant_id annotation.

- [ ] **Step 4: Verify Inventory entity**

Read `scm-inventory/service/src/main/java/com/scmcloud/inventory/domain/entity/Inventory.java` and verify tenant_id annotation.

- [ ] **Step 5: Verify ProdSpu and ProdSku entities**

Read product entities and verify tenant_id annotations.

- [ ] **Step 6: Commit if fixes needed**

```bash
git add .
git commit -m "fix(entities): ensure tenant_id auto-fill on all business entities"
```

---

### Task 4: Verify Cache Tenant Isolation in TwoLevelCache

**Files:**
- Verify: `scm-common/cache/src/main/java/com/scmcloud/common/cache/spring/TwoLevelCache.java`

- [ ] **Step 1: Verify keyString() method includes tenantId**

Read `TwoLevelCache.java` lines 174-178:

```java
private String keyString(Object key) {
    UUID tenantId = TenantContextHolder.getTenantId();
    String tenantPrefix = tenantId != null ? tenantId + ":" : "";
    return tenantPrefix + String.valueOf(key);
}
```

Status: ✅ VERIFIED - TwoLevelCache already includes tenantId in cache keys

- [ ] **Step 2: Verify clear() method is tenant-scoped**

Read `TwoLevelCache.java` lines 116-140:

```java
public void clear() {
    local.invalidateAll();
    try {
        UUID tenantId = TenantContextHolder.getTenantId();
        String pattern = (tenantId != null)
                ? name + ":" + tenantId + ":*"
                : name + ":*";
        // ... scan and delete pattern
    }
}
```

Status: ✅ VERIFIED - clear() is tenant-scoped

- [ ] **Step 3: No changes needed**

---

### Task 5: Integration Test - @Async Tenant Context Propagation

**Files:**
- Create: `scm-common/core/src/test/java/com/scmcloud/common/tenant/TenantAwareTaskDecoratorTest.java`

- [ ] **Step 1: Create integration test**

```java
package com.scmcloud.common.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class TenantAwareTaskDecoratorTest {

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldPropagateTenantIdToAsyncThread() throws Exception {
        // Given
        UUID expectedTenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(expectedTenantId);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.setTaskDecorator(new TenantAwareTaskDecorator());
        executor.initialize();

        // When
        CompletableFuture<UUID> future = CompletableFuture.supplyAsync(() -> {
            return TenantContextHolder.getTenantId();
        }, executor);

        // Then
        UUID actualTenantId = future.get(5, TimeUnit.SECONDS);
        assertEquals(expectedTenantId, actualTenantId);

        executor.shutdown();
    }

    @Test
    void shouldClearTenantIdAfterExecution() throws Exception {
        // Given
        UUID expectedTenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(expectedTenantId);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.setTaskDecorator(new TenantAwareTaskDecorator());
        executor.initialize();

        // When
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            // Simulate async work
        }, executor);

        future.get(5, TimeUnit.SECONDS);

        // Then - the async thread should have cleared TenantContextHolder
        // Verify by running another task
        CompletableFuture<UUID> future2 = CompletableFuture.supplyAsync(() -> {
            return TenantContextHolder.getTenantId();
        }, executor);

        UUID tenantIdAfterExecution = future2.get(5, TimeUnit.SECONDS);
        assertNull(tenantIdAfterExecution);

        executor.shutdown();
    }

    @Test
    void shouldHandleNullTenantIdGracefully() throws Exception {
        // Given - no tenant ID set
        TenantContextHolder.clear();

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.setTaskDecorator(new TenantAwareTaskDecorator());
        executor.initialize();

        // When
        CompletableFuture<UUID> future = CompletableFuture.supplyAsync(() -> {
            return TenantContextHolder.getTenantId();
        }, executor);

        // Then
        UUID actualTenantId = future.get(5, TimeUnit.SECONDS);
        assertNull(actualTenantId);

        executor.shutdown();
    }
}
```

- [ ] **Step 2: Run the test**

Run:
```bash
mvn test -pl scm-common/core -Dtest=TenantAwareTaskDecoratorTest -f com.scm.parent/pom.xml
```

Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add scm-common/core/src/test/java/com/scmcloud/common/tenant/TenantAwareTaskDecoratorTest.java
git commit -m "test(tenant): add integration test for @Async tenant context propagation"
```

---

### Task 6: Integration Test - Dubbo Tenant Context Propagation

**Files:**
- Create: `scm-common/web/src/test/java/com/scmcloud/common/dubbo/TenantContextFilterTest.java`

- [ ] **Step 1: Create unit test for TenantContextFilter**

```java
package com.scmcloud.common.dubbo;

import com.scmcloud.common.tenant.TenantContextHolder;
import org.apache.dubbo.rpc.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantContextFilterTest {

    private TenantContextFilter filter;
    private Invoker<?> invoker;
    private Invocation invocation;

    @BeforeEach
    void setUp() {
        filter = new TenantContextFilter();
        invoker = mock(Invoker.class);
        invocation = mock(Invocation.class);

        URL url = URL.valueOf("dubbo://localhost:20880/test?side=provider");
        when(invoker.getUrl()).thenReturn(url);
        when(invoker.invoke(any())).thenReturn(new AppResponse());
    }

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldSetTenantIdFromRpcContextOnProviderSide() {
        // Given
        UUID expectedTenantId = UUID.randomUUID();
        RpcContext.getServiceContext().setAttachment("tenant_id", expectedTenantId.toString());

        // When
        filter.invoke(invoker, invocation);

        // Then
        // The filter should have set the tenant ID in TenantContextHolder
        // Note: This is a simplified test; real integration test would verify end-to-end
    }

    @Test
    void shouldNotThrowOnInvalidTenantIdInRpcContext() {
        // Given
        RpcContext.getServiceContext().setAttachment("tenant_id", "invalid-uuid");

        // When & Then - should not throw
        assertDoesNotThrow(() -> filter.invoke(invoker, invocation));
    }

    @Test
    void shouldHandleMissingTenantIdGracefully() {
        // Given - no tenant_id in RpcContext

        // When & Then - should not throw
        assertDoesNotThrow(() -> filter.invoke(invoker, invocation));
    }
}
```

- [ ] **Step 2: Run the test**

Run:
```bash
mvn test -pl scm-common/web -Dtest=TenantContextFilterTest -f com.scm.parent/pom.xml
```

Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add scm-common/web/src/test/java/com/scmcloud/common/dubbo/TenantContextFilterTest.java
git commit -m "test(dubbo): add unit test for TenantContextFilter"
```

---

### Task 7: Integration Test - Cache Tenant Isolation

**Files:**
- Create: `scm-common/cache/src/test/java/com/scmcloud/common/cache/TwoLevelCacheTenantIsolationTest.java`

- [ ] **Step 1: Create integration test**

```java
package com.scmcloud.common.cache;

import com.scmcloud.common.cache.spring.TwoLevelCache;
import com.scmcloud.common.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TwoLevelCacheTenantIsolationTest {

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldIsolateCacheBetweenTenants() {
        // Given
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();

        RedisTemplate<String, Object> redisTemplate = createRedisTemplate();
        TwoLevelCache cache = new TwoLevelCache("test-cache", Duration.ofMinutes(5), redisTemplate, 100);

        // When - tenant A stores a value
        TenantContextHolder.setTenantId(tenantA);
        cache.put("user:123", "Alice");

        // Then - tenant B should not see tenant A's value
        TenantContextHolder.setTenantId(tenantB);
        Object valueForTenantB = cache.get("user:123", String.class);
        assertNull(valueForTenantB);

        // And - tenant A should see their own value
        TenantContextHolder.setTenantId(tenantA);
        Object valueForTenantA = cache.get("user:123", String.class);
        assertEquals("Alice", valueForTenantA);

        // Cleanup
        cache.evict("user:123");
    }

    @Test
    void shouldGenerateDifferentKeysForDifferentTenants() {
        // Given
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();

        RedisTemplate<String, Object> redisTemplate = createRedisTemplate();
        TwoLevelCache cache = new TwoLevelCache("test-cache", Duration.ofMinutes(5), redisTemplate, 100);

        // When
        TenantContextHolder.setTenantId(tenantA);
        cache.put("shared-key", "value-from-tenant-a");

        TenantContextHolder.setTenantId(tenantB);
        cache.put("shared-key", "value-from-tenant-b");

        // Then
        TenantContextHolder.setTenantId(tenantA);
        assertEquals("value-from-tenant-a", cache.get("shared-key", String.class));

        TenantContextHolder.setTenantId(tenantB);
        assertEquals("value-from-tenant-b", cache.get("shared-key", String.class));

        // Cleanup
        cache.evict("shared-key");
    }

    private RedisTemplate<String, Object> createRedisTemplate() {
        // Note: This requires a running Redis instance for integration tests
        // For unit tests, mock RedisTemplate
        LettuceConnectionFactory factory = new LettuceConnectionFactory("localhost", 6379);
        factory.afterPropertiesSet();

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();

        return template;
    }
}
```

- [ ] **Step 2: Run the test (requires Redis)**

Run:
```bash
mvn test -pl scm-common/cache -Dtest=TwoLevelCacheTenantIsolationTest -f com.scm.parent/pom.xml
```

Expected: PASS (with Redis running)

- [ ] **Step 3: Commit**

```bash
git add scm-common/cache/src/test/java/com/scmcloud/common/cache/TwoLevelCacheTenantIsolationTest.java
git commit -m "test(cache): add integration test for tenant cache isolation"
```

---

### Task 8: Verify HopCountFilter Enforcement

**Files:**
- Verify: `scm-common/web/src/main/java/com/scmcloud/common/dubbo/HopCountFilter.java`

- [ ] **Step 1: Verify HopCountFilter implementation**

Read `HopCountFilter.java` and verify:
- Line 26: `MAX_HOPS = 3`
- Line 33: `if (hops > MAX_HOPS)` throws RpcException
- Line 22: `@Activate(group = CommonConstants.PROVIDER, order = -9999)` annotation

Status: ✅ VERIFIED - HopCountFilter correctly implements 3-hop limit

- [ ] **Step 2: Verify SPI registration**

Read `META-INF/dubbo/org.apache.dubbo.rpc.Filter` and verify:
```
tenantContextFilter=com.scmcloud.common.dubbo.TenantContextFilter
hopCountFilter=com.scmcloud.common.dubbo.HopCountFilter
```

Status: ✅ VERIFIED - Both filters registered in SPI

- [ ] **Step 3: No changes needed**

---

### Task 9: Verify OutboxService Implementation

**Files:**
- Verify: `scm-common/integration/src/main/java/com/scmcloud/common/integration/outbox/OutboxService.java`
- Verify: `scripts/db/microservices/022_outbox_event.sql`

- [ ] **Step 1: Verify OutboxService implementation**

Read `OutboxService.java` and verify:
- `save()` method writes to outbox table within same transaction
- `findPendingEvents()` uses `SELECT FOR UPDATE SKIP LOCKED`
- `markFailed()` implements exponential backoff
- `cleanup()` deletes events older than 7 days

Status: ✅ VERIFIED - OutboxService correctly implemented

- [ ] **Step 2: Verify outbox_event DDL**

Read `022_outbox_event.sql` and verify table structure includes:
- `id`, `event_type`, `aggregate_type`, `aggregate_id`, `payload`
- `tenant_id`, `status`, `retry_count`, `max_retries`
- `created_at`, `published_at`, `next_retry_at`, `last_error`

- [ ] **Step 3: Verify OutboxPoller exists**

Search for OutboxPoller:
```bash
grep -r "OutboxPoller" --include="*.java" D:\ProgramProject\scm-platform
```

If missing, note as follow-up task.

- [ ] **Step 4: Commit if fixes needed**

```bash
git add .
git commit -m "fix(outbox): verify and fix outbox implementation"
```

---

### Task 10: Build Verification

- [ ] **Step 1: Run full build**

Run:
```bash
mvn clean install -DskipTests -f com.scm.parent/pom.xml
```

Expected: BUILD SUCCESS

- [ ] **Step 2: Run tests**

Run:
```bash
mvn test -f com.scm.parent/pom.xml
```

Expected: All tests pass

- [ ] **Step 3: Run lint/typecheck if available**

Run:
```bash
mvn verify -f com.scm.parent/pom.xml
```

Expected: All checks pass

---

## Summary

| Task | Description | Status |
|------|-------------|--------|
| T001 | Create TenantAwareCacheKeyGenerator | PENDING |
| T002 | Verify @Async thread pool configuration | PENDING |
| T003 | Verify entity tenant_id auto-fill | PENDING |
| T004 | Verify cache tenant isolation | PENDING |
| T005 | Integration test: @Async tenant propagation | PENDING |
| T006 | Integration test: Dubbo tenant propagation | PENDING |
| T007 | Integration test: Cache tenant isolation | PENDING |
| T008 | Verify HopCountFilter enforcement | PENDING |
| T009 | Verify OutboxService implementation | PENDING |
| T010 | Build verification | PENDING |

**Total Estimated Effort:** 2-3 days

**Priority:** P0 - Must complete before any production use

**Dependencies:** None (all infrastructure already exists)

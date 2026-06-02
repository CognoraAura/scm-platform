# Tenant API Module Restructure Design

## Background

The `scm-tenant/api` module currently contains a single `TenantDubboService.java` file with inner classes (`TenantVO`, `TenantConfigVO`). This design has several issues:

- Inner classes are not reusable across modules
- No separation between query and command contracts
- Growing interface file with mixed responsibilities
- Inconsistent with enterprise-level API design patterns

## Goals

1. Restructure `scm-tenant/api` into a clean CQRS-based package layout
2. Split the single `TenantDubboService` into multiple domain-focused interfaces
3. Decouple RPC contracts from HTTP contracts (Controller layer)
4. Expose complete tenant management capabilities via Dubbo RPC

## Non-Goals

- Refactoring other modules' Dubbo API structures (e.g., `scm-order/api`)
- Changing the service module's internal architecture
- Adding new business logic (only exposing existing service capabilities)

## Design

### API Module Package Structure

```
scm-tenant/api/src/main/java/com/frog/tenant/api/
│
├── service/
│   ├── TenantDubboService.java
│   ├── TenantConfigDubboService.java
│   └── TenantPackageDubboService.java
│
├── dto/
│   ├── common/
│   │   └── PageResult.java              # Generic<T> pagination result
│   │
│   ├── tenant/
│   │   ├── TenantDTO.java
│   │   ├── TenantFeatureDTO.java
│   │   └── TenantResourceQuotaDTO.java
│   │
│   ├── subscription/
│   │   ├── TenantPackageDTO.java
│   │   └── TenantSubscriptionDTO.java
│   │
│   └── config/
│       └── TenantConfigDTO.java
│
├── command/
│   ├── TenantCreateCommand.java
│   ├── TenantUpdateCommand.java
│   ├── TenantSubscribeCommand.java
│   └── TenantConfigUpdateCommand.java
│
└── query/
    ├── TenantQuery.java
    └── TenantDetailQuery.java
```

### Package Responsibilities

| Package | Responsibility | Naming Convention |
|---------|---------------|-------------------|
| `service/` | Dubbo RPC interface definitions | `*DubboService.java` |
| `dto/` | Read-only data transfer objects (query results), organized by domain sub-package | `*DTO.java` |
| `command/` | Write operation input objects — **pure POJOs, NO validation annotations** | `*Command.java` |
| `query/` | Query conditions only | `*Query.java` |

**Key decisions:**
- `PageResult<T>` is a generic reusable class in `dto/common/`, not per-entity
- Command objects are pure POJOs without `jakarta.validation` annotations — validation belongs to the Controller/HTTP layer
- DTO sub-packages (`tenant/`, `subscription/`, `config/`) scale as the module grows

### Entity-to-DTO Mapping

| Entity (service) | DTO (api) | Sub-package | Fields Exposed |
|------------------|-----------|-------------|----------------|
| `Tenant` | `TenantDTO` | `dto/tenant/` | id, tenantCode, tenantName, tenantType, companyName, contactName, contactPhone, contactEmail, status, industry, domain, createTime |
| `TenantConfig` | `TenantConfigDTO` | `dto/config/` | id, tenantId, configCategory, configKey, configValue, valueType, description |
| `TenantPackage` | `TenantPackageDTO` | `dto/subscription/` | id, packageCode, packageName, packageLevel, priceMonthly, priceYearly, maxUsers, maxWarehouses, maxSkus, maxOrdersPerDay, features, enabled |
| `TenantFeature` | `TenantFeatureDTO` | `dto/tenant/` | id, tenantId, featureCode, featureName, enabled, usageLimit, currentUsage, expireAt |
| `TenantSubscription` | `TenantSubscriptionDTO` | `dto/subscription/` | id, tenantId, packageId, subscriptionType, status, startDate, endDate, autoRenew, actualPrice, paymentStatus |
| `TenantResourceQuota` | `TenantResourceQuotaDTO` | `dto/tenant/` | id, tenantId, maxUsers, currentUsers, maxWarehouses, currentWarehouses, maxSkus, currentSkus, maxOrdersPerDay, currentOrdersToday, maxStorageGb, currentStorageGb |

### ID Type Consistency

The `Tenant` entity uses `String id` (UUID-based). All API method parameters and DTO fields use `String` for ID types consistently:

```java
TenantDTO getById(String tenantId);
TenantDTO getByCode(String tenantCode);
```

### Dubbo Interface Definitions

#### TenantDubboService

```java
public interface TenantDubboService {
    // Query
    TenantDTO getById(String tenantId);
    TenantDTO getByCode(String tenantCode);
    PageResult<TenantDTO> queryTenants(TenantQuery query);
    boolean checkFeatureEnabled(String tenantId, String featureCode);
    TenantResourceQuotaDTO getResourceQuota(String tenantId);
    QuotaCheckResultDTO checkQuota(String tenantId, QuotaType quotaType, int required);

    // Command
    String createTenant(TenantCreateCommand command);
    void updateTenant(String tenantId, TenantUpdateCommand command);
    void suspendTenant(String tenantId);
    void activateTenant(String tenantId);
}
```

**Quota checking** uses typed enum instead of magic strings:

```java
public enum QuotaType {
    USER, WAREHOUSE, SKU, ORDER_PER_DAY, STORAGE_GB, API_CALLS_PER_DAY
}

public class QuotaCheckResultDTO implements Serializable {
    private boolean available;
    private int current;
    private int max;
    private String message;
}
```

#### TenantConfigDubboService

```java
public interface TenantConfigDubboService {
    List<TenantConfigDTO> listConfigs(String tenantId);
    String getConfigValue(String tenantId, String configKey);
    void updateConfig(String tenantId, TenantConfigUpdateCommand command);
    Map<String, String> getFeatureFlags(String tenantId);
}
```

**Note:** `listConfigs()` returns `List<TenantConfigDTO>` instead of a single `TenantConfigDTO`, because a tenant may have 100+ config entries.

#### TenantPackageDubboService

```java
public interface TenantPackageDubboService {
    TenantPackageDTO getPackageById(String packageId);
    TenantPackageDTO getTenantCurrentPackage(String tenantId);
    List<TenantPackageDTO> listAvailablePackages();
    TenantSubscriptionDTO getActiveSubscription(String tenantId);
    String subscribe(String tenantId, TenantSubscribeCommand command);
}
```

### Command Objects (Pure POJOs)

```java
public class TenantCreateCommand implements Serializable {
    private String tenantName;
    private String tenantCode;
    private Integer tenantType;
    private String companyName;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String industry;
    private String packageId;
}

public class TenantUpdateCommand implements Serializable {
    private String tenantName;
    private String companyName;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String industry;
    private String domain;
}

public class TenantSubscribeCommand implements Serializable {
    private String packageId;
    private Integer subscriptionType;  // 1=monthly, 2=yearly
    private Boolean autoRenew;
}

public class TenantConfigUpdateCommand implements Serializable {
    private String configKey;
    private String configValue;
}
```

### Query Objects

```java
public class TenantQuery implements Serializable {
    private String tenantName;
    private Integer tenantType;
    private Integer status;
    private String industry;
    private int pageNum = 1;
    private int pageSize = 20;
    private String orderBy;
    private boolean asc = false;
}

// Generic pagination result in dto/common/
public class PageResult<T> implements Serializable {
    private long total;
    private int pageNum;
    private int pageSize;
    private List<T> records;
}
```

### HTTP Layer Separation (service module)

The Controller layer in `scm-tenant/service` maintains its own request/response objects:

```
scm-tenant/service/src/main/java/scm/tenant/
├── controller/
│   └── TenantController.java
└── web/
    ├── request/
    │   ├── TenantCreateRequest.java    # @NotBlank, @NotNull validation here
    │   └── TenantQueryRequest.java
    └── response/
        └── TenantResponse.java
```

Controller converts between HTTP and RPC contracts:
- `TenantCreateRequest` → `TenantCreateCommand` (validation lives here)
- `TenantDTO` → `TenantResponse` (may include additional display fields)

### Relationship with scm-common DTOs

- `scm-common/data/dto/` — Cross-service shared DTOs (`UserDTO`, `RoleDTO`, `DeptDTO`)
- `scm-tenant/api/dto/` — Tenant-specific DTOs, not shared in common
- Other services that need `TenantDTO` depend on `scm-tenant/api` directly

### Future Extensions

- `TenantStatisticsDubboService` — Add when statistics/monitoring features are needed
- `TenantOperationLogDTO` — Add when audit log querying is exposed via RPC
- DTO versioning strategy (V1/V2) — When breaking changes are needed

## Migration Strategy

1. Create new package structure under `scm-tenant/api`
2. Extract inner classes to standalone DTO files
3. Create generic `PageResult<T>` in `dto/common/`
4. Create Command and Query objects
5. Add `QuotaType` enum and `QuotaCheckResultDTO`
6. Split `TenantDubboService` into domain-specific interfaces
7. Update service module implementations
8. Update all consumers of the old `TenantDubboService`
9. Delete the old single-file `TenantDubboService.java`

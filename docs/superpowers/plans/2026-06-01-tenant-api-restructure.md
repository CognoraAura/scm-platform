# Tenant API Module Restructure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restructure `scm-tenant/api` into enterprise-level CQRS-based package layout with domain-focused Dubbo interfaces.

**Architecture:** Extract inner classes to standalone DTOs organized by domain sub-package. Split single `TenantDubboService` into `TenantDubboService`, `TenantConfigDubboService`, `TenantPackageDubboService`. Command/Query separation for write/read contracts.

**Tech Stack:** Java 21, Spring Boot 3.x, Apache Dubbo 3.x, MyBatis Plus, Lombok

---

## File Structure

### API Module (create)
```
scm-tenant/api/src/main/java/com/frog/tenant/api/
├── service/
│   ├── TenantDubboService.java
│   ├── TenantConfigDubboService.java
│   └── TenantPackageDubboService.java
├── dto/
│   ├── common/
│   │   └── PageResult.java
│   ├── tenant/
│   │   ├── TenantDTO.java
│   │   ├── TenantFeatureDTO.java
│   │   ├── TenantResourceQuotaDTO.java
│   │   ├── QuotaType.java
│   │   └── QuotaCheckResultDTO.java
│   ├── subscription/
│   │   ├── TenantPackageDTO.java
│   │   └── TenantSubscriptionDTO.java
│   └── config/
│       └── TenantConfigDTO.java
├── command/
│   ├── TenantCreateCommand.java
│   ├── TenantUpdateCommand.java
│   ├── TenantSubscribeCommand.java
│   └── TenantConfigUpdateCommand.java
└── query/
    ├── TenantQuery.java
    └── TenantDetailQuery.java
```

### Service Module (create)
```
scm-tenant/service/src/main/java/scm/tenant/service/dubbo/
├── TenantDubboServiceImpl.java
├── TenantConfigDubboServiceImpl.java
└── TenantPackageDubboServiceImpl.java
```

### API Module (delete)
```
scm-tenant/api/src/main/java/com/frog/tenant/api/TenantDubboService.java
```

---

### Task 1: Create generic PageResult DTO

**Files:**
- Create: `scm-tenant/api/src/main/java/com/frog/tenant/api/dto/common/PageResult.java`

- [ ] **Step 1: Create PageResult class**

```java
package com.frog.tenant.api.dto.common;

import java.io.Serializable;
import java.util.List;

public class PageResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private long total;
    private int pageNum;
    private int pageSize;
    private List<T> records;

    public PageResult() {
    }

    public PageResult(long total, int pageNum, int pageSize, List<T> records) {
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.records = records;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `mvn compile -pl scm-tenant/api -f com.scm.parent/pom.xml`
Expected: BUILD SUCCESS

---

### Task 2: Create tenant DTOs

**Files:**
- Create: `scm-tenant/api/src/main/java/com/frog/tenant/api/dto/tenant/TenantDTO.java`
- Create: `scm-tenant/api/src/main/java/com/frog/tenant/api/dto/tenant/TenantFeatureDTO.java`
- Create: `scm-tenant/api/src/main/java/com/frog/tenant/api/dto/tenant/TenantResourceQuotaDTO.java`

- [ ] **Step 1: Create TenantDTO**

```java
package com.frog.tenant.api.dto.tenant;

import java.io.Serializable;
import java.time.LocalDateTime;

public class TenantDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String tenantCode;
    private String tenantName;
    private Integer tenantType;
    private String companyName;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private Integer status;
    private String industry;
    private String domain;
    private LocalDateTime createTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public Integer getTenantType() {
        return tenantType;
    }

    public void setTenantType(Integer tenantType) {
        this.tenantType = tenantType;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
```

- [ ] **Step 2: Create TenantFeatureDTO**

```java
package com.frog.tenant.api.dto.tenant;

import java.io.Serializable;
import java.time.LocalDateTime;

public class TenantFeatureDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String tenantId;
    private String featureCode;
    private String featureName;
    private Boolean enabled;
    private Integer usageLimit;
    private Integer currentUsage;
    private LocalDateTime expireAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getFeatureCode() {
        return featureCode;
    }

    public void setFeatureCode(String featureCode) {
        this.featureCode = featureCode;
    }

    public String getFeatureName() {
        return featureName;
    }

    public void setFeatureName(String featureName) {
        this.featureName = featureName;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getUsageLimit() {
        return usageLimit;
    }

    public void setUsageLimit(Integer usageLimit) {
        this.usageLimit = usageLimit;
    }

    public Integer getCurrentUsage() {
        return currentUsage;
    }

    public void setCurrentUsage(Integer currentUsage) {
        this.currentUsage = currentUsage;
    }

    public LocalDateTime getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(LocalDateTime expireAt) {
        this.expireAt = expireAt;
    }
}
```

- [ ] **Step 3: Create TenantResourceQuotaDTO**

```java
package com.frog.tenant.api.dto.tenant;

import java.io.Serializable;
import java.math.BigDecimal;

public class TenantResourceQuotaDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String tenantId;
    private Integer maxUsers;
    private Integer currentUsers;
    private Integer maxWarehouses;
    private Integer currentWarehouses;
    private Integer maxSkus;
    private Integer currentSkus;
    private Integer maxOrdersPerDay;
    private Integer currentOrdersToday;
    private Integer maxStorageGb;
    private BigDecimal currentStorageGb;
    private Integer maxApiCallsPerDay;
    private Integer currentApiCallsToday;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Integer getMaxUsers() {
        return maxUsers;
    }

    public void setMaxUsers(Integer maxUsers) {
        this.maxUsers = maxUsers;
    }

    public Integer getCurrentUsers() {
        return currentUsers;
    }

    public void setCurrentUsers(Integer currentUsers) {
        this.currentUsers = currentUsers;
    }

    public Integer getMaxWarehouses() {
        return maxWarehouses;
    }

    public void setMaxWarehouses(Integer maxWarehouses) {
        this.maxWarehouses = maxWarehouses;
    }

    public Integer getCurrentWarehouses() {
        return currentWarehouses;
    }

    public void setCurrentWarehouses(Integer currentWarehouses) {
        this.currentWarehouses = currentWarehouses;
    }

    public Integer getMaxSkus() {
        return maxSkus;
    }

    public void setMaxSkus(Integer maxSkus) {
        this.maxSkus = maxSkus;
    }

    public Integer getCurrentSkus() {
        return currentSkus;
    }

    public void setCurrentSkus(Integer currentSkus) {
        this.currentSkus = currentSkus;
    }

    public Integer getMaxOrdersPerDay() {
        return maxOrdersPerDay;
    }

    public void setMaxOrdersPerDay(Integer maxOrdersPerDay) {
        this.maxOrdersPerDay = maxOrdersPerDay;
    }

    public Integer getCurrentOrdersToday() {
        return currentOrdersToday;
    }

    public void setCurrentOrdersToday(Integer currentOrdersToday) {
        this.currentOrdersToday = currentOrdersToday;
    }

    public Integer getMaxStorageGb() {
        return maxStorageGb;
    }

    public void setMaxStorageGb(Integer maxStorageGb) {
        this.maxStorageGb = maxStorageGb;
    }

    public BigDecimal getCurrentStorageGb() {
        return currentStorageGb;
    }

    public void setCurrentStorageGb(BigDecimal currentStorageGb) {
        this.currentStorageGb = currentStorageGb;
    }

    public Integer getMaxApiCallsPerDay() {
        return maxApiCallsPerDay;
    }

    public void setMaxApiCallsPerDay(Integer maxApiCallsPerDay) {
        this.maxApiCallsPerDay = maxApiCallsPerDay;
    }

    public Integer getCurrentApiCallsToday() {
        return currentApiCallsToday;
    }

    public void setCurrentApiCallsToday(Integer currentApiCallsToday) {
        this.currentApiCallsToday = currentApiCallsToday;
    }
}
```

- [ ] **Step 4: Verify compilation**

Run: `mvn compile -pl scm-tenant/api -f com.scm.parent/pom.xml`
Expected: BUILD SUCCESS

---

### Task 3: Create QuotaType enum and QuotaCheckResultDTO

**Files:**
- Create: `scm-tenant/api/src/main/java/com/frog/tenant/api/dto/tenant/QuotaType.java`
- Create: `scm-tenant/api/src/main/java/com/frog/tenant/api/dto/tenant/QuotaCheckResultDTO.java`

- [ ] **Step 1: Create QuotaType enum**

```java
package com.frog.tenant.api.dto.tenant;

public enum QuotaType {
    USER,
    WAREHOUSE,
    SKU,
    ORDER_PER_DAY,
    STORAGE_GB,
    API_CALLS_PER_DAY
}
```

- [ ] **Step 2: Create QuotaCheckResultDTO**

```java
package com.frog.tenant.api.dto.tenant;

import java.io.Serializable;

public class QuotaCheckResultDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean available;
    private int current;
    private int max;
    private String message;

    public QuotaCheckResultDTO() {
    }

    public QuotaCheckResultDTO(boolean available, int current, int max, String message) {
        this.available = available;
        this.current = current;
        this.max = max;
        this.message = message;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        this.current = current;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `mvn compile -pl scm-tenant/api -f com.scm.parent/pom.xml`
Expected: BUILD SUCCESS

---

### Task 4: Create package DTOs

**Files:**
- Create: `scm-tenant/api/src/main/java/com/frog/tenant/api/dto/subscription/TenantPackageDTO.java`
- Create: `scm-tenant/api/src/main/java/com/frog/tenant/api/dto/subscription/TenantSubscriptionDTO.java`

- [ ] **Step 1: Create TenantPackageDTO**

```java
package com.frog.tenant.api.dto.subscription;

import java.io.Serializable;
import java.math.BigDecimal;

public class TenantPackageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String packageCode;
    private String packageName;
    private Integer packageLevel;
    private BigDecimal priceMonthly;
    private BigDecimal priceYearly;
    private Integer maxUsers;
    private Integer maxWarehouses;
    private Integer maxSkus;
    private Integer maxOrdersPerDay;
    private String features;
    private Boolean enabled;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPackageCode() {
        return packageCode;
    }

    public void setPackageCode(String packageCode) {
        this.packageCode = packageCode;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public Integer getPackageLevel() {
        return packageLevel;
    }

    public void setPackageLevel(Integer packageLevel) {
        this.packageLevel = packageLevel;
    }

    public BigDecimal getPriceMonthly() {
        return priceMonthly;
    }

    public void setPriceMonthly(BigDecimal priceMonthly) {
        this.priceMonthly = priceMonthly;
    }

    public BigDecimal getPriceYearly() {
        return priceYearly;
    }

    public void setPriceYearly(BigDecimal priceYearly) {
        this.priceYearly = priceYearly;
    }

    public Integer getMaxUsers() {
        return maxUsers;
    }

    public void setMaxUsers(Integer maxUsers) {
        this.maxUsers = maxUsers;
    }

    public Integer getMaxWarehouses() {
        return maxWarehouses;
    }

    public void setMaxWarehouses(Integer maxWarehouses) {
        this.maxWarehouses = maxWarehouses;
    }

    public Integer getMaxSkus() {
        return maxSkus;
    }

    public void setMaxSkus(Integer maxSkus) {
        this.maxSkus = maxSkus;
    }

    public Integer getMaxOrdersPerDay() {
        return maxOrdersPerDay;
    }

    public void setMaxOrdersPerDay(Integer maxOrdersPerDay) {
        this.maxOrdersPerDay = maxOrdersPerDay;
    }

    public String getFeatures() {
        return features;
    }

    public void setFeatures(String features) {
        this.features = features;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
```

- [ ] **Step 2: Create TenantSubscriptionDTO**

```java
package com.frog.tenant.api.dto.subscription;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TenantSubscriptionDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String tenantId;
    private String packageId;
    private Integer subscriptionType;
    private Integer status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean autoRenew;
    private BigDecimal actualPrice;
    private Integer paymentStatus;
    private LocalDateTime createTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getPackageId() {
        return packageId;
    }

    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }

    public Integer getSubscriptionType() {
        return subscriptionType;
    }

    public void setSubscriptionType(Integer subscriptionType) {
        this.subscriptionType = subscriptionType;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Boolean getAutoRenew() {
        return autoRenew;
    }

    public void setAutoRenew(Boolean autoRenew) {
        this.autoRenew = autoRenew;
    }

    public BigDecimal getActualPrice() {
        return actualPrice;
    }

    public void setActualPrice(BigDecimal actualPrice) {
        this.actualPrice = actualPrice;
    }

    public Integer getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(Integer paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `mvn compile -pl scm-tenant/api -f com.scm.parent/pom.xml`
Expected: BUILD SUCCESS

---

### Task 5: Create config DTO

**Files:**
- Create: `scm-tenant/api/src/main/java/com/frog/tenant/api/dto/config/TenantConfigDTO.java`

- [ ] **Step 1: Create TenantConfigDTO**

```java
package com.frog.tenant.api.dto.config;

import java.io.Serializable;

public class TenantConfigDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String tenantId;
    private String configCategory;
    private String configKey;
    private String configValue;
    private String valueType;
    private String description;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getConfigCategory() {
        return configCategory;
    }

    public void setConfigCategory(String configCategory) {
        this.configCategory = configCategory;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `mvn compile -pl scm-tenant/api -f com.scm.parent/pom.xml`
Expected: BUILD SUCCESS

---

### Task 6: Create command objects

**Files:**
- Create: `scm-tenant/api/src/main/java/com/frog/tenant/api/command/TenantCreateCommand.java`
- Create: `scm-tenant/api/src/main/java/com/frog/tenant/api/command/TenantUpdateCommand.java`
- Create: `scm-tenant/api/src/main/java/com/frog/tenant/api/command/TenantSubscribeCommand.java`
- Create: `scm-tenant/api/src/main/java/com/frog/tenant/api/command/TenantConfigUpdateCommand.java`

- [ ] **Step 1: Create TenantCreateCommand**

```java
package com.frog.tenant.api.command;

import java.io.Serializable;

public class TenantCreateCommand implements Serializable {
    private static final long serialVersionUID = 1L;

    private String tenantName;
    private String tenantCode;
    private Integer tenantType;
    private String companyName;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String industry;
    private String packageId;

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public Integer getTenantType() {
        return tenantType;
    }

    public void setTenantType(Integer tenantType) {
        this.tenantType = tenantType;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getPackageId() {
        return packageId;
    }

    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }
}
```

- [ ] **Step 2: Create TenantUpdateCommand**

```java
package com.frog.tenant.api.command;

import java.io.Serializable;

public class TenantUpdateCommand implements Serializable {
    private static final long serialVersionUID = 1L;

    private String tenantName;
    private String companyName;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String industry;
    private String domain;

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }
}
```

- [ ] **Step 3: Create TenantSubscribeCommand**

```java
package com.frog.tenant.api.command;

import java.io.Serializable;

public class TenantSubscribeCommand implements Serializable {
    private static final long serialVersionUID = 1L;

    private String packageId;
    private Integer subscriptionType;
    private Boolean autoRenew;

    public String getPackageId() {
        return packageId;
    }

    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }

    public Integer getSubscriptionType() {
        return subscriptionType;
    }

    public void setSubscriptionType(Integer subscriptionType) {
        this.subscriptionType = subscriptionType;
    }

    public Boolean getAutoRenew() {
        return autoRenew;
    }

    public void setAutoRenew(Boolean autoRenew) {
        this.autoRenew = autoRenew;
    }
}
```

- [ ] **Step 4: Create TenantConfigUpdateCommand**

```java
package com.frog.tenant.api.command;

import java.io.Serializable;

public class TenantConfigUpdateCommand implements Serializable {
    private static final long serialVersionUID = 1L;

    private String configKey;
    private String configValue;

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }
}
```

- [ ] **Step 5: Verify compilation**

Run: `mvn compile -pl scm-tenant/api -f com.scm.parent/pom.xml`
Expected: BUILD SUCCESS

---

### Task 7: Create query objects

**Files:**
- Create: `scm-tenant/api/src/main/java/com/frog/tenant/api/query/TenantQuery.java`
- Create: `scm-tenant/api/src/main/java/com/frog/tenant/api/query/TenantDetailQuery.java`

- [ ] **Step 1: Create TenantQuery**

```java
package com.frog.tenant.api.query;

import java.io.Serializable;

public class TenantQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    private String tenantName;
    private Integer tenantType;
    private Integer status;
    private String industry;
    private int pageNum = 1;
    private int pageSize = 20;
    private String orderBy;
    private boolean asc = false;

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public Integer getTenantType() {
        return tenantType;
    }

    public void setTenantType(Integer tenantType) {
        this.tenantType = tenantType;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    public boolean isAsc() {
        return asc;
    }

    public void setAsc(boolean asc) {
        this.asc = asc;
    }
}
```

- [ ] **Step 2: Create TenantDetailQuery**

```java
package com.frog.tenant.api.query;

import java.io.Serializable;

public class TenantDetailQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    private String tenantId;
    private String tenantCode;
    private boolean includeConfig;
    private boolean includePackage;
    private boolean includeQuota;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public boolean isIncludeConfig() {
        return includeConfig;
    }

    public void setIncludeConfig(boolean includeConfig) {
        this.includeConfig = includeConfig;
    }

    public boolean isIncludePackage() {
        return includePackage;
    }

    public void setIncludePackage(boolean includePackage) {
        this.includePackage = includePackage;
    }

    public boolean isIncludeQuota() {
        return includeQuota;
    }

    public void setIncludeQuota(boolean includeQuota) {
        this.includeQuota = includeQuota;
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `mvn compile -pl scm-tenant/api -f com.scm.parent/pom.xml`
Expected: BUILD SUCCESS

---

### Task 8: Create TenantDubboService interface

**Files:**
- Create: `scm-tenant/api/src/main/java/com/frog/tenant/api/service/TenantDubboService.java`

- [ ] **Step 1: Create TenantDubboService**

```java
package com.frog.tenant.api.service;

import com.frog.tenant.api.command.TenantCreateCommand;
import com.frog.tenant.api.command.TenantUpdateCommand;
import com.frog.tenant.api.dto.common.PageResult;
import com.frog.tenant.api.dto.tenant.QuotaCheckResultDTO;
import com.frog.tenant.api.dto.tenant.QuotaType;
import com.frog.tenant.api.dto.tenant.TenantDTO;
import com.frog.tenant.api.dto.tenant.TenantResourceQuotaDTO;
import com.frog.tenant.api.query.TenantQuery;

public interface TenantDubboService {

    TenantDTO getById(String tenantId);

    TenantDTO getByCode(String tenantCode);

    PageResult<TenantDTO> queryTenants(TenantQuery query);

    boolean checkFeatureEnabled(String tenantId, String featureCode);

    TenantResourceQuotaDTO getResourceQuota(String tenantId);

    QuotaCheckResultDTO checkQuota(String tenantId, QuotaType quotaType, int required);

    String createTenant(TenantCreateCommand command);

    void updateTenant(String tenantId, TenantUpdateCommand command);

    void suspendTenant(String tenantId);

    void activateTenant(String tenantId);
}
```

- [ ] **Step 2: Verify compilation**

Run: `mvn compile -pl scm-tenant/api -f com.scm.parent/pom.xml`
Expected: BUILD SUCCESS

---

### Task 9: Create TenantConfigDubboService interface

**Files:**
- Create: `scm-tenant/api/src/main/java/com/frog/tenant/api/service/TenantConfigDubboService.java`

- [ ] **Step 1: Create TenantConfigDubboService**

```java
package com.frog.tenant.api.service;

import com.frog.tenant.api.command.TenantConfigUpdateCommand;
import com.frog.tenant.api.dto.config.TenantConfigDTO;

import java.util.List;
import java.util.Map;

public interface TenantConfigDubboService {

    List<TenantConfigDTO> listConfigs(String tenantId);

    String getConfigValue(String tenantId, String configKey);

    void updateConfig(String tenantId, TenantConfigUpdateCommand command);

    Map<String, String> getFeatureFlags(String tenantId);
}
```

- [ ] **Step 2: Verify compilation**

Run: `mvn compile -pl scm-tenant/api -f com.scm.parent/pom.xml`
Expected: BUILD SUCCESS

---

### Task 10: Create TenantPackageDubboService interface

**Files:**
- Create: `scm-tenant/api/src/main/java/com/frog/tenant/api/service/TenantPackageDubboService.java`

- [ ] **Step 1: Create TenantPackageDubboService**

```java
package com.frog.tenant.api.service;

import com.frog.tenant.api.command.TenantSubscribeCommand;
import com.frog.tenant.api.dto.subscription.TenantPackageDTO;
import com.frog.tenant.api.dto.subscription.TenantSubscriptionDTO;

import java.util.List;

public interface TenantPackageDubboService {

    TenantPackageDTO getPackageById(String packageId);

    TenantPackageDTO getTenantCurrentPackage(String tenantId);

    List<TenantPackageDTO> listAvailablePackages();

    TenantSubscriptionDTO getActiveSubscription(String tenantId);

    String subscribe(String tenantId, TenantSubscribeCommand command);
}
```

- [ ] **Step 2: Verify compilation**

Run: `mvn compile -pl scm-tenant/api -f com.scm.parent/pom.xml`
Expected: BUILD SUCCESS

---

### Task 11: Create TenantDubboServiceImpl

**Files:**
- Create: `scm-tenant/service/src/main/java/scm/tenant/service/dubbo/TenantDubboServiceImpl.java`

- [ ] **Step 1: Create TenantDubboServiceImpl**

```java
package scm.tenant.service.dubbo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frog.tenant.api.command.TenantCreateCommand;
import com.frog.tenant.api.command.TenantUpdateCommand;
import com.frog.tenant.api.dto.common.PageResult;
import com.frog.tenant.api.dto.tenant.QuotaCheckResultDTO;
import com.frog.tenant.api.dto.tenant.QuotaType;
import com.frog.tenant.api.dto.tenant.TenantDTO;
import com.frog.tenant.api.dto.tenant.TenantResourceQuotaDTO;
import com.frog.tenant.api.query.TenantQuery;
import com.frog.tenant.api.service.TenantDubboService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Component;
import scm.tenant.domain.entity.Tenant;
import scm.tenant.domain.entity.TenantResourceQuota;
import scm.tenant.service.ITenantFeatureService;
import scm.tenant.service.ITenantResourceQuotaService;
import scm.tenant.service.ITenantService;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@DubboService
@Component
@RequiredArgsConstructor
public class TenantDubboServiceImpl implements TenantDubboService {

    private final ITenantService tenantService;
    private final ITenantFeatureService featureService;
    private final ITenantResourceQuotaService quotaService;

    @Override
    public TenantDTO getById(String tenantId) {
        log.debug("Dubbo查询租户: tenantId={}", tenantId);
        Tenant tenant = tenantService.lambdaQuery()
                .eq(Tenant::getId, tenantId)
                .eq(Tenant::getDeleted, false)
                .one();
        return tenant == null ? null : convertToDTO(tenant);
    }

    @Override
    public TenantDTO getByCode(String tenantCode) {
        log.debug("Dubbo查询租户: tenantCode={}", tenantCode);
        Tenant tenant = tenantService.lambdaQuery()
                .eq(Tenant::getTenantCode, tenantCode)
                .eq(Tenant::getDeleted, false)
                .one();
        return tenant == null ? null : convertToDTO(tenant);
    }

    @Override
    public PageResult<TenantDTO> queryTenants(TenantQuery query) {
        log.debug("Dubbo分页查询租户: query={}", query);

        LambdaQueryWrapper<Tenant> wrapper = Wrappers.lambdaQuery();
        if (query.getTenantName() != null) {
            wrapper.like(Tenant::getTenantName, query.getTenantName());
        }
        if (query.getTenantType() != null) {
            wrapper.eq(Tenant::getTenantType, query.getTenantType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(Tenant::getStatus, query.getStatus());
        }
        if (query.getIndustry() != null) {
            wrapper.eq(Tenant::getIndustry, query.getIndustry());
        }
        wrapper.eq(Tenant::getDeleted, false);
        wrapper.orderByDesc(Tenant::getCreateTime);

        Page<Tenant> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<Tenant> result = tenantService.page(page, wrapper);

        PageResult<TenantDTO> pageResult = new PageResult<>();
        pageResult.setTotal(result.getTotal());
        pageResult.setPageNum(query.getPageNum());
        pageResult.setPageSize(query.getPageSize());
        pageResult.setRecords(result.getRecords().stream().map(this::convertToDTO).toList());
        return pageResult;
    }

    @Override
    public boolean checkFeatureEnabled(String tenantId, String featureCode) {
        return featureService.isFeatureEnabled(tenantId, featureCode);
    }

    @Override
    public TenantResourceQuotaDTO getResourceQuota(String tenantId) {
        log.debug("Dubbo查询资源配额: tenantId={}", tenantId);
        TenantResourceQuota quota = quotaService.lambdaQuery()
                .eq(TenantResourceQuota::getTenantId, tenantId)
                .one();
        return quota == null ? null : convertQuotaToDTO(quota);
    }

    @Override
    public QuotaCheckResultDTO checkQuota(String tenantId, QuotaType quotaType, int required) {
        log.debug("Dubbo检查配额: tenantId={}, quotaType={}, required={}", tenantId, quotaType, required);

        TenantResourceQuota quota = quotaService.lambdaQuery()
                .eq(TenantResourceQuota::getTenantId, tenantId)
                .one();

        if (quota == null) {
            return new QuotaCheckResultDTO(false, 0, 0, "配额信息不存在");
        }

        return switch (quotaType) {
            case USER -> buildResult(quota.getCurrentUsers(), quota.getMaxUsers(), required, "用户数");
            case WAREHOUSE -> buildResult(quota.getCurrentWarehouses(), quota.getMaxWarehouses(), required, "仓库数");
            case SKU -> buildResult(quota.getCurrentSkus(), quota.getMaxSkus(), required, "SKU数");
            case ORDER_PER_DAY -> buildResult(quota.getCurrentOrdersToday(), quota.getMaxOrdersPerDay(), required, "每日订单数");
            case STORAGE_GB -> buildResult(quota.getCurrentStorageGb() != null ? quota.getCurrentStorageGb().intValue() : 0, quota.getMaxStorageGb(), required, "存储空间");
            case API_CALLS_PER_DAY -> buildResult(quota.getCurrentApiCallsToday(), quota.getMaxApiCallsPerDay(), required, "每日API调用数");
        };
    }

    private QuotaCheckResultDTO buildResult(int current, int max, int required, String resourceName) {
        boolean available = (current + required) <= max;
        String message = available ? resourceName + "配额充足" : resourceName + "配额不足";
        return new QuotaCheckResultDTO(available, current, max, message);
    }

    @Override
    public String createTenant(TenantCreateCommand command) {
        log.info("Dubbo创建租户: tenantCode={}, tenantName={}", command.getTenantCode(), command.getTenantName());

        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID().toString());
        tenant.setTenantCode(command.getTenantCode());
        tenant.setTenantName(command.getTenantName());
        tenant.setTenantType(command.getTenantType());
        tenant.setCompanyName(command.getCompanyName());
        tenant.setContactName(command.getContactName());
        tenant.setContactPhone(command.getContactPhone());
        tenant.setContactEmail(command.getContactEmail());
        tenant.setIndustry(command.getIndustry());
        tenant.setStatus(0);
        tenant.setDeleted(false);
        tenant.setCreateTime(LocalDateTime.now());
        tenant.setUpdateTime(LocalDateTime.now());

        tenantService.save(tenant);
        log.info("Dubbo创建租户成功: id={}", tenant.getId());
        return tenant.getId();
    }

    @Override
    public void updateTenant(String tenantId, TenantUpdateCommand command) {
        log.info("Dubbo更新租户: tenantId={}", tenantId);

        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setTenantName(command.getTenantName());
        tenant.setCompanyName(command.getCompanyName());
        tenant.setContactName(command.getContactName());
        tenant.setContactPhone(command.getContactPhone());
        tenant.setContactEmail(command.getContactEmail());
        tenant.setIndustry(command.getIndustry());
        tenant.setDomain(command.getDomain());
        tenant.setUpdateTime(LocalDateTime.now());

        tenantService.updateById(tenant);
    }

    @Override
    public void suspendTenant(String tenantId) {
        log.info("Dubbo停用租户: tenantId={}", tenantId);
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setStatus(2);
        tenant.setSuspendedAt(LocalDateTime.now());
        tenant.setUpdateTime(LocalDateTime.now());
        tenantService.updateById(tenant);
    }

    @Override
    public void activateTenant(String tenantId) {
        log.info("Dubbo激活租户: tenantId={}", tenantId);
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setStatus(1);
        tenant.setActivatedAt(LocalDateTime.now());
        tenant.setUpdateTime(LocalDateTime.now());
        tenantService.updateById(tenant);
    }

    private TenantDTO convertToDTO(Tenant tenant) {
        TenantDTO dto = new TenantDTO();
        dto.setId(tenant.getId());
        dto.setTenantCode(tenant.getTenantCode());
        dto.setTenantName(tenant.getTenantName());
        dto.setTenantType(tenant.getTenantType());
        dto.setCompanyName(tenant.getCompanyName());
        dto.setContactName(tenant.getContactName());
        dto.setContactPhone(tenant.getContactPhone());
        dto.setContactEmail(tenant.getContactEmail());
        dto.setStatus(tenant.getStatus());
        dto.setIndustry(tenant.getIndustry());
        dto.setDomain(tenant.getDomain());
        dto.setCreateTime(tenant.getCreateTime());
        return dto;
    }

    private TenantResourceQuotaDTO convertQuotaToDTO(TenantResourceQuota quota) {
        TenantResourceQuotaDTO dto = new TenantResourceQuotaDTO();
        dto.setId(quota.getId());
        dto.setTenantId(quota.getTenantId());
        dto.setMaxUsers(quota.getMaxUsers());
        dto.setCurrentUsers(quota.getCurrentUsers());
        dto.setMaxWarehouses(quota.getMaxWarehouses());
        dto.setCurrentWarehouses(quota.getCurrentWarehouses());
        dto.setMaxSkus(quota.getMaxSkus());
        dto.setCurrentSkus(quota.getCurrentSkus());
        dto.setMaxOrdersPerDay(quota.getMaxOrdersPerDay());
        dto.setCurrentOrdersToday(quota.getCurrentOrdersToday());
        dto.setMaxStorageGb(quota.getMaxStorageGb());
        dto.setCurrentStorageGb(quota.getCurrentStorageGb());
        dto.setMaxApiCallsPerDay(quota.getMaxApiCallsPerDay());
        dto.setCurrentApiCallsToday(quota.getCurrentApiCallsToday());
        return dto;
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `mvn compile -pl scm-tenant/service -am -f com.scm.parent/pom.xml`
Expected: BUILD SUCCESS

---

### Task 12: Create TenantConfigDubboServiceImpl

**Files:**
- Create: `scm-tenant/service/src/main/java/scm/tenant/service/dubbo/TenantConfigDubboServiceImpl.java`

- [ ] **Step 1: Create TenantConfigDubboServiceImpl**

```java
package scm.tenant.service.dubbo;

import com.frog.tenant.api.command.TenantConfigUpdateCommand;
import com.frog.tenant.api.dto.config.TenantConfigDTO;
import com.frog.tenant.api.service.TenantConfigDubboService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Component;
import scm.tenant.domain.entity.TenantConfig;
import scm.tenant.service.ITenantConfigService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@DubboService
@Component
@RequiredArgsConstructor
public class TenantConfigDubboServiceImpl implements TenantConfigDubboService {

    private final ITenantConfigService configService;

    @Override
    public List<TenantConfigDTO> listConfigs(String tenantId) {
        log.debug("Dubbo查询租户配置列表: tenantId={}", tenantId);
        List<TenantConfig> configs = configService.lambdaQuery()
                .eq(TenantConfig::getTenantId, tenantId)
                .orderByAsc(TenantConfig::getConfigCategory)
                .list();
        return configs.stream().map(this::convertToDTO).toList();
    }

    @Override
    public String getConfigValue(String tenantId, String configKey) {
        log.debug("Dubbo查询配置值: tenantId={}, configKey={}", tenantId, configKey);
        TenantConfig config = configService.lambdaQuery()
                .eq(TenantConfig::getTenantId, tenantId)
                .eq(TenantConfig::getConfigKey, configKey)
                .one();
        return config == null ? null : config.getConfigValue();
    }

    @Override
    public void updateConfig(String tenantId, TenantConfigUpdateCommand command) {
        log.info("Dubbo更新租户配置: tenantId={}, configKey={}", tenantId, command.getConfigKey());

        TenantConfig existing = configService.lambdaQuery()
                .eq(TenantConfig::getTenantId, tenantId)
                .eq(TenantConfig::getConfigKey, command.getConfigKey())
                .one();

        if (existing != null) {
            existing.setConfigValue(command.getConfigValue());
            existing.setUpdateTime(LocalDateTime.now());
            configService.updateById(existing);
        } else {
            TenantConfig newConfig = new TenantConfig();
            newConfig.setId(UUID.randomUUID().toString());
            newConfig.setTenantId(tenantId);
            newConfig.setConfigKey(command.getConfigKey());
            newConfig.setConfigValue(command.getConfigValue());
            newConfig.setCreateTime(LocalDateTime.now());
            newConfig.setUpdateTime(LocalDateTime.now());
            configService.save(newConfig);
        }
    }

    @Override
    public Map<String, String> getFeatureFlags(String tenantId) {
        log.debug("Dubbo查询功能开关: tenantId={}", tenantId);
        List<TenantConfig> configs = configService.lambdaQuery()
                .eq(TenantConfig::getTenantId, tenantId)
                .eq(TenantConfig::getConfigCategory, "FEATURE")
                .list();

        Map<String, String> flags = new HashMap<>();
        for (TenantConfig config : configs) {
            flags.put(config.getConfigKey(), config.getConfigValue());
        }
        return flags;
    }

    private TenantConfigDTO convertToDTO(TenantConfig config) {
        TenantConfigDTO dto = new TenantConfigDTO();
        dto.setId(config.getId());
        dto.setTenantId(config.getTenantId());
        dto.setConfigCategory(config.getConfigCategory());
        dto.setConfigKey(config.getConfigKey());
        dto.setConfigValue(config.getConfigValue());
        dto.setValueType(config.getValueType());
        dto.setDescription(config.getDescription());
        return dto;
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `mvn compile -pl scm-tenant/service -am -f com.scm.parent/pom.xml`
Expected: BUILD SUCCESS

---

### Task 13: Create TenantPackageDubboServiceImpl

**Files:**
- Create: `scm-tenant/service/src/main/java/scm/tenant/service/dubbo/TenantPackageDubboServiceImpl.java`

- [ ] **Step 1: Create TenantPackageDubboServiceImpl**

```java
package scm.tenant.service.dubbo;

import com.frog.tenant.api.command.TenantSubscribeCommand;
import com.frog.tenant.api.dto.subscription.TenantPackageDTO;
import com.frog.tenant.api.dto.subscription.TenantSubscriptionDTO;
import com.frog.tenant.api.service.TenantPackageDubboService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Component;
import scm.tenant.domain.entity.TenantPackage;
import scm.tenant.domain.entity.TenantSubscription;
import scm.tenant.service.ITenantPackageService;
import scm.tenant.service.ITenantSubscriptionService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@DubboService
@Component
@RequiredArgsConstructor
public class TenantPackageDubboServiceImpl implements TenantPackageDubboService {

    private final ITenantPackageService packageService;
    private final ITenantSubscriptionService subscriptionService;

    @Override
    public TenantPackageDTO getPackageById(String packageId) {
        log.debug("Dubbo查询套餐: packageId={}", packageId);
        TenantPackage pkg = packageService.lambdaQuery()
                .eq(TenantPackage::getId, packageId)
                .eq(TenantPackage::getDeleted, false)
                .one();
        return pkg == null ? null : convertPackageToDTO(pkg);
    }

    @Override
    public TenantPackageDTO getTenantCurrentPackage(String tenantId) {
        log.debug("Dubbo查询租户当前套餐: tenantId={}", tenantId);
        TenantSubscription subscription = subscriptionService.lambdaQuery()
                .eq(TenantSubscription::getTenantId, tenantId)
                .eq(TenantSubscription::getStatus, 1)
                .orderByDesc(TenantSubscription::getCreateTime)
                .last("LIMIT 1")
                .one();

        if (subscription == null) {
            return null;
        }

        return getPackageById(subscription.getPackageId());
    }

    @Override
    public List<TenantPackageDTO> listAvailablePackages() {
        log.debug("Dubbo查询可用套餐列表");
        List<TenantPackage> packages = packageService.lambdaQuery()
                .eq(TenantPackage::getEnabled, true)
                .eq(TenantPackage::getDeleted, false)
                .orderByAsc(TenantPackage::getSortOrder)
                .list();
        return packages.stream().map(this::convertPackageToDTO).toList();
    }

    @Override
    public TenantSubscriptionDTO getActiveSubscription(String tenantId) {
        log.debug("Dubbo查询活跃订阅: tenantId={}", tenantId);
        TenantSubscription subscription = subscriptionService.lambdaQuery()
                .eq(TenantSubscription::getTenantId, tenantId)
                .eq(TenantSubscription::getStatus, 1)
                .orderByDesc(TenantSubscription::getCreateTime)
                .last("LIMIT 1")
                .one();
        return subscription == null ? null : convertSubscriptionToDTO(subscription);
    }

    @Override
    public String subscribe(String tenantId, TenantSubscribeCommand command) {
        log.info("Dubbo订阅: tenantId={}, packageId={}", tenantId, command.getPackageId());

        TenantSubscription subscription = new TenantSubscription();
        subscription.setId(UUID.randomUUID().toString());
        subscription.setTenantId(tenantId);
        subscription.setPackageId(command.getPackageId());
        subscription.setSubscriptionType(command.getSubscriptionType());
        subscription.setAutoRenew(command.getAutoRenew());
        subscription.setStatus(0);
        subscription.setCreateTime(LocalDateTime.now());
        subscription.setUpdateTime(LocalDateTime.now());

        subscriptionService.save(subscription);
        log.info("Dubbo订阅成功: id={}", subscription.getId());
        return subscription.getId();
    }

    private TenantPackageDTO convertPackageToDTO(TenantPackage pkg) {
        TenantPackageDTO dto = new TenantPackageDTO();
        dto.setId(pkg.getId());
        dto.setPackageCode(pkg.getPackageCode());
        dto.setPackageName(pkg.getPackageName());
        dto.setPackageLevel(pkg.getPackageLevel());
        dto.setPriceMonthly(pkg.getPriceMonthly());
        dto.setPriceYearly(pkg.getPriceYearly());
        dto.setMaxUsers(pkg.getMaxUsers());
        dto.setMaxWarehouses(pkg.getMaxWarehouses());
        dto.setMaxSkus(pkg.getMaxSkus());
        dto.setMaxOrdersPerDay(pkg.getMaxOrdersPerDay());
        dto.setFeatures(pkg.getFeatures());
        dto.setEnabled(pkg.getEnabled());
        return dto;
    }

    private TenantSubscriptionDTO convertSubscriptionToDTO(TenantSubscription sub) {
        TenantSubscriptionDTO dto = new TenantSubscriptionDTO();
        dto.setId(sub.getId());
        dto.setTenantId(sub.getTenantId());
        dto.setPackageId(sub.getPackageId());
        dto.setSubscriptionType(sub.getSubscriptionType());
        dto.setStatus(sub.getStatus());
        dto.setStartDate(sub.getStartDate());
        dto.setEndDate(sub.getEndDate());
        dto.setAutoRenew(sub.getAutoRenew());
        dto.setActualPrice(sub.getActualPrice());
        dto.setPaymentStatus(sub.getPaymentStatus());
        dto.setCreateTime(sub.getCreateTime());
        return dto;
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `mvn compile -pl scm-tenant/service -am -f com.scm.parent/pom.xml`
Expected: BUILD SUCCESS

---

### Task 14: Delete old TenantDubboService

**Files:**
- Delete: `scm-tenant/api/src/main/java/com/frog/tenant/api/TenantDubboService.java`

- [ ] **Step 1: Verify no other files reference the old interface**

Run: `rg "import com.frog.tenant.api.TenantDubboService" --include "*.java"`
Expected: No matches (consumers should have been updated in Task 11-13)

- [ ] **Step 2: Delete the old file**

```bash
git rm scm-tenant/api/src/main/java/com/frog/tenant/api/TenantDubboService.java
```

- [ ] **Step 3: Verify full build**

Run: `mvn clean install -DskipTests -f com.scm.parent/pom.xml`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "refactor(tenant-api): restructure to enterprise-level CQRS layout

- Extract inner classes (TenantVO, TenantConfigVO) to standalone DTOs
- Split TenantDubboService into TenantDubboService, TenantConfigDubboService, TenantPackageDubboService
- Add CQRS separation: dto/, command/, query/ packages
- Add PageResult<T> generic pagination
- Add QuotaType enum for type-safe quota checking
- Add dubbo service implementations in service module
- Remove old single-file TenantDubboService"
```

---

### Task 15: Final verification

- [ ] **Step 1: Run full build with tests**

Run: `mvn clean install -f com.scm.parent/pom.xml`
Expected: BUILD SUCCESS

- [ ] **Step 2: Verify no compilation warnings**

Run: `mvn compile -pl scm-tenant/api,scm-tenant/service -am -f com.scm.parent/pom.xml 2>&1 | Select-String "WARNING"`
Expected: No warnings related to tenant module

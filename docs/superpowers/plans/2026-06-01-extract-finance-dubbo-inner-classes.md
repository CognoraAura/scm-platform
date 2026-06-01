# Extract FinanceDubboService Inner Classes

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract 5 inner classes from `FinanceDubboService.java` into standalone files under `request/` and `dto/` sub-packages, using Lombok instead of manual getters/setters.

**Architecture:** Move inner classes to `com.frog.finance.api.request` and `com.frog.finance.api.dto` packages. Follow existing codebase conventions (Lombok `@Data` + `@Accessors(chain = true)`, no Serializable). No DubboServiceImpl exists in the service module — only the API interface needs updating.

**Tech Stack:** Java, Lombok, Maven

---

## File Structure

| Action | File |
|--------|------|
| Create | `scm-finance/api/src/main/java/com/frog/finance/api/request/SettlementRequest.java` |
| Create | `scm-finance/api/src/main/java/com/frog/finance/api/request/FreightRequest.java` |
| Create | `scm-finance/api/src/main/java/com/frog/finance/api/dto/SettlementVO.java` |
| Create | `scm-finance/api/src/main/java/com/frog/finance/api/dto/InvoiceVO.java` |
| Create | `scm-finance/api/src/main/java/com/frog/finance/api/dto/FreightResult.java` |
| Modify | `scm-finance/api/src/main/java/com/frog/finance/api/FinanceDubboService.java` |
| Modify | `scm-finance/api/pom.xml` |

---

### Task 1: Add Lombok dependency to scm-finance/api/pom.xml

**Files:**
- Modify: `scm-finance/api/pom.xml`

The parent POM at `com.scm.parent/pom.xml` already defines Lombok `1.18.38` in `<dependencyManagement>`. The API module's `pom.xml` has an empty `<dependencies>` block.

- [ ] **Step 1: Add Lombok dependency**

```xml
<dependencies>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

---

### Task 2: Create request classes

**Files:**
- Create: `scm-finance/api/src/main/java/com/frog/finance/api/request/SettlementRequest.java`
- Create: `scm-finance/api/src/main/java/com/frog/finance/api/request/FreightRequest.java`

- [ ] **Step 1: Create SettlementRequest**

```java
package com.frog.finance.api.request;

import java.math.BigDecimal;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SettlementRequest {

    private Long orderId;
    private Long supplierId;
    private BigDecimal amount;
    private String settlementType;
    private Long applicantId;
    private String remark;
}
```

- [ ] **Step 2: Create FreightRequest**

```java
package com.frog.finance.api.request;

import java.math.BigDecimal;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FreightRequest {

    private Long warehouseId;
    private String destinationAddress;
    private BigDecimal weight;
    private BigDecimal volume;
    private String shippingMethod;
}
```

---

### Task 3: Create DTO/VO classes

**Files:**
- Create: `scm-finance/api/src/main/java/com/frog/finance/api/dto/SettlementVO.java`
- Create: `scm-finance/api/src/main/java/com/frog/finance/api/dto/InvoiceVO.java`
- Create: `scm-finance/api/src/main/java/com/frog/finance/api/dto/FreightResult.java`

- [ ] **Step 1: Create SettlementVO**

```java
package com.frog.finance.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SettlementVO {

    private Long id;
    private String settlementNo;
    private Long orderId;
    private Long supplierId;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 2: Create InvoiceVO**

```java
package com.frog.finance.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class InvoiceVO {

    private Long id;
    private String invoiceNo;
    private Long orderId;
    private Long supplierId;
    private BigDecimal amount;
    private BigDecimal taxAmount;
    private String invoiceType;
    private String status;
    private LocalDateTime issuedAt;
}
```

- [ ] **Step 3: Create FreightResult**

```java
package com.frog.finance.api.dto;

import java.math.BigDecimal;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FreightResult {

    private BigDecimal freightAmount;
    private BigDecimal insuranceAmount;
    private BigDecimal totalAmount;
    private String carrier;
    private Integer estimatedDays;
}
```

---

### Task 4: Update FinanceDubboService.java

**Files:**
- Modify: `scm-finance/api/src/main/java/com/frog/finance/api/FinanceDubboService.java`

- [ ] **Step 1: Replace file contents**

Remove all inner classes and update imports:

```java
package com.frog.finance.api;

import com.frog.finance.api.dto.FreightResult;
import com.frog.finance.api.dto.InvoiceVO;
import com.frog.finance.api.dto.SettlementVO;
import com.frog.finance.api.request.FreightRequest;
import com.frog.finance.api.request.SettlementRequest;

/**
 * 财务服务 Dubbo 接口
 *
 * <p>提供结算、发票、运费计算等核心功能，供其他微服务通过 RPC 调用。
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
public interface FinanceDubboService {

    /**
     * 创建结算单
     *
     * @param request 结算请求
     * @return 结算单信息
     */
    SettlementVO createSettlement(SettlementRequest request);

    /**
     * 根据 ID 查询发票
     *
     * @param id 发票 ID
     * @return 发票信息，不存在时返回 null
     */
    InvoiceVO getInvoiceById(Long id);

    /**
     * 计算运费
     *
     * @param request 运费计算请求
     * @return 运费计算结果
     */
    FreightResult calculateFreight(FreightRequest request);
}
```

---

### Task 5: Verify build

- [ ] **Step 1: Build the finance API module**

```bash
mvn clean install -pl scm-finance/api -am -f com.scm.parent/pom.xml
```

Expected: BUILD SUCCESS

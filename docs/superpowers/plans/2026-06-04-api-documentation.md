# API Documentation with Springdoc OpenAPI - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add OpenAPI 3.0 documentation across all services with Swagger UI and TypeScript client generation capability.

**Architecture:** Springdoc OpenAPI 3.0 integration with auto-configuration. Global API info, security schemes, and per-controller annotations.

**Tech Stack:** Springdoc OpenAPI 2.x, Swagger UI, OpenAPI Generator (TypeScript-Axios)

---

## File Structure

```
com.scm.parent/
  pom.xml                              ← Add springdoc dependency

scm-common/
  web/
    src/main/java/com/scmcloud/common/config/
      OpenApiConfig.java               ← CREATE - Global OpenAPI config

scm-auth/
  service/src/main/java/com/scmcloud/auth/
    controller/
      AuthController.java              ← MODIFY - Add @Operation annotations

scm-order/
  service/src/main/java/com/scmcloud/order/
    controller/
      OrdOrderController.java          ← MODIFY - Add @Operation annotations

scm-inventory/
  service/src/main/java/com/scmcloud/inventory/
    controller/
      InventoryController.java         ← MODIFY - Add @Operation annotations

scm-product/
  service/src/main/java/com/scmcloud/product/
    controller/
      ProdSpuController.java           ← MODIFY - Add @Operation annotations
```

---

### Task 1: Add Springdoc Dependency to Parent POM

**Files:**
- Modify: `com.scm.parent/pom.xml`

- [ ] **Step 1: Add springdoc-openapi dependency**

Add to `<dependencies>` section:
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
</dependency>
```

- [ ] **Step 2: Run build to verify**

```bash
mvn clean install -DskipTests -f com.scm.parent/pom.xml
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add com.scm.parent/pom.xml
git commit -m "deps: add springdoc-openapi for API documentation"
```

---

### Task 2: Create Global OpenAPI Configuration

**Files:**
- Create: `scm-common/web/src/main/java/com/scmcloud/common/config/OpenApiConfig.java`

- [ ] **Step 1: Create OpenAPI configuration**

```java
package com.scmcloud.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Global OpenAPI 3.0 configuration for all SCM services.
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:SCM Service}")
    private String applicationName;

    @Value("${server.port:8080}")
    private int serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SCM Platform API")
                        .version("1.0.0")
                        .description("Supply Chain Management Platform API Documentation")
                        .contact(new Contact()
                                .name("SCM Team")
                                .email("admin@scmcloud.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT token for authentication")));
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add scm-common/web/src/main/java/com/scmcloud/common/config/OpenApiConfig.java
git commit -m "feat(api): add global OpenAPI 3.0 configuration"
```

---

### Task 3: Annotate Auth Controllers

**Files:**
- Modify: `scm-auth/service/src/main/java/com/scmcloud/auth/controller/AuthController.java`

- [ ] **Step 1: Read existing AuthController**

```bash
Read scm-auth/service/src/main/java/com/scmcloud/auth/controller/AuthController.java
```

- [ ] **Step 2: Add OpenAPI annotations**

Add imports:
```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
```

Add class-level annotation:
```java
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
```

Add method-level annotations:
```java
@Operation(summary = "User login", description = "Authenticate user with username and password")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Login successful",
            content = @Content(schema = @Schema(implementation = LoginResponse.class))),
    @ApiResponse(responseCode = "401", description = "Invalid credentials"),
    @ApiResponse(responseCode = "403", description = "Account locked or disabled")
})
@PostMapping("/login")
public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
    // existing implementation
}
```

- [ ] **Step 3: Commit**

```bash
git add scm-auth/
git commit -m "feat(api): add OpenAPI annotations to AuthController"
```

---

### Task 4: Annotate Order Controllers

**Files:**
- Modify: `scm-order/service/src/main/java/com/scmcloud/order/controller/OrdOrderController.java`

- [ ] **Step 1: Read existing OrdOrderController**

```bash
Read scm-order/service/src/main/java/com/scmcloud/order/controller/OrdOrderController.java
```

- [ ] **Step 2: Add OpenAPI annotations**

Add imports:
```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
```

Add class-level annotation:
```java
@Tag(name = "Orders", description = "Order management endpoints")
```

Add method-level annotations:
```java
@Operation(summary = "Create order", description = "Create a new order with items")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Order created successfully"),
    @ApiResponse(responseCode = "400", description = "Invalid order data"),
    @ApiResponse(responseCode = "409", description = "Insufficient stock")
})
@PostMapping
public ApiResponse<OrdOrder> createOrder(@RequestBody CreateOrderRequest request) {
    // existing implementation
}

@Operation(summary = "Get order by ID", description = "Retrieve order details by order ID")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Order found"),
    @ApiResponse(responseCode = "404", description = "Order not found")
})
@GetMapping("/{id}")
public ApiResponse<OrdOrder> getOrder(
        @Parameter(description = "Order ID", required = true) @PathVariable Long id) {
    // existing implementation
}
```

- [ ] **Step 3: Commit**

```bash
git add scm-order/
git commit -m "feat(api): add OpenAPI annotations to OrderController"
```

---

### Task 5: Annotate Inventory Controllers

**Files:**
- Modify: `scm-inventory/service/src/main/java/com/scmcloud/inventory/controller/InventoryController.java`

- [ ] **Step 1: Read existing InventoryController**

```bash
Read scm-inventory/service/src/main/java/com/scmcloud/inventory/controller/InventoryController.java
```

- [ ] **Step 2: Add OpenAPI annotations**

Add class-level annotation:
```java
@Tag(name = "Inventory", description = "Inventory management endpoints")
```

Add method-level annotations:
```java
@Operation(summary = "Get stock level", description = "Get current stock level for a SKU")
@GetMapping("/stock/{skuId}")
public ApiResponse<StockResponse> getStock(
        @Parameter(description = "SKU ID", required = true) @PathVariable String skuId) {
    // existing implementation
}

@Operation(summary = "Deduct stock", description = "Deduct stock for an order (TCC reserve)")
@PostMapping("/deduct")
public ApiResponse<Void> deductStock(@RequestBody StockDeductRequest request) {
    // existing implementation
}
```

- [ ] **Step 3: Commit**

```bash
git add scm-inventory/
git commit -m "feat(api): add OpenAPI annotations to InventoryController"
```

---

### Task 6: Annotate Product Controllers

**Files:**
- Modify: `scm-product/service/src/main/java/com/scmcloud/product/controller/ProdSpuController.java`
- Modify: `scm-product/service/src/main/java/com/scmcloud/product/controller/ProdSkuController.java`

- [ ] **Step 1: Read existing controllers**

```bash
Read scm-product/service/src/main/java/com/scmcloud/product/controller/ProdSpuController.java
Read scm-product/service/src/main/java/com/scmcloud/product/controller/ProdSkuController.java
```

- [ ] **Step 2: Add OpenAPI annotations**

Add class-level annotations:
```java
@Tag(name = "Products", description = "Product catalog management")
```

Add method-level annotations:
```java
@Operation(summary = "List products", description = "Get paginated list of products")
@GetMapping
public ApiResponse<Page<ProdSpu>> listProducts(
        @Parameter(description = "Page number") @RequestParam(defaultValue = "1") int page,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
    // existing implementation
}

@Operation(summary = "Get product details", description = "Get product with SKUs")
@GetMapping("/{id}")
public ApiResponse<ProductDetail> getProduct(
        @Parameter(description = "Product ID", required = true) @PathVariable Long id) {
    // existing implementation
}
```

- [ ] **Step 3: Commit**

```bash
git add scm-product/
git commit -m "feat(api): add OpenAPI annotations to Product controllers"
```

---

### Task 7: Configure Swagger UI Access

**Files:**
- Modify: `application.yml` or `application.properties` in each service

- [ ] **Step 1: Add Swagger UI configuration**

Add to each service's `application.yml`:
```yaml
springdoc:
  api-docs:
    enabled: true
    path: /v3/api-docs
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
    tags-sorter: alpha
    operations-sorter: method
  packages-to-scan: com.scmcloud.*.controller
```

- [ ] **Step 2: Verify Swagger UI accessible**

Start a service and navigate to:
```
http://localhost:{port}/swagger-ui.html
```

Expected: Swagger UI displays all endpoints

- [ ] **Step 3: Commit**

```bash
git add .
git commit -m "config: enable Swagger UI for all services"
```

---

### Task 8: Generate TypeScript Client

**Files:**
- Create: `scm-web/openapitools.json` (if not exists)

- [ ] **Step 1: Start auth service**

```bash
cd scm-auth
mvn spring-boot:run
```

- [ ] **Step 2: Generate TypeScript client**

```bash
cd scm-web
npx openapi-generator-cli generate \
  -i http://localhost:8106/v3/api-docs \
  -g typescript-axios \
  -o src/api/generated
```

- [ ] **Step 3: Verify generated client**

Check that `scm-web/src/api/generated/api.ts` exists and contains proper types.

- [ ] **Step 4: Commit**

```bash
git add scm-web/src/api/generated/
git commit -m "feat(api): generate TypeScript client from OpenAPI spec"
```

---

### Task 9: Build Verification

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

- [ ] **Step 3: Verify Swagger UI on multiple services**

Start at least 2 services and verify:
- Auth: http://localhost:8106/swagger-ui.html
- Order: http://localhost:8201/swagger-ui.html

Expected: All endpoints visible with proper documentation

---

## Summary

| Task | Description | Status |
|------|-------------|--------|
| T052 | Add Springdoc dependency | PENDING |
| T053 | Global OpenAPI configuration | PENDING |
| T054 | Annotate Auth controllers | PENDING |
| T055 | Annotate Order controllers | PENDING |
| T056 | Annotate Inventory controllers | PENDING |
| T057 | Annotate Product controllers | PENDING |
| T058 | Configure Swagger UI | PENDING |
| T059 | Generate TypeScript client | PENDING |
| T060 | Build verification | PENDING |

**Total Estimated Effort:** 3-4 days

**Priority:** P1 - Start in Week 5

**Dependencies:** None

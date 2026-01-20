# SCM Platform 开发规范

> **版本**: v2.0.0
> **最后更新**: 2025-12-26
> **维护者**: SCM Platform Team

本文档提供SCM平台的完整开发规范，包括代码规范、配置管理、API设计、数据库规范等，便于AI和开发人员快速理解项目标准。

---

## 📋 目录

1. [代码规范](#1-代码规范)
2. [配置管理](#2-配置管理)
3. [API设计规范](#3-api设计规范)
4. [数据库规范](#4-数据库规范)
5. [测试规范](#5-测试规范)
6. [Git工作流](#6-git工作流)
7. [命名规范](#7-命名规范)
8. [日志规范](#8-日志规范)
9. [异常处理](#9-异常处理)
10. [性能规范](#10-性能规范)
11. [安全规范](#11-安全规范)
12. [认证服务设计](#12-认证服务设计)
13. [快速参考](#13-快速参考)
14. [scm-common 共享模块设计](#14-scm-common-共享模块设计)

---

## 1. 代码规范

### 1.1 Java 编码规范

遵循 [阿里巴巴 Java 开发手册](https://github.com/alibaba/p3c) 与 Google Java Style Guide。

**强制要求**:
- ✅ 使用 Java 21 特性（Record、Pattern Matching、Virtual Threads）
- ✅ 使用 Lombok 减少样板代码（@Data, @Builder, @Slf4j, @RequiredArgsConstructor）
- ✅ 所有公共方法必须有 Javadoc 注释
- ✅ 代码格式化使用 Google Java Format（自动格式化）
- ✅ 使用 Stream API 处理集合（禁止过度使用 for 循环）

### 1.2 包结构规范

**统一包名格式**: `com.frog.{module}`

```
✅ com.frog.product
✅ com.frog.order
✅ com.frog.inventory
✅ com.frog.system

❌ scm.product
❌ scm.order
```

**标准包结构**:
```
scm-{module}/
├── api/                          # Dubbo RPC 接口
│   └── src/main/java/
│       └── com.frog.{module}.api/
│           ├── dto/              # 数据传输对象
│           ├── enums/            # 枚举
│           └── {Module}DubboService.java
│
└── service/                      # 服务实现
    ├── src/main/java/
    │   └── com.frog.{module}/
    │       ├── {Module}ServiceApplication.java  # 启动类
    │       ├── controller/        # REST 控制器
    │       ├── service/           # 业务逻辑层
    │       │   ├── I{Entity}Service.java
    │       │   └── impl/
    │       │       └── {Entity}ServiceImpl.java
    │       ├── mapper/            # MyBatis Mapper
    │       ├── domain/
    │       │   ├── entity/        # 数据库实体
    │       │   ├── dto/           # 数据传输对象
    │       │   └── vo/            # 视图对象
    │       ├── config/            # 配置类
    │       ├── util/              # 工具类
    │       └── exception/         # 自定义异常
    │
    └── src/main/resources/
        ├── application.yml
        ├── application-dev.yml
        ├── application-prod.yml
        └── mapper/                # MyBatis XML
```

### 1.3 Controller层规范

#### 标准模板

```java
package com.frog.{module}.controller;

import com.frog.common.domain.PageResult;
import com.frog.common.log.annotation.AuditLog;
import com.frog.common.response.ApiResponse;
import com.frog.{module}.domain.dto.{Resource}DTO;
import com.frog.{module}.service.I{Resource}Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * {资源}管理控制器
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@RestController
@RequestMapping("/api/v1/{module}/{resource}")
@RequiredArgsConstructor
@Tag(name = "{资源}管理", description = "{资源}CRUD接口")
public class {Resource}Controller {

    private final I{Resource}Service {resource}Service;

    /**
     * 分页查询{资源}列表
     */
    @GetMapping
    @PreAuthorize("hasAuthority('{module}:{resource}:list')")
    @Operation(summary = "分页查询{资源}")
    public ApiResponse<PageResult<{Resource}DTO>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword) {

        PageResult<{Resource}DTO> result = {resource}Service.listPage(page, size, keyword);
        return ApiResponse.success(result);
    }

    /**
     * 查询{资源}详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('{module}:{resource}:list')")
    @Operation(summary = "查询{资源}详情")
    public ApiResponse<{Resource}DTO> getById(
            @Parameter(description = "{资源}ID") @PathVariable UUID id) {

        {Resource}DTO dto = {resource}Service.getById(id);
        return ApiResponse.success(dto);
    }

    /**
     * 新增{资源}
     */
    @PostMapping
    @PreAuthorize("hasAuthority('{module}:{resource}:add')")
    @AuditLog(operation = "新增{资源}", businessType = "{RESOURCE}")
    @Operation(summary = "新增{资源}")
    public ApiResponse<Void> add(
            @Validated @RequestBody {Resource}DTO dto) {

        {resource}Service.add(dto);
        return ApiResponse.success();
    }

    /**
     * 修改{资源}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('{module}:{resource}:edit')")
    @AuditLog(operation = "修改{资源}", businessType = "{RESOURCE}")
    @Operation(summary = "修改{资源}")
    public ApiResponse<Void> update(
            @Parameter(description = "{资源}ID") @PathVariable UUID id,
            @Validated @RequestBody {Resource}DTO dto) {

        dto.setId(id);
        {resource}Service.update(dto);
        return ApiResponse.success();
    }

    /**
     * 删除{资源}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('{module}:{resource}:delete')")
    @AuditLog(operation = "删除{资源}", businessType = "{RESOURCE}", riskLevel = 4)
    @Operation(summary = "删除{资源}")
    public ApiResponse<Void> delete(
            @Parameter(description = "{资源}ID") @PathVariable UUID id) {

        {resource}Service.delete(id);
        return ApiResponse.success();
    }
}
```

#### 关键注解说明

| 注解 | 用途 | 必需 |
|-----|------|-----|
| `@RequiredArgsConstructor` | Lombok依赖注入 | ✅ 必需 |
| `@Tag` | Swagger文档分组 | ✅ 必需 |
| `@Operation` | Swagger接口说明 | ✅ 必需 |
| `@PreAuthorize` | 权限控制 | ✅ 必需 |
| `@AuditLog` | 审计日志（敏感操作） | ⚠️  建议 |
| `@Validated/@Valid` | 参数校验 | ✅ 必需 |

#### URL路径规范

```
/api/v1/{module}/{resource}          # 列表/新增
/api/v1/{module}/{resource}/{id}     # 详情/修改/删除

示例:
/api/v1/product/brands               # 品牌列表
/api/v1/order/orders/{id}            # 订单详情
/api/v1/inventory/stocks             # 库存列表
```

#### 统一返回类型

**所有Controller方法必须返回 `ApiResponse<T>`**

```java
// 成功返回数据
return ApiResponse.success(data);

// 成功无数据
return ApiResponse.success();

// 分页返回
return ApiResponse.success(PageResult.of(page));

// 失败返回
return ApiResponse.error("操作失败");
return ApiResponse.error(ErrorCode.BUSINESS_ERROR, "业务错误");
```

**响应格式**:
```json
{
  "code": 200,
  "message": "Success",
  "data": { ... },
  "traceId": "550e8400-...",
  "timestamp": "2025-12-26T10:00:00Z"
}
```

### 1.4 Service层规范

#### 接口定义

```java
package com.frog.{module}.service;

import com.frog.common.domain.PageResult;
import com.frog.{module}.domain.dto.{Resource}DTO;

import java.util.UUID;

/**
 * {资源}业务接口
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
public interface I{Resource}Service {

    /**
     * 分页查询
     */
    PageResult<{Resource}DTO> listPage(Integer page, Integer size, String keyword);

    /**
     * 根据ID查询
     */
    {Resource}DTO getById(UUID id);

    /**
     * 新增
     */
    void add({Resource}DTO dto);

    /**
     * 修改
     */
    void update({Resource}DTO dto);

    /**
     * 删除
     */
    void delete(UUID id);
}
```

#### 实现类模板

```java
package com.frog.{module}.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frog.common.domain.PageResult;
import com.frog.common.exception.BusinessException;
import com.frog.{module}.domain.dto.{Resource}DTO;
import com.frog.{module}.domain.entity.{Resource};
import com.frog.{module}.mapper.{Resource}Mapper;
import com.frog.{module}.service.I{Resource}Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * {资源}业务实现
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
@DS("{datasource}")  // 多租户数据源: "product", "order", "inventory"等
public class {Resource}ServiceImpl extends ServiceImpl<{Resource}Mapper, {Resource}>
        implements I{Resource}Service {

    private final {Resource}Mapper {resource}Mapper;

    @Override
    public PageResult<{Resource}DTO> listPage(Integer page, Integer size, String keyword) {
        log.debug("分页查询{资源}: page={}, size={}, keyword={}", page, size, keyword);

        Page<{Resource}> entityPage = lambdaQuery()
                .like(keyword != null, {Resource}::getName, keyword)
                .orderByDesc({Resource}::getCreatedAt)
                .page(new Page<>(page, size));

        return PageResult.of(entityPage, this::convertToDTO);
    }

    @Override
    public {Resource}DTO getById(UUID id) {
        log.debug("查询{资源}详情: id={}", id);

        {Resource} entity = {resource}Mapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("{资源}不存在");
        }

        return convertToDTO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add({Resource}DTO dto) {
        log.info("新增{资源}: {}", dto);

        // 1. 业务校验
        validate{Resource}(dto);

        // 2. DTO转Entity
        {Resource} entity = convertToEntity(dto);

        // 3. 保存
        {resource}Mapper.insert(entity);

        log.info("新增{资源}成功: id={}", entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update({Resource}DTO dto) {
        log.info("修改{资源}: {}", dto);

        // 1. 检查存在性
        {Resource} entity = {resource}Mapper.selectById(dto.getId());
        if (entity == null) {
            throw new BusinessException("{资源}不存在");
        }

        // 2. 业务校验
        validate{Resource}(dto);

        // 3. 更新
        entity = convertToEntity(dto);
        {resource}Mapper.updateById(entity);

        log.info("修改{资源}成功: id={}", entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(UUID id) {
        log.info("删除{资源}: id={}", id);

        // 逻辑删除
        int rows = {resource}Mapper.deleteById(id);
        if (rows == 0) {
            throw new BusinessException("{资源}不存在");
        }

        log.info("删除{资源}成功: id={}", id);
    }

    /**
     * 业务校验
     */
    private void validate{Resource}({Resource}DTO dto) {
        // 实现具体的业务校验逻辑
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new BusinessException("名称不能为空");
        }
    }

    /**
     * Entity转DTO
     */
    private {Resource}DTO convertToDTO({Resource} entity) {
        // 使用MapStruct或手动转换
        return new {Resource}DTO();
    }

    /**
     * DTO转Entity
     */
    private {Resource} convertToEntity({Resource}DTO dto) {
        // 使用MapStruct或手动转换
        return new {Resource}();
    }
}
```

#### 关键注解说明

| 注解 | 用途 | 必需 |
|-----|------|-----|
| `@Service` | Spring Bean | ✅ 必需 |
| `@Slf4j` | Lombok日志 | ✅ 必需 |
| `@RequiredArgsConstructor` | 依赖注入 | ✅ 必需 |
| `@DS("{datasource}")` | 多租户数据源路由 | ✅ 必需 |
| `@Transactional` | 事务管理 | ✅ 必需（写操作） |

#### 分布式事务示例

```java
/**
 * 订单服务实现类
 *
 * <p>负责订单的创建、查询、状态流转等核心业务逻辑。
 * 使用 Seata 分布式事务保证数据一致性。
 *
 * @author Zhang San
 * @since 2025-12-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements IOrderService {

    private final OrderMapper orderMapper;
    private final InventoryClient inventoryClient;

    /**
     * 创建订单
     *
     * @param request 订单创建请求
     * @return 订单 DTO
     * @throws BusinessException 库存不足、商品不存在等业务异常
     */
    @Override
    @GlobalTransactional(name = "create-order", rollbackFor = Exception.class)
    public OrderDTO createOrder(CreateOrderRequest request) {
        log.info("Creating order for user: {}", request.getUserId());

        // 1. 参数校验
        validateOrderRequest(request);

        // 2. 库存扣减
        inventoryClient.deductStock(request.getItems());

        // 3. 创建订单
        Order order = OrderConverter.toEntity(request);
        orderMapper.insert(order);

        log.info("Order created successfully: {}", order.getOrderNo());
        return OrderConverter.toDTO(order);
    }
}
```

### 1.5 Entity层规范

#### 标准模板

```java
package com.frog.{module}.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * {资源}实体
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("{table_name}")
public class {Resource} implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private UUID id;

    /**
     * 业务字段示例
     */
    @TableField("name")
    private String name;

    /**
     * 租户ID（多租户隔离）
     */
    @TableField("tenant_id")
    private String tenantId;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 创建人ID
     */
    @TableField("created_by")
    private UUID createdBy;

    /**
     * 更新人ID
     */
    @TableField("updated_by")
    private UUID updatedBy;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    @TableField("deleted")
    private Boolean deleted;
}
```

#### 字段规范

**必需字段**（所有Entity都应包含）:

| 字段 | 类型 | 注解 | 说明 |
|-----|------|------|------|
| `id` | UUID | `@TableId(type = IdType.ASSIGN_UUID)` | 主键 |
| `tenant_id` | String | `@TableField("tenant_id")` | 租户ID |
| `created_at` | LocalDateTime | `@TableField(fill = FieldFill.INSERT)` | 创建时间 |
| `updated_at` | LocalDateTime | `@TableField(fill = FieldFill.INSERT_UPDATE)` | 更新时间 |
| `created_by` | UUID | `@TableField("created_by")` | 创建人 |
| `updated_by` | UUID | `@TableField("updated_by")` | 更新人 |
| `deleted` | Boolean | `@TableLogic` | 逻辑删除 |

### 1.6 DTO规范

```java
package com.frog.{module}.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.UUID;

/**
 * {资源}DTO
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Data
@Schema(description = "{资源}传输对象")
public class {Resource}DTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    private UUID id;

    @NotBlank(message = "名称不能为空")
    @Schema(description = "名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotNull(message = "状态不能为空")
    @Schema(description = "状态: 0-禁用 1-启用")
    private Integer status;
}
```

### 1.7 依赖管理

**依赖注入规范**:

```java
// ✅ 推荐：使用 @RequiredArgsConstructor (构造器注入)
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;

    // Lombok自动生成构造器
}

// ❌ 不推荐：字段注入
@Service
public class UserService {

    @Autowired  // ❌ 不推荐
    private UserMapper userMapper;
}
```

**依赖版本管理**:
- ✅ 所有依赖版本在父 POM 中统一管理
- ✅ 禁止使用 SNAPSHOT 版本依赖（生产环境）
- ✅ 定期检查依赖安全漏洞（OWASP Dependency Check）
- ✅ 禁止引入功能重复的依赖（如同时引入 Gson 和 Jackson）

---

## 2. 配置管理

### 2.1 application.yml 标准配置

```yaml
# 服务名称
spring:
  application:
    name: scm-{module}-service

  # 数据源配置（动态多数据源）
  datasource:
    dynamic:
      # 主数据源
      primary: {module}
      # 严格模式（未匹配到数据源抛出异常）
      strict: true
      datasource:
        # 业务数据源
        {module}:
          driver-class-name: org.postgresql.Driver
          url: jdbc:postgresql://${DB_HOST:localhost}:5432/db_{module}
          username: ${DB_USERNAME:admin}
          password: ${DB_PASSWORD:admin123}
          hikari:
            minimum-idle: 5
            maximum-pool-size: 20
            connection-timeout: 30000
            idle-timeout: 600000
            max-lifetime: 1800000

  # Redis配置
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    database: 0
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 2
        max-wait: -1ms
    timeout: 5000ms

  # Jackson序列化配置
  jackson:
    time-zone: GMT+8
    date-format: yyyy-MM-dd HH:mm:ss
    serialization:
      write-dates-as-timestamps: false
      fail-on-empty-beans: false
    deserialization:
      fail-on-unknown-properties: false

  # Nacos配置
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER:localhost:8848}
        namespace: ${NACOS_NAMESPACE:scm-dev}
      config:
        server-addr: ${NACOS_SERVER:localhost:8848}
        namespace: ${NACOS_NAMESPACE:scm-dev}
        file-extension: yaml

# MyBatis-Plus配置
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: com.frog.{module}.domain.entity

  global-config:
    db-config:
      id-type: ASSIGN_UUID
      logic-delete-field: deleted
      logic-delete-value: true
      logic-not-delete-value: false
    banner: false

  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: false
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl

# Seata分布式事务
seata:
  enabled: true
  application-id: scm-{module}
  tx-service-group: scm-tx-group
  service:
    vgroup-mapping:
      scm-tx-group: default
    grouplist:
      default: ${SEATA_SERVER_ADDR:localhost:8091}

# XXL-Job配置
xxl:
  job:
    admin:
      addresses: ${XXL_JOB_ADMIN:http://localhost:8088/xxl-job-admin}
    executor:
      appname: scm-{module}-executor
      port: 0  # 自动分配端口
      logpath: /data/applogs/xxl-job/jobhandler
      logretentiondays: 30

# Actuator监控端点
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true

# 日志配置
logging:
  level:
    root: INFO
    com.frog.{module}: DEBUG
    com.baomidou.mybatisplus: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

# 服务端口
server:
  port: ${SERVER_PORT:8201}
```

### 2.2 Mapper XML 配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.frog.{module}.mapper.{Resource}Mapper">

    <!-- 基础结果映射 -->
    <resultMap id="BaseResultMap" type="com.frog.{module}.domain.entity.{Resource}">
        <id column="id" property="id" jdbcType="OTHER"/>
        <result column="name" property="name" jdbcType="VARCHAR"/>
        <result column="tenant_id" property="tenantId" jdbcType="VARCHAR"/>
        <result column="created_at" property="createdAt" jdbcType="TIMESTAMP"/>
        <result column="updated_at" property="updatedAt" jdbcType="TIMESTAMP"/>
        <result column="created_by" property="createdBy" jdbcType="OTHER"/>
        <result column="updated_by" property="updatedBy" jdbcType="OTHER"/>
        <result column="deleted" property="deleted" jdbcType="BOOLEAN"/>
    </resultMap>

    <!-- 自定义查询示例 -->
    <select id="selectByCustomCondition" resultMap="BaseResultMap">
        SELECT * FROM {table_name}
        WHERE deleted = false
        <if test="name != null and name != ''">
            AND name LIKE CONCAT('%', #{name}, '%')
        </if>
        ORDER BY created_at DESC
    </select>

</mapper>
```

---

## 3. API设计规范

### 3.1 RESTful API 设计

遵循 REST 最佳实践：

**URL 设计**:
```
GET    /api/v1/orders              # 查询订单列表
GET    /api/v1/orders/{id}         # 查询单个订单
POST   /api/v1/orders              # 创建订单
PUT    /api/v1/orders/{id}         # 完整更新订单
PATCH  /api/v1/orders/{id}         # 部分更新订单
DELETE /api/v1/orders/{id}         # 删除订单

# 嵌套资源
GET    /api/v1/orders/{id}/items   # 查询订单明细
POST   /api/v1/orders/{id}/cancel  # 取消订单（操作）
```

**HTTP 状态码**:
- `200 OK` - 成功
- `201 Created` - 创建成功
- `204 No Content` - 删除成功
- `400 Bad Request` - 参数错误
- `401 Unauthorized` - 未认证
- `403 Forbidden` - 无权限
- `404 Not Found` - 资源不存在
- `409 Conflict` - 资源冲突
- `500 Internal Server Error` - 服务器错误

**统一响应格式**:
```java
@Data
@Builder
public class ApiResponse<T> {
    private Integer code;        // 业务状态码
    private String message;      // 消息
    private T data;              // 数据
    private String traceId;      // 链路追踪ID
    private Long timestamp;      // 时间戳

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
            .code(200)
            .message("Success")
            .data(data)
            .traceId(TraceContext.getTraceId())
            .timestamp(System.currentTimeMillis())
            .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
            .code(500)
            .message(message)
            .traceId(TraceContext.getTraceId())
            .timestamp(System.currentTimeMillis())
            .build();
    }
}
```

### 3.2 Swagger / OpenAPI 规范

所有 API 必须使用 Swagger v3 注解：

```java
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "订单管理", description = "订单相关接口")
@RequiredArgsConstructor
public class OrderController {

    private final IOrderService orderService;

    @PostMapping
    @Operation(summary = "创建订单", description = "用户下单接口")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "参数错误")
    })
    public ApiResponse<OrderDTO> createOrder(
        @RequestBody @Valid CreateOrderRequest request
    ) {
        OrderDTO order = orderService.createOrder(request);
        return ApiResponse.success(order);
    }
}
```

### 3.3 分页查询

统一使用 PageResult：

```java
@Data
public class PageResult<T> {
    private Long total;          // 总记录数
    private Integer page;        // 当前页码
    private Integer size;        // 每页大小
    private Integer pages;       // 总页数
    private List<T> records;     // 数据列表
}

// 使用 MyBatis-Plus 分页
Page<Order> page = new Page<>(pageNum, pageSize);
IPage<Order> result = orderMapper.selectPage(page, queryWrapper);
return PageResult.of(result);
```

---

## 4. 数据库规范

### 4.1 表设计规范

**命名规范**:
- 表名：`{module}_{entity}` (全小写，下划线分隔)
- 字段名：`column_name` (全小写，下划线分隔)
- 主键：统一使用 `id` (UUID v7)
- 索引：`idx_{table}_{column}` (普通索引), `uk_{table}_{column}` (唯一索引)

**示例**:
```sql
CREATE TABLE ord_order (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_no VARCHAR(32) NOT NULL,
    user_id UUID NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING_PAYMENT',
    tenant_id VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    deleted BOOLEAN DEFAULT FALSE,

    CONSTRAINT uk_order_no UNIQUE (order_no)
);

CREATE INDEX idx_order_user_id ON ord_order(user_id);
CREATE INDEX idx_order_status ON ord_order(status) WHERE deleted = FALSE;
CREATE INDEX idx_order_tenant_id ON ord_order(tenant_id);
```

### 4.2 字段规范

**必需字段**:
- `id` - 主键（UUID）
- `tenant_id` - 租户ID（多租户场景）
- `created_at` - 创建时间（TIMESTAMPTZ）
- `updated_at` - 更新时间（TIMESTAMPTZ）
- `deleted` - 逻辑删除标记（BOOLEAN，默认 FALSE）

**可选字段**:
- `created_by` - 创建人（UUID）
- `updated_by` - 更新人（UUID）
- `remark` - 备注（TEXT）

### 4.3 索引规范

- ✅ 所有外键字段必须加索引
- ✅ 高频查询字段添加组合索引
- ✅ 使用部分索引过滤 deleted = FALSE
- ✅ 避免过多索引（单表不超过 5 个）
- ✅ 多租户场景必须为 tenant_id 创建索引

---

## 5. 测试规范

### 5.1 单元测试

**要求**:
- ✅ 核心业务逻辑覆盖率 > 80%
- ✅ 使用 JUnit 5 + Mockito
- ✅ 测试类命名：`{Class}Test`
- ✅ 测试方法命名：`should{ExpectedBehavior}_when{StateUnderTest}`

**示例**:
```java
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private InventoryClient inventoryClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void shouldCreateOrder_whenStockIsAvailable() {
        // Given
        CreateOrderRequest request = CreateOrderRequest.builder()
            .userId(UUID.randomUUID())
            .items(List.of(createOrderItem()))
            .build();
        when(inventoryClient.checkStock(any())).thenReturn(true);

        // When
        OrderDTO result = orderService.createOrder(request);

        // Then
        assertNotNull(result);
        verify(orderMapper).insert(any(Order.class));
    }

    @Test
    void shouldThrowException_whenStockIsInsufficient() {
        // Given
        when(inventoryClient.checkStock(any())).thenReturn(false);

        // When & Then
        assertThrows(BusinessException.class,
            () -> orderService.createOrder(request));
    }
}
```

### 5.2 集成测试

使用 Testcontainers 进行集成测试：

```java
@SpringBootTest
@Testcontainers
class OrderIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("test_db");

    @Autowired
    private IOrderService orderService;

    @Test
    void shouldCreateAndQueryOrder() {
        // 测试完整流程
        OrderDTO created = orderService.createOrder(request);
        OrderDTO queried = orderService.getById(created.getId());

        assertEquals(created.getOrderNo(), queried.getOrderNo());
    }
}
```

### 5.3 性能测试

使用 JMH 进行基准测试：

```java
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class InventoryBenchmark {

    @Benchmark
    public void testRedisDeduction() {
        inventoryService.deductStock(skuId, 1);
    }
}
```

---

## 6. Git工作流

### 6.1 分支策略

采用 **Git Flow** 模型：

```
master       # 生产环境分支（只读）
  ↑
develop      # 开发分支
  ↑
feature/*    # 功能分支 (feature/order-service)
hotfix/*     # 紧急修复 (hotfix/fix-inventory-bug)
release/*    # 发布分支 (release/v1.0.0)
```

### 6.2 Commit 规范

遵循 [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type**:
- `feat` - 新功能
- `fix` - Bug 修复
- `docs` - 文档更新
- `style` - 代码格式（不影响功能）
- `refactor` - 重构
- `perf` - 性能优化
- `test` - 测试
- `chore` - 构建/工具变更

**示例**:
```
feat(order): add distributed transaction support for order creation

- Integrate Seata AT mode
- Add @GlobalTransactional annotation
- Implement inventory rollback on failure

Closes #123
```

### 6.3 Pull Request 规范

- ✅ PR 标题：`[类型] 简短描述`
- ✅ PR 描述：包含背景、改动、测试、截图
- ✅ 至少 1 人 Code Review 通过
- ✅ CI 全部通过（构建、测试、代码质量）
- ✅ 无冲突

---

## 7. 命名规范

### 7.1 类命名

| 类型 | 命名规则 | 示例 |
|-----|---------|------|
| Controller | `{Entity}Controller` | `OrderController` |
| Service 接口 | `I{Entity}Service` | `IOrderService` |
| Service 实现 | `{Entity}ServiceImpl` | `OrderServiceImpl` |
| Mapper | `{Entity}Mapper` | `OrderMapper` |
| Entity | `{Entity}` | `Order` |
| DTO | `{Entity}DTO` | `OrderDTO` |
| VO | `{Entity}VO` | `OrderVO` |
| Request | `{Action}{Entity}Request` | `CreateOrderRequest` |
| Response | `{Action}{Entity}Response` | `CreateOrderResponse` |

### 7.2 方法命名

| 操作 | 命名规则 | 示例 |
|-----|---------|------|
| 查询单个 | `getBy{Condition}` | `getById`, `getByOrderNo` |
| 查询列表 | `listBy{Condition}` | `listByUserId` |
| 分页查询 | `pageBy{Condition}` | `pageByStatus` |
| 创建 | `create` | `createOrder` |
| 更新 | `update` | `updateOrder` |
| 删除 | `delete` | `deleteById` |
| 统计 | `count{Entity}` | `countOrders` |
| 存在判断 | `exists{Entity}` | `existsOrderNo` |

---

## 8. 日志规范

### 8.1 日志级别

- `ERROR` - 系统错误，需要立即处理
- `WARN` - 警告信息，需要关注
- `INFO` - 重要业务流程（创建订单、扣减库存）
- `DEBUG` - 调试信息（本地开发）
- `TRACE` - 详细追踪（性能分析）

### 8.2 日志格式

```java
// ✅ 推荐
log.info("Creating order for user: {}, items: {}", userId, items.size());
log.error("Failed to deduct inventory for SKU: {}", skuId, exception);

// ❌ 禁止
log.info("Creating order for user: " + userId); // 字符串拼接
log.error(exception.getMessage()); // 丢失堆栈
```

### 8.3 敏感数据脱敏

```java
log.info("User login: {}, phone: {}", username, maskPhone(phone));
// 输出: User login: zhangsan, phone: 138****8000
```

---

## 9. 异常处理

### 9.1 异常分类

```java
// 业务异常（可预期）
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
}

// 系统异常（不可预期）
public class SystemException extends RuntimeException {
}

// 第三方服务异常
public class ThirdPartyException extends RuntimeException {
}
```

### 9.2 全局异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<?> handleBusinessException(BusinessException e) {
        log.warn("Business exception: {}", e.getMessage());
        return ApiResponse.error(e.getErrorCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<?> handleException(Exception e) {
        log.error("Unexpected exception", e);
        return ApiResponse.error("系统异常，请联系管理员");
    }
}
```

---

## 10. 性能规范

### 10.1 数据库查询

- ✅ 避免 `SELECT *`，只查询需要的字段
- ✅ 使用批量操作（`batchInsert`, `batchUpdate`）
- ✅ 分页查询必须有上限（单次最多 1000 条）
- ✅ 避免 N+1 查询（使用 JOIN 或批量查询）

### 10.2 缓存策略

- ✅ 热点数据缓存（商品详情、用户信息）
- ✅ 设置合理的过期时间（避免缓存雪崩）
- ✅ 使用缓存穿透保护（BloomFilter）
- ✅ 缓存更新采用 Cache Aside 模式

### 10.3 并发控制

- ✅ 使用分布式锁（Redis）控制并发
- ✅ 使用乐观锁（version 字段）更新数据
- ✅ 库存扣减使用 Lua 脚本保证原子性

---

## 11. 安全规范

### 11.1 认证与授权

- ✅ 所有 API 必须经过认证（除公开接口）
- ✅ 使用 JWT Token 进行身份验证
- ✅ Token 有效期：Access Token 2小时，Refresh Token 7天
- ✅ 使用 RBAC 模型进行权限控制

### 11.2 数据安全

- ✅ 敏感数据加密存储（密码使用 Argon2）
- ✅ 传输层使用 HTTPS（生产环境）
- ✅ SQL 注入防护（使用参数化查询）
- ✅ XSS 防护（输入验证 + 输出转义）

### 11.3 接口安全

- ✅ 接口限流（网关层 + 服务层）
- ✅ 请求签名验证（防止重放攻击）
- ✅ IP 白名单（敏感操作）
- ✅ 审计日志（记录所有敏感操作）

---

## 12. 认证服务设计 (scm-auth)

### 12.1 架构概述

**scm-auth** 是 SCM 平台的核心认证服务，提供企业级的身份认证和授权功能。服务采用多因素认证策略，支持传统密码、TOTP、WebAuthn（FIDO2）等多种认证方式，并提供 OAuth2 授权服务器能力。

**核心特性**:
- 多因素认证 (MFA): 密码 + TOTP + WebAuthn
- 无密码认证: FIDO2/WebAuthn 标准实现
- OAuth2 授权服务器: 支持标准 OAuth2 流程
- JWT 令牌管理: Access Token + Refresh Token
- mTLS 服务间认证: 双向 TLS 加密通信
- 账户安全保护: 登录失败锁定、设备追踪、IP 黑名单
- 高可用设计: 熔断降级、双 RPC 策略（Dubbo + Feign）

**服务信息**:
- 端口: `8106`
- 服务名: `auth-service`
- 数据库: PostgreSQL (WebAuthn credentials)
- 缓存: Redis (challenges, login attempts, token blacklist)

### 12.2 目录结构

```
scm-auth/
├── src/main/java/com/frog/auth/
│   ├── AuthApplication.java           # 启动类
│   ├── controller/                    # REST API 控制器
│   │   ├── SysAuthController.java     # 核心认证 (login/logout/refresh)
│   │   ├── OAuth2LogoutController.java # OAuth2 登出
│   │   └── WebAuthnCredentialController.java  # WebAuthn 管理
│   ├── service/                       # 业务逻辑层
│   │   ├── ISysAuthService.java
│   │   ├── IWebauthnCredentialService.java
│   │   ├── ICustomAuthorizationService.java
│   │   └── Impl/                      # 实现类
│   ├── domain/                        # 领域模型
│   │   ├── entity/WebauthnCredential.java  # WebAuthn 凭证实体
│   │   └── dto/                       # 数据传输对象
│   ├── mapper/                        # MyBatis 数据访问
│   │   └── WebauthnCredentialMapper.java
│   └── webauthn/                      # WebAuthn 实现
│       ├── WebAuthnConfig.java        # 配置
│       └── WebAuthnValidator.java     # W3C WebAuthn 验证器
└── src/main/resources/
    ├── application.yaml               # 配置文件
    └── mapper/WebAuthnCredentialMapper.xml
```

### 12.3 核心认证流程

#### 12.3.1 传统用户名密码登录

**API**: `POST /api/auth/login`

**请求示例**:
```json
{
  "username": "admin",
  "password": "your-password",
  "mfaCode": "123456",  // 可选：TOTP 验证码
  "deviceId": "device-fingerprint",
  "ipAddress": "192.168.1.100"
}
```

**执行流程**:
```java
// 1. 账户锁定检查 (Redis: "account:lock:{username}")
if (isAccountLocked(username)) {
    throw new BusinessException("账户已锁定，请15分钟后重试");
}

// 2. 登录失败次数检查 (Redis: "login:attempts:{username}")
int attempts = getLoginAttempts(username);
if (attempts >= 5) {
    lockAccount(username, 15 * 60); // 锁定15分钟
    throw new BusinessException("登录失败次数过多，账户已锁定");
}

// 3. Spring Security 认证
Authentication auth = authenticationManager.authenticate(
    new UsernamePasswordAuthenticationToken(username, password)
);

// 4. MFA 验证（如果启用）
if (user.isMfaEnabled() && mfaCode != null) {
    if (!totpValidator.validate(user.getTotpSecret(), mfaCode)) {
        throw new BusinessException("MFA 验证码错误");
    }
    // 防止重放攻击
    redisTemplate.opsForValue().set(
        "mfa:totp:used:" + userId + ":" + mfaCode,
        "1", 90, TimeUnit.SECONDS
    );
}

// 5. 密码过期检查
if (isPasswordExpired(user)) {
    throw new BusinessException("密码已过期，请修改密码");
}

// 6. 生成 JWT 令牌
String accessToken = jwtUtils.generateAccessToken(user, deviceId, ipAddress);
String refreshToken = jwtUtils.generateRefreshToken(user);

// 7. 清除失败记录
clearLoginAttempts(username);

// 8. 更新最后登录信息 (通过 Dubbo/Feign RPC)
try {
    userDubboService.updateLastLogin(userId, ipAddress, deviceId);
} catch (Exception ex) {
    userServiceClient.updateLastLogin(userId, ipAddress, deviceId);
}

// 9. 审计日志记录
auditLogService.record("USER_LOGIN", userId, ipAddress);

return LoginResponse.builder()
    .accessToken(accessToken)
    .refreshToken(refreshToken)
    .expiresIn(7200)  // 2小时
    .build();
```

**响应示例**:
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 7200,
    "tokenType": "Bearer"
  },
  "traceId": "550e8400-...",
  "timestamp": "2025-12-26T10:30:00Z"
}
```

**安全机制**:
- 账户锁定: 5次失败后锁定15分钟（可配置）
- MFA 重放保护: TOTP 验证码90秒内不可重复使用
- 设备追踪: JWT 包含 deviceId，异常设备告警
- IP 追踪: JWT 包含 ipAddress，异常 IP 告警
- 密码过期: 可配置密码有效期（如90天）

#### 12.3.2 WebAuthn 无密码认证

**注册流程**:

**1. 生成注册挑战**

`POST /api/auth/webauthn/register/challenge`

```java
@PostMapping("/register/challenge")
public ApiResponse<ChallengeResponse> generateChallenge() {
    UUID userId = SecurityContextHolder.getContext().getUserId();

    // 生成32字节加密随机挑战
    byte[] challenge = new byte[32];
    SecureRandom.getInstanceStrong().nextBytes(challenge);
    String challengeBase64 = Base64.getUrlEncoder()
        .withoutPadding().encodeToString(challenge);

    // 存储到 Redis (5分钟过期)
    String key = "webauthn:reg:challenge:" + userId + ":" + deviceId;
    redisTemplate.opsForValue().set(key, challengeBase64, 5, TimeUnit.MINUTES);

    return ApiResponse.success(ChallengeResponse.builder()
        .challenge(challengeBase64)
        .rpId("localhost")  // Relying Party ID
        .rpName("SCM Platform")
        .userId(userId)
        .userName(username)
        .timeout(300000)  // 5分钟
        .build());
}
```

**2. 验证并注册凭证**

`POST /api/auth/webauthn/register/verify`

```java
@PostMapping("/register/verify")
@Transactional
public ApiResponse<WebauthnCredentialDTO> registerCredential(
    @RequestBody WebauthnRegistrationRequest request) {

    // 1. 检查 credential ID 唯一性
    if (credentialMapper.existsByCredentialId(request.getCredentialId())) {
        throw new BusinessException("该凭证已被注册");
    }

    // 2. 从 Redis 获取并验证挑战
    String key = "webauthn:reg:challenge:" + userId + ":" + deviceId;
    String storedChallenge = redisTemplate.opsForValue().get(key);
    if (storedChallenge == null) {
        throw new BusinessException("挑战已过期，请重新注册");
    }

    // 3. 使用 WebAuthn4J 验证注册响应
    RegistrationResult result = webAuthnValidator.validateRegistration(
        request.getClientDataJSON(),
        request.getAttestationObject(),
        storedChallenge,
        "https://localhost",  // origin
        "localhost"           // rpId
    );

    // 4. 删除已使用的挑战（一次性使用）
    redisTemplate.delete(key);

    // 5. 构建凭证实体
    WebauthnCredential credential = WebauthnCredential.builder()
        .id(UUID.randomUUID())
        .userId(userId)
        .credentialId(result.getCredentialId())  // Base64URL
        .publicKeyPem(result.getPublicKeyPem())  // COSE 公钥
        .algorithm(result.getAlgorithm())        // ES256, RS256, EdDSA
        .signCount(0L)                           // 初始签名计数器
        .aaguid(result.getAaguid())              // 认证器模型ID
        .transports(result.getTransports())      // ["usb", "nfc", "ble", "internal"]
        .deviceName(request.getDeviceName())     // 用户自定义名称
        .isActive(true)
        .createdAt(LocalDateTime.now())
        .build();

    // 6. 保存到数据库
    credentialMapper.insert(credential);

    log.info("WebAuthn credential registered: userId={}, credId={}, device={}",
        userId, credential.getCredentialId(), credential.getDeviceName());

    return ApiResponse.success(WebauthnCredentialConverter.toDTO(credential));
}
```

**认证流程**:

**1. 生成认证挑战**

`POST /api/auth/webauthn/authenticate/challenge`

```java
@PostMapping("/authenticate/challenge")
public ApiResponse<AuthChallengeResponse> generateAuthChallenge(
    @RequestParam String username) {

    // 生成挑战
    byte[] challenge = new byte[32];
    SecureRandom.getInstanceStrong().nextBytes(challenge);
    String challengeBase64 = Base64.getUrlEncoder()
        .withoutPadding().encodeToString(challenge);

    // 存储到 Redis (2分钟过期)
    String key = "webauthn:auth:challenge:" + username;
    redisTemplate.opsForValue().set(key, challengeBase64, 2, TimeUnit.MINUTES);

    // 获取用户的所有激活凭证
    List<WebauthnCredential> credentials =
        credentialMapper.findActiveByUsername(username);

    return ApiResponse.success(AuthChallengeResponse.builder()
        .challenge(challengeBase64)
        .allowCredentials(credentials.stream()
            .map(c -> new CredentialDescriptor(c.getCredentialId(), c.getTransports()))
            .collect(Collectors.toList()))
        .timeout(120000)  // 2分钟
        .build());
}
```

**2. 验证并升级令牌**

`POST /api/auth/webauthn/authenticate/verify`

```java
@PostMapping("/authenticate/verify")
@Transactional
public ApiResponse<TokenUpgradeResponse> authenticateAndUpgrade(
    @RequestBody WebauthnAuthenticationRequest request) {

    // 1. 获取并验证挑战
    String key = "webauthn:auth:challenge:" + username;
    String storedChallenge = redisTemplate.opsForValue().get(key);
    if (storedChallenge == null) {
        throw new BusinessException("挑战已过期");
    }

    // 2. 从数据库获取凭证
    WebauthnCredential credential = credentialMapper
        .findByCredentialId(request.getCredentialId());
    if (credential == null || !credential.isAvailable()) {
        throw new BusinessException("凭证无效或已停用");
    }

    // 3. 签名计数器预检查（防克隆攻击）
    if (!credential.isCounterValid(request.getSignCount())) {
        log.error("Cloned authenticator detected! userId={}, credId={}, " +
            "expected={}, got={}", credential.getUserId(),
            credential.getCredentialId(), credential.getSignCount(),
            request.getSignCount());
        throw new BusinessException("检测到认证器克隆攻击，凭证已停用");
    }

    // 4. 使用 WebAuthn4J 验证断言
    AuthenticationResult result = webAuthnValidator.validateAuthentication(
        request.getClientDataJSON(),
        request.getAuthenticatorData(),
        request.getSignature(),
        storedChallenge,
        credential.getPublicKeyPem(),
        "https://localhost",
        "localhost"
    );

    // 5. 更新凭证使用信息
    credential.updateUsage(result.getSignCount());
    credentialMapper.updateSignCount(
        credential.getId(),
        result.getSignCount(),
        LocalDateTime.now()
    );

    // 6. 删除已使用的挑战
    redisTemplate.delete(key);

    // 7. 获取用户角色和权限（Feign RPC）
    UserInfoDTO userInfo = userServiceClient.getUserInfo(credential.getUserId()).data();
    List<String> roles = userInfo.getRoles();
    List<String> permissions = userInfo.getPermissions();

    // 8. 生成升级后的 JWT（包含多因素认证标记）
    String accessToken = jwtUtils.generateAccessToken(
        credential.getUserId(),
        username,
        roles,
        permissions,
        request.getDeviceId(),
        request.getIpAddress(),
        List.of("pwd", "webauthn")  // AMR: Authentication Methods Reference
    );

    log.info("WebAuthn authentication successful: userId={}, credId={}, AMR=[pwd,webauthn]",
        credential.getUserId(), credential.getCredentialId());

    return ApiResponse.success(TokenUpgradeResponse.builder()
        .accessToken(accessToken)
        .expiresIn(7200)
        .amr(List.of("pwd", "webauthn"))
        .build());
}
```

**安全亮点**:
- **挑战-响应机制**: 32字节加密随机挑战，一次性使用
- **签名计数器验证**: 严格递增检查，防止认证器克隆
- **公钥加密**: COSE 格式公钥（支持 ES256, RS256, EdDSA）
- **W3C 标准合规**: WebAuthn Level 2 标准实现（WebAuthn4J）
- **AMR 声明**: JWT 包含认证方法引用 `["pwd", "webauthn"]`，用于敏感操作的升级认证

#### 12.3.3 令牌刷新

**API**: `POST /api/auth/refresh`

```java
@PostMapping("/refresh")
@RateLimit(permits = 10, duration = 60)  // 限流: 60秒内最多10次
public ApiResponse<LoginResponse> refresh(@RequestParam String refreshToken) {

    // 1. 验证 Refresh Token
    if (!jwtUtils.validateToken(refreshToken)) {
        throw new BusinessException("Refresh Token 无效或已过期");
    }

    // 2. 从 Token 提取用户信息
    UUID userId = jwtUtils.getUserIdFromToken(refreshToken);
    String username = jwtUtils.getUsernameFromToken(refreshToken);

    // 3. 检查 Token 是否在黑名单中（已登出）
    if (jwtUtils.isTokenRevoked(refreshToken)) {
        throw new BusinessException("Token 已被撤销，请重新登录");
    }

    // 4. 重新获取用户权限（权限可能已变更）
    List<String> roles;
    List<String> permissions;
    try {
        // 优先使用 Dubbo（高性能）
        roles = userDubboService.findRoles(userId);
        permissions = userDubboService.findPermissions(userId);
    } catch (Exception ex) {
        log.warn("Dubbo RPC failed, fallback to Feign: {}", ex.getMessage());
        // Feign 降级
        UserInfoDTO userInfo = userServiceClient.getUserInfo(userId).data();
        roles = userInfo.getRoles();
        permissions = userInfo.getPermissions();
    }

    // 5. 生成新的 Access Token
    String newAccessToken = jwtUtils.generateAccessToken(
        userId, username, roles, permissions, null, null, null
    );

    log.info("Token refreshed: userId={}, username={}", userId, username);

    return ApiResponse.success(LoginResponse.builder()
        .accessToken(newAccessToken)
        .refreshToken(refreshToken)  // Refresh Token 保持不变
        .expiresIn(7200)
        .build());
}
```

**关键设计**:
- **限流保护**: 防止 Refresh Token 被暴力枚举
- **权限重新加载**: 确保最新权限生效
- **双 RPC 策略**: Dubbo 主，Feign 备（高可用）
- **黑名单机制**: 支持强制登出（全局撤销）

#### 12.3.4 登出

**API**: `POST /api/auth/logout`

```java
@PostMapping("/logout")
@PreAuthorize("isAuthenticated()")
public ApiResponse<Void> logout() {

    UUID userId = SecurityContextHolder.getContext().getUserId();
    String accessToken = SecurityContextHolder.getContext().getToken();

    // 1. 撤销当前 Token (添加到 Redis 黑名单)
    jwtUtils.revokeToken(accessToken);

    // 2. 审计日志
    auditLogService.record("USER_LOGOUT", userId, getClientIp());

    log.info("User logged out: userId={}", userId);

    return ApiResponse.success();
}
```

**强制全局登出** (管理员功能):

`POST /api/auth/force-logout/{userId}`

```java
@PostMapping("/force-logout/{userId}")
@PreAuthorize("hasAuthority('system:user:edit')")
@AuditLog(operation = "强制用户登出", businessType = "USER", riskLevel = 4)
public ApiResponse<Void> forceLogout(@PathVariable UUID userId) {

    // 撤销该用户的所有 Token
    jwtUtils.revokeAllUserTokens(userId);

    log.warn("Force logout executed: targetUserId={}, operatorId={}",
        userId, SecurityContextHolder.getContext().getUserId());

    return ApiResponse.success();
}
```

### 12.4 JWT 令牌结构

**Access Token Claims**:
```json
{
  "sub": "admin",                      // 用户名
  "userId": "550e8400-e29b-41d4-...",  // 用户ID (UUID)
  "roles": ["ROLE_ADMIN", "ROLE_USER"],
  "permissions": [
    "system:user:list",
    "system:user:add",
    "order:order:create"
  ],
  "deviceId": "fp_1234567890abcdef",   // 设备指纹
  "ipAddress": "192.168.1.100",        // 登录IP
  "amr": ["pwd", "webauthn"],          // 认证方法引用
  "iat": 1703145600,                   // 签发时间
  "exp": 1703152800                    // 过期时间 (2小时后)
}
```

**Refresh Token Claims**:
```json
{
  "sub": "admin",
  "userId": "550e8400-e29b-41d4-...",
  "type": "refresh",
  "iat": 1703145600,
  "exp": 1703750400  // 7天后过期
}
```

**令牌管理策略**:
- Access Token: 2小时过期，包含完整权限
- Refresh Token: 7天过期，仅用于刷新
- 撤销机制: Redis 黑名单（TTL = Token 剩余有效期）
- 全局登出: 通过用户ID撤销所有 Token

### 12.5 安全配置

#### 12.5.1 mTLS (双向TLS)

**application.yaml**:
```yaml
server:
  port: 8106
  ssl:
    enabled: true
    # 服务端证书
    key-store: classpath:ssl/auth-keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    # 客户端证书验证
    client-auth: need  # 强制要求客户端证书
    trust-store: classpath:ssl/truststore.p12
    trust-store-password: ${TRUSTSTORE_PASSWORD}
    trust-store-type: PKCS12
```

**使用场景**:
- Gateway → Auth Service 通信
- 内部服务间敏感调用
- 第三方集成（需要客户端证书）

#### 12.5.2 熔断与降级

**Resilience4j 配置**:
```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        failure-rate-threshold: 50        # 失败率阈值
        slow-call-rate-threshold: 50      # 慢调用阈值
        slow-call-duration-threshold: 2s  # 慢调用定义
        wait-duration-in-open-state: 60s  # 熔断器打开后等待时间
        permitted-number-of-calls-in-half-open-state: 3
        sliding-window-type: COUNT_BASED
        sliding-window-size: 10
    instances:
      authService:
        base-config: default
        failure-rate-threshold: 60  # 认证服务容忍度更高
```

**双RPC降级示例**:
```java
// Dubbo 主，Feign 备
try {
    return userDubboService.getUserInfo(userId);
} catch (Exception dubboEx) {
    log.warn("Dubbo RPC failed: {}, fallback to Feign", dubboEx.getMessage());
    try {
        return userServiceClient.getUserInfo(userId).data();
    } catch (Exception feignEx) {
        log.error("Both Dubbo and Feign failed", feignEx);
        throw new SystemException("用户服务不可用");
    }
}
```

#### 12.5.3 限流配置

**Sentinel 流控规则** (通过 Nacos 配置):
```json
[
  {
    "resource": "/api/auth/login",
    "grade": 1,
    "count": 100,
    "limitApp": "default",
    "strategy": 0,
    "controlBehavior": 0
  },
  {
    "resource": "/api/auth/refresh",
    "grade": 1,
    "count": 50,
    "limitApp": "default",
    "strategy": 0,
    "controlBehavior": 0
  }
]
```

**注解式限流**:
```java
@SentinelResource(
    value = "/api/auth/login",
    blockHandler = "loginBlockHandler",
    fallback = "loginFallback"
)
public ApiResponse<LoginResponse> login(...) {
    // 业务逻辑
}

public ApiResponse<LoginResponse> loginBlockHandler(..., BlockException ex) {
    log.warn("Login request blocked by Sentinel: {}", ex.getMessage());
    return ApiResponse.error(429, "请求过于频繁，请稍后重试");
}
```

### 12.6 服务集成指南

#### 12.6.1 客户端集成 (前端/移动端)

**传统登录**:
```javascript
// 1. 登录
const response = await fetch('https://localhost:8106/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    username: 'admin',
    password: 'password123',
    mfaCode: '123456',  // 可选
    deviceId: getDeviceFingerprint(),
    ipAddress: getClientIp()
  })
});

const { data } = await response.json();
// 存储 Token
localStorage.setItem('access_token', data.accessToken);
localStorage.setItem('refresh_token', data.refreshToken);

// 2. 后续请求携带 Token
fetch('https://localhost:8201/api/v1/products', {
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('access_token')}`
  }
});

// 3. Token 过期时刷新
if (response.status === 401) {
  const refreshResponse = await fetch('https://localhost:8106/api/auth/refresh', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: `refreshToken=${localStorage.getItem('refresh_token')}`
  });

  if (refreshResponse.ok) {
    const { data } = await refreshResponse.json();
    localStorage.setItem('access_token', data.accessToken);
    // 重试原请求
  } else {
    // Refresh Token 也过期，跳转登录页
    window.location.href = '/login';
  }
}
```

**WebAuthn 注册**:
```javascript
// 1. 获取注册挑战
const challengeResponse = await fetch(
  'https://localhost:8106/api/auth/webauthn/register/challenge',
  { headers: { 'Authorization': `Bearer ${accessToken}` } }
);
const { challenge, rpId, userId, userName } = await challengeResponse.json();

// 2. 调用浏览器 WebAuthn API
const credential = await navigator.credentials.create({
  publicKey: {
    challenge: Uint8Array.from(atob(challenge), c => c.charCodeAt(0)),
    rp: { id: rpId, name: "SCM Platform" },
    user: {
      id: Uint8Array.from(userId, c => c.charCodeAt(0)),
      name: userName,
      displayName: userName
    },
    pubKeyCredParams: [
      { type: "public-key", alg: -7 },  // ES256
      { type: "public-key", alg: -257 } // RS256
    ],
    authenticatorSelection: {
      authenticatorAttachment: "platform",  // 平台认证器（TouchID/FaceID/Windows Hello）
      userVerification: "required"
    },
    timeout: 300000,
    attestation: "none"
  }
});

// 3. 提交凭证注册
await fetch('https://localhost:8106/api/auth/webauthn/register/verify', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${accessToken}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    credentialId: arrayBufferToBase64(credential.rawId),
    clientDataJSON: arrayBufferToBase64(credential.response.clientDataJSON),
    attestationObject: arrayBufferToBase64(credential.response.attestationObject),
    deviceName: "MacBook Pro TouchID"
  })
});
```

**WebAuthn 认证**:
```javascript
// 1. 获取认证挑战
const challengeResponse = await fetch(
  `https://localhost:8106/api/auth/webauthn/authenticate/challenge?username=admin`
);
const { challenge, allowCredentials } = await challengeResponse.json();

// 2. 调用浏览器 WebAuthn API
const assertion = await navigator.credentials.get({
  publicKey: {
    challenge: Uint8Array.from(atob(challenge), c => c.charCodeAt(0)),
    allowCredentials: allowCredentials.map(c => ({
      type: "public-key",
      id: Uint8Array.from(atob(c.credentialId), ch => ch.charCodeAt(0)),
      transports: c.transports
    })),
    timeout: 120000,
    userVerification: "required"
  }
});

// 3. 提交认证并升级 Token
const authResponse = await fetch(
  'https://localhost:8106/api/auth/webauthn/authenticate/verify',
  {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      username: 'admin',
      credentialId: arrayBufferToBase64(assertion.rawId),
      clientDataJSON: arrayBufferToBase64(assertion.response.clientDataJSON),
      authenticatorData: arrayBufferToBase64(assertion.response.authenticatorData),
      signature: arrayBufferToBase64(assertion.response.signature),
      signCount: assertion.response.signature.signCount,
      deviceId: getDeviceFingerprint(),
      ipAddress: getClientIp()
    })
  }
);

const { data } = await authResponse.json();
localStorage.setItem('access_token', data.accessToken);  // 包含 AMR: ["pwd", "webauthn"]
```

#### 12.6.2 服务间调用

**方式1: Feign Client**

```java
@FeignClient(
    name = "auth-service",
    fallback = AuthServiceFallback.class
)
public interface AuthServiceClient {

    @PostMapping("/api/auth/internal/validate-token")
    ApiResponse<TokenValidationResult> validateToken(@RequestParam String token);

    @GetMapping("/api/auth/internal/user-permissions/{userId}")
    ApiResponse<List<String>> getUserPermissions(@PathVariable UUID userId);
}

// 使用示例
@Service
@RequiredArgsConstructor
public class OrderService {

    private final AuthServiceClient authClient;

    public void createOrder(CreateOrderRequest request) {
        // 验证 Token
        TokenValidationResult validation =
            authClient.validateToken(request.getAccessToken()).data();

        if (!validation.isValid()) {
            throw new UnauthorizedException("Token 无效");
        }

        // 检查权限
        List<String> permissions =
            authClient.getUserPermissions(validation.getUserId()).data();

        if (!permissions.contains("order:order:create")) {
            throw new ForbiddenException("无权限创建订单");
        }

        // 业务逻辑...
    }
}
```

**方式2: Gateway 统一认证** (推荐)

```yaml
# Gateway 配置
spring:
  cloud:
    gateway:
      routes:
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/v1/orders/**
          filters:
            - JwtAuthenticationFilter  # 自动验证 JWT
            - name: AddRequestHeader
              args:
                name: X-User-Id
                value: "#{@jwtUtils.getUserId(#token)}"
            - name: AddRequestHeader
              args:
                name: X-Username
                value: "#{@jwtUtils.getUsername(#token)}"
            - name: AddRequestHeader
              args:
                name: X-Roles
                value: "#{@jwtUtils.getRoles(#token)}"
            - name: AddRequestHeader
              args:
                name: X-Permissions
                value: "#{@jwtUtils.getPermissions(#token)}"
```

**下游服务从 Header 获取用户信息**:
```java
@Component
public class SecurityContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, ...) {
        String userId = request.getHeader("X-User-Id");
        String username = request.getHeader("X-Username");
        String rolesJson = request.getHeader("X-Roles");
        String permissionsJson = request.getHeader("X-Permissions");

        // 填充 SecurityContext
        UserContext userContext = UserContext.builder()
            .userId(UUID.fromString(userId))
            .username(username)
            .roles(parseRoles(rolesJson))
            .permissions(parsePermissions(permissionsJson))
            .build();

        SecurityContextHolder.getContext().setUserContext(userContext);

        return true;
    }
}
```

### 12.7 数据库设计

**webauthn_credential 表**:
```sql
CREATE TABLE webauthn_credential (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    credential_id VARCHAR(512) NOT NULL,  -- Base64URL 编码
    public_key_pem TEXT NOT NULL,         -- COSE 公钥 (敏感字段)
    alg VARCHAR(20) NOT NULL,             -- ES256, RS256, EdDSA
    sign_count BIGINT DEFAULT 0,          -- 签名计数器（防克隆）
    device_name VARCHAR(255),             -- 用户自定义设备名
    aaguid UUID,                          -- 认证器模型标识
    transports TEXT[],                    -- 通信方式: usb, nfc, ble, internal, hybrid
    is_active BOOLEAN DEFAULT TRUE,       -- 激活状态
    last_used_at TIMESTAMPTZ,             -- 最后使用时间
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    tenant_id VARCHAR(32),

    CONSTRAINT uk_credential_id UNIQUE (credential_id),
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
);

CREATE INDEX idx_webauthn_user_id ON webauthn_credential(user_id);
CREATE INDEX idx_webauthn_active ON webauthn_credential(is_active) WHERE is_active = TRUE;
CREATE INDEX idx_webauthn_last_used ON webauthn_credential(last_used_at) WHERE is_active = TRUE;
```

### 12.8 监控与审计

#### 12.8.1 业务指标

**Prometheus Metrics**:
```java
// 登录成功/失败计数
@Timed(value = "auth.login", description = "Login duration")
@Counter(value = "auth.login.success", description = "Successful logins")
@Counter(value = "auth.login.failed", description = "Failed logins")

// WebAuthn 使用率
@Gauge(value = "auth.webauthn.credentials.total", description = "Total WebAuthn credentials")
@Counter(value = "auth.webauthn.auth.success", description = "WebAuthn authentications")

// 账户锁定事件
@Counter(value = "auth.account.locked", description = "Account lockout events")
```

**Grafana 仪表盘**:
- 登录成功率 (success / (success + failed))
- 平均登录耗时
- WebAuthn 使用率
- MFA 启用率
- 账户锁定趋势

#### 12.8.2 审计日志

**自动记录事件**:
- `USER_LOGIN` - 用户登录（包含 IP、设备信息）
- `USER_LOGOUT` - 用户登出
- `USER_FORCE_LOGOUT` - 管理员强制登出
- `WEBAUTHN_REGISTER` - WebAuthn 凭证注册
- `WEBAUTHN_AUTH` - WebAuthn 认证成功
- `WEBAUTHN_CLONE_DETECTED` - 检测到认证器克隆
- `ACCOUNT_LOCKED` - 账户锁定
- `PASSWORD_EXPIRED` - 密码过期
- `MFA_FAILED` - MFA 验证失败

**日志格式**:
```json
{
  "event": "USER_LOGIN",
  "userId": "550e8400-e29b-41d4-...",
  "username": "admin",
  "ipAddress": "192.168.1.100",
  "deviceId": "fp_1234567890abcdef",
  "userAgent": "Mozilla/5.0...",
  "result": "SUCCESS",
  "mfaUsed": true,
  "timestamp": "2025-12-26T10:30:00Z",
  "traceId": "550e8400-..."
}
```

### 12.9 最佳实践

#### 12.9.1 密码策略

```java
// 密码复杂度要求（PasswordValidator）
- 最小长度: 8位
- 必须包含: 大写字母 + 小写字母 + 数字 + 特殊字符
- 不能包含: 用户名、常见密码（字典检查）
- 密码历史: 不能与最近5次密码相同
- 密码有效期: 90天

// 密码哈希（PasswordUtils）
使用 Argon2id（OWASP 推荐）:
- Memory: 65536 KB
- Iterations: 3
- Parallelism: 4
- Salt: 16 bytes (自动生成)
```

#### 12.9.2 MFA 最佳实践

```java
// TOTP 配置
- 算法: HMAC-SHA1
- 时间步长: 30秒
- 容忍窗口: ±1 步长 (允许时钟偏移)
- 密钥长度: 160 bits (32 Base32 字符)

// 备用恢复码
- 生成 10 个恢复码（16位随机字符串）
- 每个恢复码只能使用一次
- 使用 Argon2 哈希存储
- 下载后明文删除，仅保留哈希
```

#### 12.9.3 WebAuthn 最佳实践

```java
// 推荐配置
authenticatorSelection: {
    authenticatorAttachment: "platform",  // 优先平台认证器（更安全）
    residentKey: "preferred",             // 支持无用户名登录
    userVerification: "required"          // 强制用户验证（PIN/生物识别）
}

// 凭证管理
- 允许用户注册多个凭证（主设备 + 备用设备）
- 自动检测90天未使用凭证并提醒
- 提供凭证健康检查接口
- 签名计数器异常时立即停用并告警

// 降级策略
- WebAuthn 不可用时回退到 密码 + TOTP
- 提供恢复码作为最后手段
```

### 12.10 常见问题

**Q1: 如何实现"记住我"功能？**
```java
// 使用长期 Refresh Token (30天)
@PostMapping("/login")
public ApiResponse<LoginResponse> login(
    @RequestParam(required = false, defaultValue = "false") boolean rememberMe) {

    // ...认证逻辑

    int refreshTokenExpiry = rememberMe ? 30 * 24 * 3600 : 7 * 24 * 3600;
    String refreshToken = jwtUtils.generateRefreshToken(user, refreshTokenExpiry);

    return ApiResponse.success(LoginResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .expiresIn(7200)
        .build());
}
```

**Q2: 如何实现单点登录 (SSO)？**
```
使用 OAuth2 Authorization Code Flow:
1. 用户访问应用A → 重定向到 scm-auth OAuth2 授权端点
2. 用户在 scm-auth 登录（如已登录则跳过）
3. scm-auth 返回授权码
4. 应用A 使用授权码换取 Access Token
5. 用户访问应用B → 检测到 scm-auth 已登录 → 直接授权

参考: OAuth2AuthorizationServerConfig
```

**Q3: 如何防止账户枚举攻击？**
```java
// 错误消息统一化
if (user == null || !passwordMatches) {
    // ❌ 不要暴露用户是否存在
    // throw new BusinessException("用户不存在");

    // ✅ 使用模糊消息
    throw new BusinessException("用户名或密码错误");
}

// 登录失败时增加延迟（防暴力破解）
if (!authenticated) {
    Thread.sleep(new Random().nextInt(1000) + 500);  // 500-1500ms 随机延迟
    throw new BusinessException("用户名或密码错误");
}
```

**Q4: 如何实现跨域 SSO？**
```yaml
# Gateway CORS 配置
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowed-origins:
              - "https://app1.example.com"
              - "https://app2.example.com"
            allowed-methods: ["GET", "POST", "PUT", "DELETE"]
            allowed-headers: ["*"]
            allow-credentials: true  # 允许携带 Cookie
            max-age: 3600

# Cookie 配置（用于存储 Refresh Token）
server:
  servlet:
    session:
      cookie:
        domain: ".example.com"  # 父域名
        same-site: lax
        secure: true
        http-only: true
```

### 12.11 性能优化

**缓存策略**:
```java
// 用户权限缓存（Caffeine + Redis 二级缓存）
@Cacheable(value = "user:permissions", key = "#userId", unless = "#result == null")
public List<String> getUserPermissions(UUID userId) {
    // L1: Caffeine (本地缓存，10秒TTL)
    // L2: Redis (分布式缓存，5分钟TTL)
    return userService.findPermissions(userId);
}

// JWT 验证缓存（本地缓存，减少签名验证开销）
@Cacheable(value = "jwt:validation", key = "#token.substring(0, 50)")
public boolean validateToken(String token) {
    return jwtUtils.validateSignature(token);
}

// WebAuthn 公钥缓存（凭证不常变更）
@Cacheable(value = "webauthn:publickey", key = "#credentialId")
public String getPublicKey(String credentialId) {
    return credentialMapper.selectPublicKey(credentialId);
}
```

**异步处理**:
```java
// 审计日志异步写入
@Async("auditExecutor")
public void recordAuditLog(AuditLog log) {
    auditLogService.save(log);
}

// 用户最后登录时间异步更新
@Async("userUpdateExecutor")
public void updateLastLogin(UUID userId, String ip, String device) {
    userMapper.updateLastLogin(userId, ip, device, LocalDateTime.now());
}
```

## 13. 快速参考

### 13.1 Controller层检查清单

- [ ] 包名: `com.frog.{module}.controller`
- [ ] 使用 `@RequiredArgsConstructor`
- [ ] URL格式: `/api/v1/{module}/{resource}`
- [ ] 返回类型: `ApiResponse<T>`
- [ ] 添加 `@PreAuthorize` 权限控制
- [ ] 添加 `@AuditLog` (敏感操作)
- [ ] 添加 `@Operation` Swagger注解
- [ ] 参数校验: `@Validated` / `@Valid`

### 12.2 Service层检查清单

- [ ] 包名: `com.frog.{module}.service.impl`
- [ ] 继承: `ServiceImpl<Mapper, Entity>`
- [ ] 使用 `@RequiredArgsConstructor`
- [ ] 添加 `@DS("{datasource}")` 数据源路由
- [ ] 添加 `@Transactional` 事务管理
- [ ] 添加 `@Slf4j` 日志
- [ ] 实现完整的业务逻辑和校验

### 12.3 Entity层检查清单

- [ ] 包名: `com.frog.{module}.domain.entity`
- [ ] 主键: `UUID` + `@TableId(type = IdType.ASSIGN_UUID)`
- [ ] 必需字段: `tenant_id`, 审计字段, `deleted`
- [ ] 使用 `@TableField` 自动填充
- [ ] 使用 `@TableLogic` 逻辑删除

### 12.4 配置文件检查清单

- [ ] application.yml 配置完整（数据源、Redis、Nacos等）
- [ ] 使用环境变量（${DB_HOST:localhost}）
- [ ] MyBatis-Plus 配置正确（UUID主键、逻辑删除）
- [ ] 日志级别配置合理（生产INFO，开发DEBUG）

---

## 附录

### A. IDE 配置

**推荐插件**:
- Lombok
- CheckStyle
- SonarLint
- GitToolBox
- Rainbow Brackets

**代码格式化**:
导入 `config/google-java-format.xml` 到 IDE。

### B. 开发工具

- IDE: IntelliJ IDEA 2024+
- JDK: OpenJDK 21 (Temurin)
- Maven: 3.8+
- Docker: 24+
- Git: 2.40+

### C. 参考资料

- [阿里巴巴 Java 开发手册](https://github.com/alibaba/p3c)
- [Spring Boot Best Practices](https://spring.io/guides/gs/rest-service/)
- [RESTful API Design Guidelines](https://restfulapi.net/)
- [Conventional Commits](https://www.conventionalcommits.org/)

---

### D. 参考文档

本文档提供开发规范的概览和常用标准。更多详细内容请参考：

**架构与设计**:
- [微服务架构文档](../design/ARCHITECTURE.md) - 完整的微服务架构、数据库设计思想
- [SCM设计方案](../design/SCM_DESIGN_PLAN.md) - 平台整体业务设计
- [架构决策记录](../design/ADR.md) - 重要架构决策（ADR）

**技术详细规范**:
- [API设计规范](../technical/API_DESIGN.md) - RESTful API详细设计标准
- [数据库设计](../technical/DATABASE_DESIGN.md) - 表设计、索引、分区、性能优化详解

**多租户架构**:
- [多租户完整指南](../multi-tenant/MULTI_TENANT_GUIDE.md) - 多租户SaaS架构的完整实施指南

**集成指南**:
- [Seata集成指南](../guides/SEATA_INTEGRATION_GUIDE.md) - 分布式事务集成
- [XXL-Job集成指南](../guides/XXL_JOB_INTEGRATION_GUIDE.md) - 分布式任务调度
- [Elasticsearch集成指南](../guides/ELASTICSEARCH_INTEGRATION_GUIDE.md) - 商品搜索集成

---

**文档维护**: 所有团队成员有责任更新和完善本文档。如有疑问，请联系架构师。
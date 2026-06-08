# WmsInboundController 重构实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 WmsInboundController 中的业务逻辑下沉到 CQRS Service 层，消除代码重复，引入状态枚举，使 Controller 仅负责 HTTP 路由和参数传递。

**Architecture:** Controller 注入 Command/Query 服务替代 IService，所有业务逻辑（ID 生成、默认值设置、状态校验、软删除）移至 CommandService，查询逻辑集中在 QueryService。参照 OrderStatus 枚举模式创建 InboundStatus 枚举。

**Tech Stack:** Spring Boot, MyBatis-Plus, Baomidou dynamic-datasource (`@Master`/`@Slave`), Jakarta Validation

---

## 文件清单

| 操作 | 文件路径 | 职责 |
|------|---------|------|
| Create | `scm-warehouse/service/src/main/java/com/scmcloud/warehouse/domain/entity/InboundStatus.java` | 入库单状态枚举 |
| Modify | `scm-warehouse/service/src/main/java/com/scmcloud/warehouse/service/command/WmsInboundCommandService.java` | 唯一写操作入口，包含所有业务逻辑 |
| Modify | `scm-warehouse/service/src/main/java/com/scmcloud/warehouse/service/query/WmsInboundQueryService.java` | 唯一读操作入口 |
| Modify | `scm-warehouse/service/src/main/java/com/scmcloud/warehouse/controller/WmsInboundController.java` | 瘦 Controller，仅路由 |
| Modify | `scm-warehouse/service/src/main/java/com/scmcloud/warehouse/service/impl/WmsInboundServiceImpl.java` | 移除与 CQRS 重复的方法 |
| Modify | `scm-warehouse/service/src/main/java/com/scmcloud/warehouse/service/IWmsInboundService.java` | 移除与 CQRS 重复的方法声明 |

---

### Task 1: 创建 InboundStatus 枚举

**Files:**
- Create: `scm-warehouse/service/src/main/java/com/scmcloud/warehouse/domain/entity/InboundStatus.java`

参照 `OrderStatus` 模式，定义入库单状态枚举，包含 `code`、`description`、`fromCode()`、`canTransitionTo()`、`isTerminal()` 方法。

- [ ] **Step 1: 创建 InboundStatus 枚举**

```java
package com.scmcloud.warehouse.domain.entity;

import java.util.Set;

public enum InboundStatus {

    WAITING(0, "待入库"),
    PROCESSING(1, "入库中"),
    PARTIAL(2, "部分入库"),
    COMPLETED(3, "已完成"),
    CANCELLED(4, "已取消");

    private final int code;
    private final String description;

    InboundStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static InboundStatus fromCode(int code) {
        for (InboundStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown inbound status code: " + code);
    }

    public Set<InboundStatus> validNextStatuses() {
        return switch (this) {
            case WAITING -> Set.of(PROCESSING, CANCELLED);
            case PROCESSING -> Set.of(PARTIAL, COMPLETED, CANCELLED);
            case PARTIAL -> Set.of(COMPLETED, CANCELLED);
            case COMPLETED -> Set.of();
            case CANCELLED -> Set.of();
        };
    }

    public boolean canTransitionTo(InboundStatus target) {
        return validNextStatuses().contains(target);
    }

    public boolean isTerminal() {
        return validNextStatuses().isEmpty();
    }

    public boolean isCancellable() {
        return this == WAITING || this == PROCESSING || this == PARTIAL;
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -pl scm-warehouse/service -am -f com.scm.parent/pom.xml -q`
Expected: BUILD SUCCESS

---

### Task 2: 重构 WmsInboundCommandService — 唯一写操作入口

**Files:**
- Modify: `scm-warehouse/service/src/main/java/com/scmcloud/warehouse/service/command/WmsInboundCommandService.java`

将 Controller 和 ServiceImpl 中的业务逻辑全部移入此服务。CommandService 是所有写操作的唯一入口，使用 `@Master` 注解确保走主库。

**Bug 修复:** 原 `WmsInboundServiceImpl.receive()` 中 `statusValidator.validateTransition("INBOUND", receiveFromStatus, allReceived ? "CANCELLED" : "FINISHED")` 存在逻辑错误——全部收货时应转为 `COMPLETED` 而非 `CANCELLED`，部分收货时应转为 `PARTIAL` 而非 `FINISHED`。重构时一并修正。

- [ ] **Step 1: 重写 WmsInboundCommandService**

将 `WmsInboundCommandService` 重构为包含 `create`、`update`、`softDelete`、`receive`、`cancel` 五个核心写操作。所有方法使用 `@Master` + `@Transactional`。`receive` 和 `cancel` 方法使用 `InboundStatus` 枚举替代魔法数字，同时保留 `StatusValidator` 校验。

```java
package com.scmcloud.warehouse.service.command;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.common.status.StatusValidator;
import com.scmcloud.common.util.UUIDv7Util;
import com.scmcloud.warehouse.domain.entity.InboundStatus;
import com.scmcloud.warehouse.domain.entity.WmsInbound;
import com.scmcloud.warehouse.domain.entity.WmsInboundItem;
import com.scmcloud.warehouse.mapper.WmsInboundItemMapper;
import com.scmcloud.warehouse.mapper.WmsInboundMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WmsInboundCommandService {

    private final WmsInboundMapper inboundMapper;
    private final WmsInboundItemMapper inboundItemMapper;
    private final StatusValidator statusValidator;

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public WmsInbound create(WmsInbound inbound) {
        inbound.setId(UUIDv7Util.generateString());
        inbound.setInboundNo("IN" + System.currentTimeMillis());
        inbound.setStatus(InboundStatus.WAITING.getCode());
        inbound.setReceivedQuantity(0);
        inbound.setDeleted(false);
        inbound.setCreateTime(LocalDateTime.now());
        inbound.setUpdateTime(LocalDateTime.now());
        inboundMapper.insert(inbound);
        log.info("入库单创建成功 id={}, inboundNo={}", inbound.getId(), inbound.getInboundNo());
        return inbound;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean update(WmsInbound inbound) {
        WmsInbound existing = inboundMapper.selectById(inbound.getId());
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            return false;
        }
        if (!InboundStatus.WAITING.getCode().equals(existing.getStatus())) {
            throw new IllegalStateException("只有待入库状态的入库单才能修改");
        }
        inbound.setUpdateTime(LocalDateTime.now());
        return inboundMapper.updateById(inbound) > 0;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean softDeleteById(String id) {
        WmsInbound inbound = inboundMapper.selectById(id);
        if (inbound == null || Boolean.TRUE.equals(inbound.getDeleted())) {
            return false;
        }
        inbound.setDeleted(true);
        inbound.setUpdateTime(LocalDateTime.now());
        return inboundMapper.updateById(inbound) > 0;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean receive(String inboundId, String operatorId, String operatorName) {
        WmsInbound inbound = inboundMapper.selectById(inboundId);
        if (inbound == null) {
            log.warn("入库单不存在: id={}", inboundId);
            return false;
        }

        List<WmsInboundItem> items = inboundItemMapper.selectList(
                Wrappers.<WmsInboundItem>lambdaQuery()
                        .eq(WmsInboundItem::getInboundId, inboundId)
                        .eq(WmsInboundItem::getDeleted, false));

        int totalReceived = items.stream()
                .mapToInt(item -> item.getActualQuantity() != null ? item.getActualQuantity() : 0)
                .sum();

        if (totalReceived == 0) {
            throw new IllegalStateException("入库明细实际收货数量不能全部为0");
        }

        boolean allReceived = items.stream()
                .allMatch(item -> item.getActualQuantity() != null
                        && item.getActualQuantity().equals(item.getPlanQuantity()));

        InboundStatus currentStatus = InboundStatus.fromCode(inbound.getStatus());
        statusValidator.validateTransition("INBOUND",
                currentStatus.name(), allReceived ? "COMPLETED" : "PARTIAL");

        inbound.setReceivedQuantity(totalReceived);
        inbound.setStatus(allReceived ? InboundStatus.COMPLETED.getCode() : InboundStatus.PARTIAL.getCode());
        inbound.setOperatorId(operatorId);
        inbound.setOperatorName(operatorName);
        inbound.setUpdateTime(LocalDateTime.now());
        inbound.setUpdateBy(operatorId);

        if (allReceived) {
            inbound.setCompletedAt(LocalDateTime.now());
        }

        boolean success = inboundMapper.updateById(inbound) > 0;
        if (success) {
            log.info("入库单收货完成: id={}, inboundNo={}, status={}, receivedQty={}",
                    inboundId, inbound.getInboundNo(), inbound.getStatus(), totalReceived);
        }
        return success;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(String inboundId, String operatorId, String operatorName) {
        WmsInbound inbound = inboundMapper.selectById(inboundId);
        if (inbound == null) {
            log.warn("入库单不存在: id={}", inboundId);
            return false;
        }

        InboundStatus currentStatus = InboundStatus.fromCode(inbound.getStatus());
        statusValidator.validateTransition("INBOUND", currentStatus.name(), "CANCELLED");

        inbound.setStatus(InboundStatus.CANCELLED.getCode());
        inbound.setOperatorId(operatorId);
        inbound.setOperatorName(operatorName);
        inbound.setUpdateTime(LocalDateTime.now());
        inbound.setUpdateBy(operatorId);

        boolean success = inboundMapper.updateById(inbound) > 0;
        if (success) {
            log.info("入库单已取消: id={}, inboundNo={}", inboundId, inbound.getInboundNo());
        }
        return success;
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -pl scm-warehouse/service -am -f com.scm.parent/pom.xml -q`
Expected: BUILD SUCCESS（注意：此步骤会因后续 Task 未完成而可能编译失败，先记录问题，继续后续 Task）

---

### Task 3: 重构 WmsInboundQueryService — 唯一读操作入口

**Files:**
- Modify: `scm-warehouse/service/src/main/java/com/scmcloud/warehouse/service/query/WmsInboundQueryService.java`

确保 QueryService 包含所有读操作：`getById`、`pageList`。当前已包含这两个方法，无需新增，仅确认结构完整。

- [ ] **Step 1: 确认 WmsInboundQueryService 内容**

当前 `WmsInboundQueryService` 已包含 `getById(String id)` 和 `pageList(int page, int size, String warehouseId, Integer inboundType, Integer status)`，均使用 `@Slave` 注解。结构完整，无需修改。

- [ ] **Step 2: 编译验证**

Run: `mvn compile -pl scm-warehouse/service -am -f com.scm.parent/pom.xml -q`
Expected: 无变化，跳过

---

### Task 4: 重构 WmsInboundController — 瘦 Controller

**Files:**
- Modify: `scm-warehouse/service/src/main/java/com/scmcloud/warehouse/controller/WmsInboundController.java`

Controller 注入 `WmsInboundCommandService` 和 `WmsInboundQueryService` 替代 `IWmsInboundService`。所有业务逻辑委托给 CQRS 服务，Controller 仅做参数传递和异常处理。

- [ ] **Step 1: 重写 WmsInboundController**

```java
package com.scmcloud.warehouse.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.warehouse.domain.entity.WmsInbound;
import com.scmcloud.warehouse.service.command.WmsInboundCommandService;
import com.scmcloud.warehouse.service.query.WmsInboundQueryService;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/wms-inbound")
public class WmsInboundController {

    private final WmsInboundCommandService inboundCommandService;
    private final WmsInboundQueryService inboundQueryService;

    @PostMapping
    public ApiResponse<WmsInbound> create(@RequestBody WmsInbound inbound) {
        log.info("[API] 创建入库单 warehouseId={}, type={}", inbound.getWarehouseId(), inbound.getInboundType());
        WmsInbound created = inboundCommandService.create(inbound);
        log.info("[API] 入库单创建成功 id={}, inboundNo={}", created.getId(), created.getInboundNo());
        return ApiResponse.success(created);
    }

    @PutMapping("/{id}")
    public ApiResponse<WmsInbound> update(@PathVariable String id, @RequestBody WmsInbound inbound) {
        log.info("[API] 更新入库单 id={}", id);
        inbound.setId(id);
        try {
            boolean success = inboundCommandService.update(inbound);
            if (!success) {
                return ApiResponse.fail(404, "入库单不存在");
            }
            return ApiResponse.success(inboundQueryService.getById(id));
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("[API] 删除入库单 id={}", id);
        boolean success = inboundCommandService.softDeleteById(id);
        return success ? ApiResponse.success() : ApiResponse.fail(404, "入库单不存在");
    }

    @GetMapping("/{id}")
    public ApiResponse<WmsInbound> getById(@PathVariable String id) {
        WmsInbound inbound = inboundQueryService.getById(id);
        if (inbound == null || Boolean.TRUE.equals(inbound.getDeleted())) {
            return ApiResponse.fail(404, "入库单不存在");
        }
        return ApiResponse.success(inbound);
    }

    @GetMapping("/page")
    public ApiResponse<Page<WmsInbound>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) Integer inboundType,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.success(inboundQueryService.pageList(page, size, warehouseId, inboundType, status));
    }

    @PutMapping("/{id}/receive")
    public ApiResponse<Void> receive(
            @PathVariable String id,
            @RequestParam String operatorId,
            @RequestParam String operatorName) {
        log.info("[API] 入库收货: id={}, operator={}", id, operatorName);
        try {
            boolean success = inboundCommandService.receive(id, operatorId, operatorName);
            return success ? ApiResponse.success() : ApiResponse.fail(400, "收货失败");
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(
            @PathVariable String id,
            @RequestParam String operatorId,
            @RequestParam String operatorName) {
        log.info("[API] 取消入库单 id={}, operator={}", id, operatorName);
        try {
            boolean success = inboundCommandService.cancel(id, operatorId, operatorName);
            return success ? ApiResponse.success() : ApiResponse.fail(400, "取消失败");
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -pl scm-warehouse/service -am -f com.scm.parent/pom.xml -q`
Expected: BUILD SUCCESS

---

### Task 5: 清理 IWmsInboundService 和 WmsInboundServiceImpl

**Files:**
- Modify: `scm-warehouse/service/src/main/java/com/scmcloud/warehouse/service/IWmsInboundService.java`
- Modify: `scm-warehouse/service/src/main/java/com/scmcloud/warehouse/service/impl/WmsInboundServiceImpl.java`

Controller 已不再使用 `IWmsInboundService`，但该接口可能被其他模块通过 Dubbo 或内部调用引用。需要检查引用情况后决定保留或删除重复方法。

- [ ] **Step 1: 检查 IWmsInboundService 的外部引用**

搜索项目中所有引用 `IWmsInboundService` 的文件，确认是否有 Controller 以外的调用方。

Run: `rg "IWmsInboundService" --include "*.java" -l`

- [ ] **Step 2: 根据引用结果决定处理方式**

- **无外部引用**: 删除 `IWmsInboundService` 中的 `pageList`、`receive`、`cancel` 方法声明，以及 `WmsInboundServiceImpl` 中的对应实现。保留 `IService` 继承的基础 CRUD 方法（`save`、`getById`、`updateById` 等）供可能的内部使用。
- **有外部引用**: 保留 `IWmsInboundService` 不变，仅在 `WmsInboundServiceImpl` 中将 `receive` 和 `cancel` 的实现委托给 `WmsInboundCommandService`，消除重复逻辑。

- [ ] **Step 3: 编译验证**

Run: `mvn compile -pl scm-warehouse/service -am -f com.scm.parent/pom.xml -q`
Expected: BUILD SUCCESS

---

### Task 6: 全量编译和测试验证

**Files:** 无新增/修改

- [ ] **Step 1: 全量编译**

Run: `mvn clean compile -f com.scm.parent/pom.xml -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: 运行 warehouse 模块测试**

Run: `mvn test -pl scm-warehouse/service -f com.scm.parent/pom.xml`
Expected: 所有测试通过

---

## 重构前后对比

| 维度 | 重构前 | 重构后 |
|------|--------|--------|
| Controller 行数 | 119 行（含业务逻辑） | ~80 行（纯路由） |
| 状态表示 | 魔法数字 `0/1/2/3/4` | `InboundStatus` 枚举 |
| 写操作入口 | Controller → IService → ServiceImpl（重复） + CommandService（重复） | Controller → CommandService（唯一） |
| 读操作入口 | Controller → IService → ServiceImpl + QueryService（重复） | Controller → QueryService（唯一） |
| 读写分离 | 无 `@Master`/`@Slave` | CommandService `@Master`，QueryService `@Slave` |

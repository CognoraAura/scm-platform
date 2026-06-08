# Warehouse Controller 全量重构实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将剩余 6 个 Warehouse Controller 中的业务逻辑下沉到 CQRS Service 层，统一架构风格。

**Architecture:** 每个 Controller 注入 Command/Query 服务替代 IService。所有业务逻辑（ID 生成、默认值设置、状态校验、软删除）移至 CommandService，查询逻辑集中在 QueryService。需要状态枚举的模块创建对应枚举类。

**Tech Stack:** Spring Boot, MyBatis-Plus, Baomidou dynamic-datasource (`@Master`/`@Slave`), Lombok `@Getter`

---

## 文件清单

| 操作 | 文件路径 | 职责 |
|------|---------|------|
| **模块 1: WmsOutbound** | | |
| Create | `domain/entity/OutboundStatus.java` | 出库单状态枚举 |
| Modify | `service/command/WmsOutboundCommandService.java` | 唯一写操作入口 |
| Modify | `service/query/WmsOutboundQueryService.java` | 唯一读操作入口 |
| Modify | `controller/WmsOutboundController.java` | 瘦 Controller |
| Modify | `service/IWmsOutboundService.java` | 移除重复方法 |
| Modify | `service/impl/WmsOutboundServiceImpl.java` | 移除重复实现 |
| **模块 2: WmsWavePicking** | | |
| Create | `domain/entity/WavePickingStatus.java` | 波次拣货状态枚举 |
| Modify | `service/command/WmsWavePickingCommandService.java` | 唯一写操作入口 |
| Modify | `service/query/WmsWavePickingQueryService.java` | 唯一读操作入口 |
| Modify | `controller/WmsWavePickingController.java` | 瘦 Controller |
| Modify | `service/IWmsWavePickingService.java` | 移除重复方法 |
| Modify | `service/impl/WmsWavePickingServiceImpl.java` | 移除重复实现 |
| **模块 3: WmsWarehouse** | | |
| Modify | `service/command/WmsWarehouseCommandService.java` | 添加 create 方法 |
| Modify | `controller/WmsWarehouseController.java` | 瘦 Controller |
| Modify | `service/IWmsWarehouseService.java` | 移除重复方法 |
| Modify | `service/impl/WmsWarehouseServiceImpl.java` | 移除重复实现 |
| **模块 4: WmsLocation** | | |
| Modify | `service/command/WmsLocationCommandService.java` | 添加 create 方法（含唯一性校验） |
| Modify | `controller/WmsLocationController.java` | 瘦 Controller |
| Modify | `service/IWmsLocationService.java` | 移除重复方法 |
| Modify | `service/impl/WmsLocationServiceImpl.java` | 移除重复实现 |
| **模块 5: WmsInboundItem** | | |
| Modify | `service/command/WmsInboundItemCommandService.java` | 添加 create 方法 |
| Modify | `controller/WmsInboundItemController.java` | 瘦 Controller |
| Modify | `service/IWmsInboundItemService.java` | 移除重复方法 |
| Modify | `service/impl/WmsInboundItemServiceImpl.java` | 移除重复实现 |
| **模块 6: WmsOutboundItem** | | |
| Modify | `service/command/WmsOutboundItemCommandService.java` | 添加 create 方法 |
| Modify | `controller/WmsOutboundItemController.java` | 瘦 Controller |
| Modify | `service/IWmsOutboundItemService.java` | 移除重复方法 |
| Modify | `service/impl/WmsOutboundItemServiceImpl.java` | 移除重复实现 |

---

### Task 1: 创建 OutboundStatus 枚举 + 重构 WmsOutboundCommandService

**Files:**
- Create: `scm-warehouse/service/src/main/java/com/scmcloud/warehouse/domain/entity/OutboundStatus.java`
- Modify: `scm-warehouse/service/src/main/java/com/scmcloud/warehouse/service/command/WmsOutboundCommandService.java`

- [ ] **Step 1: 创建 OutboundStatus 枚举**

```java
package com.scmcloud.warehouse.domain.entity;

import lombok.Getter;
import java.util.Set;

@Getter
public enum OutboundStatus {

    WAITING(0, "待拣货"),
    PICKING(1, "拣货中"),
    PACKED(2, "已打包"),
    SHIPPED(3, "已出库"),
    CANCELLED(4, "已取消");

    private final int code;
    private final String description;

    OutboundStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static OutboundStatus fromCode(int code) {
        for (OutboundStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown outbound status code: " + code);
    }

    public Set<OutboundStatus> validNextStatuses() {
        return switch (this) {
            case WAITING -> Set.of(PICKING, CANCELLED);
            case PICKING -> Set.of(PACKED, CANCELLED);
            case PACKED -> Set.of(SHIPPED, CANCELLED);
            case SHIPPED, CANCELLED -> Set.of();
        };
    }

    public boolean canTransitionTo(OutboundStatus target) {
        return validNextStatuses().contains(target);
    }

    public boolean isTerminal() {
        return validNextStatuses().isEmpty();
    }

    public boolean isCancellable() {
        return this == WAITING || this == PICKING || this == PACKED;
    }
}
```

- [ ] **Step 2: 重写 WmsOutboundCommandService**

添加 `create`、`update`、`softDeleteById` 方法，重构 `ship`、`cancel` 使用 `OutboundStatus` 枚举。

```java
package com.scmcloud.warehouse.service.command;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.common.status.StatusValidator;
import com.scmcloud.common.util.UUIDv7Util;
import com.scmcloud.warehouse.domain.entity.OutboundStatus;
import com.scmcloud.warehouse.domain.entity.WmsOutbound;
import com.scmcloud.warehouse.domain.entity.WmsOutboundItem;
import com.scmcloud.warehouse.mapper.WmsOutboundItemMapper;
import com.scmcloud.warehouse.mapper.WmsOutboundMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WmsOutboundCommandService {

    private final WmsOutboundMapper outboundMapper;
    private final WmsOutboundItemMapper outboundItemMapper;
    private final StatusValidator statusValidator;

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public WmsOutbound create(WmsOutbound outbound) {
        outbound.setId(UUIDv7Util.generateString());
        outbound.setOutboundNo("OUT" + System.currentTimeMillis());
        outbound.setStatus(OutboundStatus.WAITING.getCode());
        outbound.setPickedQuantity(0);
        outbound.setDeleted(false);
        outbound.setCreateTime(LocalDateTime.now());
        outbound.setUpdateTime(LocalDateTime.now());
        outboundMapper.insert(outbound);
        log.info("出库单创建成功 id={}, outboundNo={}", outbound.getId(), outbound.getOutboundNo());
        return outbound;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean update(WmsOutbound outbound) {
        WmsOutbound existing = outboundMapper.selectById(outbound.getId());
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            return false;
        }
        if (existing.getStatus() != OutboundStatus.WAITING.getCode()) {
            throw new IllegalStateException("只有待拣货状态的出库单才能修改");
        }
        outbound.setUpdateTime(LocalDateTime.now());
        return outboundMapper.updateById(outbound) > 0;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean softDeleteById(String id) {
        WmsOutbound outbound = outboundMapper.selectById(id);
        if (outbound == null || Boolean.TRUE.equals(outbound.getDeleted())) {
            return false;
        }
        outbound.setDeleted(true);
        outbound.setUpdateTime(LocalDateTime.now());
        return outboundMapper.updateById(outbound) > 0;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean ship(String outboundId, String operatorId, String operatorName) {
        WmsOutbound outbound = outboundMapper.selectById(outboundId);
        if (outbound == null) {
            log.warn("[ship] 出库单不存在: id={}", outboundId);
            return false;
        }

        List<WmsOutboundItem> items = outboundItemMapper.selectList(
                Wrappers.<WmsOutboundItem>lambdaQuery()
                        .eq(WmsOutboundItem::getOutboundId, outboundId)
                        .eq(WmsOutboundItem::getDeleted, false));

        int totalPicked = items.stream()
                .mapToInt(item -> item.getActualQuantity() != null ? item.getActualQuantity() : 0)
                .sum();

        if (totalPicked == 0) {
            throw new IllegalStateException("出库明细实际拣货数量不能全部为0");
        }

        OutboundStatus currentStatus = OutboundStatus.fromCode(outbound.getStatus());
        statusValidator.validateTransition("OUTBOUND", currentStatus.name(), "SHIPPED");

        outbound.setPickedQuantity(totalPicked);
        outbound.setStatus(OutboundStatus.SHIPPED.getCode());
        outbound.setOperatorId(operatorId);
        outbound.setOperatorName(operatorName);
        outbound.setUpdateTime(LocalDateTime.now());
        outbound.setUpdateBy(operatorId);
        outbound.setShippedAt(LocalDateTime.now());

        boolean success = outboundMapper.updateById(outbound) > 0;
        if (success) {
            log.info("出库单已出库: id={}, outboundNo={}, pickedQty={}", outboundId, outbound.getOutboundNo(), totalPicked);
        }
        return success;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(String outboundId, String operatorId, String operatorName) {
        WmsOutbound outbound = outboundMapper.selectById(outboundId);
        if (outbound == null) {
            log.warn("[cancel] 出库单不存在: id={}", outboundId);
            return false;
        }

        OutboundStatus currentStatus = OutboundStatus.fromCode(outbound.getStatus());
        statusValidator.validateTransition("OUTBOUND", currentStatus.name(), "CANCELLED");

        outbound.setStatus(OutboundStatus.CANCELLED.getCode());
        outbound.setOperatorId(operatorId);
        outbound.setOperatorName(operatorName);
        outbound.setUpdateTime(LocalDateTime.now());
        outbound.setUpdateBy(operatorId);

        boolean success = outboundMapper.updateById(outbound) > 0;
        if (success) {
            log.info("出库单已取消: id={}, outboundNo={}", outboundId, outbound.getOutboundNo());
        }
        return success;
    }
}
```

---

### Task 2: 重构 WmsOutboundController + 清理 ServiceImpl

**Files:**
- Modify: `scm-warehouse/service/src/main/java/com/scmcloud/warehouse/controller/WmsOutboundController.java`
- Modify: `scm-warehouse/service/src/main/java/com/scmcloud/warehouse/service/IWmsOutboundService.java`
- Modify: `scm-warehouse/service/src/main/java/com/scmcloud/warehouse/service/impl/WmsOutboundServiceImpl.java`

- [ ] **Step 1: 重写 WmsOutboundController**

```java
package com.scmcloud.warehouse.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.warehouse.domain.entity.WmsOutbound;
import com.scmcloud.warehouse.service.command.WmsOutboundCommandService;
import com.scmcloud.warehouse.service.query.WmsOutboundQueryService;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/wms-outbound")
public class WmsOutboundController {

    private final WmsOutboundCommandService outboundCommandService;
    private final WmsOutboundQueryService outboundQueryService;

    @PostMapping
    public ApiResponse<WmsOutbound> create(@RequestBody WmsOutbound outbound) {
        log.info("[API] 创建出库单 warehouseId={}, type={}", outbound.getWarehouseId(), outbound.getOutboundType());
        WmsOutbound created = outboundCommandService.create(outbound);
        log.info("[API] 出库单创建成功 id={}, outboundNo={}", created.getId(), created.getOutboundNo());
        return ApiResponse.success(created);
    }

    @PutMapping("/{id}")
    public ApiResponse<WmsOutbound> update(@PathVariable String id, @RequestBody WmsOutbound outbound) {
        log.info("[API] 更新出库单 id={}", id);
        outbound.setId(id);
        try {
            boolean success = outboundCommandService.update(outbound);
            if (!success) {
                return ApiResponse.fail(404, "出库单不存在");
            }
            return ApiResponse.success(outboundQueryService.getById(id));
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("[API] 删除出库单 id={}", id);
        boolean success = outboundCommandService.softDeleteById(id);
        return success ? ApiResponse.success() : ApiResponse.fail(404, "出库单不存在");
    }

    @GetMapping("/{id}")
    public ApiResponse<WmsOutbound> getById(@PathVariable String id) {
        WmsOutbound outbound = outboundQueryService.getById(id);
        if (outbound == null || Boolean.TRUE.equals(outbound.getDeleted())) {
            return ApiResponse.fail(404, "出库单不存在");
        }
        return ApiResponse.success(outbound);
    }

    @GetMapping("/page")
    public ApiResponse<Page<WmsOutbound>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) Integer outboundType,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.success(outboundQueryService.pageList(page, size, warehouseId, outboundType, status));
    }

    @PutMapping("/{id}/ship")
    public ApiResponse<Void> ship(
            @PathVariable String id,
            @RequestParam String operatorId,
            @RequestParam String operatorName) {
        log.info("[API] 出库确认: id={}, operator={}", id, operatorName);
        try {
            boolean success = outboundCommandService.ship(id, operatorId, operatorName);
            return success ? ApiResponse.success() : ApiResponse.fail(400, "出库失败");
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(
            @PathVariable String id,
            @RequestParam String operatorId,
            @RequestParam String operatorName) {
        log.info("[API] 取消出库单 id={}, operator={}", id, operatorName);
        try {
            boolean success = outboundCommandService.cancel(id, operatorId, operatorName);
            return success ? ApiResponse.success() : ApiResponse.fail(400, "取消失败");
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }
}
```

- [ ] **Step 2: 清理 IWmsOutboundService**

```java
package com.scmcloud.warehouse.service;

import com.scmcloud.warehouse.domain.entity.WmsOutbound;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IWmsOutboundService extends IService<WmsOutbound> {
}
```

- [ ] **Step 3: 清理 WmsOutboundServiceImpl**

```java
package com.scmcloud.warehouse.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.scmcloud.warehouse.domain.entity.WmsOutbound;
import com.scmcloud.warehouse.mapper.WmsOutboundMapper;
import com.scmcloud.warehouse.service.IWmsOutboundService;

@RequiredArgsConstructor
@Slf4j
@Service
public class WmsOutboundServiceImpl extends ServiceImpl<WmsOutboundMapper, WmsOutbound>
        implements IWmsOutboundService {
}
```

---

### Task 3: 创建 WavePickingStatus 枚举 + 重构 WmsWavePickingCommandService

**Files:**
- Create: `scm-warehouse/service/src/main/java/com/scmcloud/warehouse/domain/entity/WavePickingStatus.java`
- Modify: `scm-warehouse/service/src/main/java/com/scmcloud/warehouse/service/command/WmsWavePickingCommandService.java`

- [ ] **Step 1: 创建 WavePickingStatus 枚举**

```java
package com.scmcloud.warehouse.domain.entity;

import lombok.Getter;
import java.util.Set;

@Getter
public enum WavePickingStatus {

    WAITING(0, "待拣货"),
    PICKING(1, "拣货中"),
    COMPLETED(2, "已完成"),
    CANCELLED(3, "已取消");

    private final int code;
    private final String description;

    WavePickingStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static WavePickingStatus fromCode(int code) {
        for (WavePickingStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown wave picking status code: " + code);
    }

    public Set<WavePickingStatus> validNextStatuses() {
        return switch (this) {
            case WAITING -> Set.of(PICKING, CANCELLED);
            case PICKING -> Set.of(COMPLETED, CANCELLED);
            case COMPLETED, CANCELLED -> Set.of();
        };
    }

    public boolean canTransitionTo(WavePickingStatus target) {
        return validNextStatuses().contains(target);
    }

    public boolean isTerminal() {
        return validNextStatuses().isEmpty();
    }

    public boolean isCancellable() {
        return this == WAITING || this == PICKING;
    }
}
```

- [ ] **Step 2: 重写 WmsWavePickingCommandService**

添加 `create` 方法，重构 `start`、`complete`、`cancel` 使用 `WavePickingStatus` 枚举，修复 `removeById` 为软删除。

```java
package com.scmcloud.warehouse.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.common.status.StatusValidator;
import com.scmcloud.common.util.UUIDv7Util;
import com.scmcloud.warehouse.domain.entity.WavePickingStatus;
import com.scmcloud.warehouse.domain.entity.WmsWavePicking;
import com.scmcloud.warehouse.mapper.WmsWavePickingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WmsWavePickingCommandService {

    private final WmsWavePickingMapper wavePickingMapper;
    private final StatusValidator statusValidator;

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public WmsWavePicking create(WmsWavePicking wave) {
        wave.setId(UUIDv7Util.generateString());
        wave.setWaveNo("WAVE" + System.currentTimeMillis());
        wave.setStatus(WavePickingStatus.WAITING.getCode());
        wave.setCreateTime(LocalDateTime.now());
        wave.setUpdateTime(LocalDateTime.now());
        wavePickingMapper.insert(wave);
        log.info("波次拣货单创建成功 id={}, waveNo={}", wave.getId(), wave.getWaveNo());
        return wave;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean update(WmsWavePicking wave) {
        WmsWavePicking existing = wavePickingMapper.selectById(wave.getId());
        if (existing == null) {
            return false;
        }
        if (existing.getStatus() != WavePickingStatus.WAITING.getCode()) {
            throw new IllegalStateException("只有待拣货状态的波次拣货单才能修改");
        }
        wave.setUpdateTime(LocalDateTime.now());
        return wavePickingMapper.updateById(wave) > 0;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean softDeleteById(String id) {
        WmsWavePicking wave = wavePickingMapper.selectById(id);
        if (wave == null) {
            return false;
        }
        wave.setDeleted(true);
        wave.setUpdateTime(LocalDateTime.now());
        return wavePickingMapper.updateById(wave) > 0;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean start(String waveId, String pickerId, String pickerName) {
        WmsWavePicking wave = wavePickingMapper.selectById(waveId);
        if (wave == null) {
            log.warn("[start] 波次拣货单不存在: id={}", waveId);
            return false;
        }

        WavePickingStatus currentStatus = WavePickingStatus.fromCode(wave.getStatus());
        statusValidator.validateTransition("WAVE_PICKING", currentStatus.name(), "PICKING");

        wave.setStatus(WavePickingStatus.PICKING.getCode());
        wave.setPickerId(pickerId);
        wave.setPickerName(pickerName);
        wave.setStartedAt(LocalDateTime.now());
        wave.setUpdateTime(LocalDateTime.now());

        boolean success = wavePickingMapper.updateById(wave) > 0;
        if (success) {
            log.info("波次拣货已开始: id={}, waveNo={}, picker={}", waveId, wave.getWaveNo(), pickerName);
        }
        return success;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean complete(String waveId, String operatorId) {
        WmsWavePicking wave = wavePickingMapper.selectById(waveId);
        if (wave == null) {
            log.warn("[complete] 波次拣货单不存在: id={}", waveId);
            return false;
        }

        WavePickingStatus currentStatus = WavePickingStatus.fromCode(wave.getStatus());
        statusValidator.validateTransition("WAVE_PICKING", currentStatus.name(), "COMPLETED");

        wave.setStatus(WavePickingStatus.COMPLETED.getCode());
        wave.setCompletedAt(LocalDateTime.now());
        wave.setUpdateTime(LocalDateTime.now());

        boolean success = wavePickingMapper.updateById(wave) > 0;
        if (success) {
            log.info("波次拣货已完成: id={}, waveNo={}", waveId, wave.getWaveNo());
        }
        return success;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(String waveId, String operatorId) {
        WmsWavePicking wave = wavePickingMapper.selectById(waveId);
        if (wave == null) {
            log.warn("[cancel] 波次拣货单不存在: id={}", waveId);
            return false;
        }

        WavePickingStatus currentStatus = WavePickingStatus.fromCode(wave.getStatus());
        statusValidator.validateTransition("WAVE_PICKING", currentStatus.name(), "CANCELLED");

        wave.setStatus(WavePickingStatus.CANCELLED.getCode());
        wave.setUpdateTime(LocalDateTime.now());

        boolean success = wavePickingMapper.updateById(wave) > 0;
        if (success) {
            log.info("波次拣货已取消: id={}, waveNo={}", waveId, wave.getWaveNo());
        }
        return success;
    }
}
```

---

### Task 4: 重构 WmsWavePickingController + 清理 ServiceImpl

**Files:**
- Modify: `scm-warehouse/service/src/main/java/com/scmcloud/warehouse/controller/WmsWavePickingController.java`
- Modify: `scm-warehouse/service/src/main/java/com/scmcloud/warehouse/service/IWmsWavePickingService.java`
- Modify: `scm-warehouse/service/src/main/java/com/scmcloud/warehouse/service/impl/WmsWavePickingServiceImpl.java`

- [ ] **Step 1: 重写 WmsWavePickingController**

```java
package com.scmcloud.warehouse.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.warehouse.domain.entity.WmsWavePicking;
import com.scmcloud.warehouse.service.command.WmsWavePickingCommandService;
import com.scmcloud.warehouse.service.query.WmsWavePickingQueryService;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/wms-wave-picking")
public class WmsWavePickingController {

    private final WmsWavePickingCommandService wavePickingCommandService;
    private final WmsWavePickingQueryService wavePickingQueryService;

    @PostMapping
    public ApiResponse<WmsWavePicking> create(@RequestBody WmsWavePicking wave) {
        log.info("[API] 创建波次拣货单 warehouseId={}, orderCount={}", wave.getWarehouseId(), wave.getOrderCount());
        WmsWavePicking created = wavePickingCommandService.create(wave);
        log.info("[API] 波次拣货单创建成功 id={}, waveNo={}", created.getId(), created.getWaveNo());
        return ApiResponse.success(created);
    }

    @PutMapping("/{id}")
    public ApiResponse<WmsWavePicking> update(@PathVariable String id, @RequestBody WmsWavePicking wave) {
        log.info("[API] 更新波次拣货单 id={}", id);
        wave.setId(id);
        try {
            boolean success = wavePickingCommandService.update(wave);
            if (!success) {
                return ApiResponse.fail(404, "波次拣货单不存在");
            }
            return ApiResponse.success(wavePickingQueryService.getById(id));
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("[API] 删除波次拣货单 id={}", id);
        boolean success = wavePickingCommandService.softDeleteById(id);
        return success ? ApiResponse.success() : ApiResponse.fail(404, "波次拣货单不存在");
    }

    @GetMapping("/{id}")
    public ApiResponse<WmsWavePicking> getById(@PathVariable String id) {
        WmsWavePicking wave = wavePickingQueryService.getById(id);
        if (wave == null) {
            return ApiResponse.fail(404, "波次拣货单不存在");
        }
        return ApiResponse.success(wave);
    }

    @GetMapping("/page")
    public ApiResponse<Page<WmsWavePicking>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.success(wavePickingQueryService.pageList(page, size, warehouseId, status));
    }

    @PutMapping("/{id}/start")
    public ApiResponse<Void> start(
            @PathVariable String id,
            @RequestParam String pickerId,
            @RequestParam String pickerName) {
        log.info("[API] 开始拣货 id={}, picker={}", id, pickerName);
        try {
            boolean success = wavePickingCommandService.start(id, pickerId, pickerName);
            return success ? ApiResponse.success() : ApiResponse.fail(400, "开始拣货失败");
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @PutMapping("/{id}/complete")
    public ApiResponse<Void> complete(
            @PathVariable String id,
            @RequestParam String operatorId) {
        log.info("[API] 完成拣货: id={}", id);
        try {
            boolean success = wavePickingCommandService.complete(id, operatorId);
            return success ? ApiResponse.success() : ApiResponse.fail(400, "完成拣货失败");
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(
            @PathVariable String id,
            @RequestParam String operatorId) {
        log.info("[API] 取消波次拣货单 id={}", id);
        try {
            boolean success = wavePickingCommandService.cancel(id, operatorId);
            return success ? ApiResponse.success() : ApiResponse.fail(400, "取消失败");
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }
}
```

- [ ] **Step 2: 清理 IWmsWavePickingService**

```java
package com.scmcloud.warehouse.service;

import com.scmcloud.warehouse.domain.entity.WmsWavePicking;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IWmsWavePickingService extends IService<WmsWavePicking> {
}
```

- [ ] **Step 3: 清理 WmsWavePickingServiceImpl**

```java
package com.scmcloud.warehouse.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.scmcloud.warehouse.domain.entity.WmsWavePicking;
import com.scmcloud.warehouse.mapper.WmsWavePickingMapper;
import com.scmcloud.warehouse.service.IWmsWavePickingService;

@RequiredArgsConstructor
@Slf4j
@Service
public class WmsWavePickingServiceImpl extends ServiceImpl<WmsWavePickingMapper, WmsWavePicking>
        implements IWmsWavePickingService {
}
```

---

### Task 5: 重构 WmsWarehouse — CommandService + Controller + 清理

**Files:**
- Modify: `scm-warehouse/service/src/main/java/com/scmcloud/warehouse/service/command/WmsWarehouseCommandService.java`
- Modify: `scm-warehouse/service/src/main/java/com/scmcloud/warehouse/controller/WmsWarehouseController.java`
- Modify: `scm-warehouse/service/src/main/java/com/scmcloud/warehouse/service/IWmsWarehouseService.java`
- Modify: `scm-warehouse/service/src/main/java/com/scmcloud/warehouse/service/impl/WmsWarehouseServiceImpl.java`

- [ ] **Step 1: 重写 WmsWarehouseCommandService — 添加 create 和 update**

在现有方法基础上添加 `create`（含编码唯一性校验）和 `update` 方法。

```java
package com.scmcloud.warehouse.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.common.util.UUIDv7Util;
import com.scmcloud.warehouse.domain.entity.WmsWarehouse;
import com.scmcloud.warehouse.mapper.WmsWarehouseMapper;
import com.scmcloud.warehouse.service.query.WmsWarehouseQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WmsWarehouseCommandService {

    private final WmsWarehouseMapper warehouseMapper;
    private final WmsWarehouseQueryService warehouseQueryService;

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public WmsWarehouse create(WmsWarehouse warehouse) {
        if (warehouseQueryService.existsByWarehouseCode(warehouse.getWarehouseCode())) {
            throw new IllegalStateException("仓库编码已存在: " + warehouse.getWarehouseCode());
        }
        warehouse.setId(UUIDv7Util.generateString());
        warehouse.setEnabled(true);
        warehouse.setUsedCapacity(0);
        warehouse.setDeleted(false);
        warehouse.setCreateTime(LocalDateTime.now());
        warehouse.setUpdateTime(LocalDateTime.now());
        warehouseMapper.insert(warehouse);
        log.info("仓库创建成功: id={}, code={}", warehouse.getId(), warehouse.getWarehouseCode());
        return warehouse;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean update(WmsWarehouse warehouse) {
        WmsWarehouse existing = warehouseMapper.selectById(warehouse.getId());
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            return false;
        }
        warehouse.setUpdateTime(LocalDateTime.now());
        return warehouseMapper.updateById(warehouse) > 0;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean softDeleteById(String id) {
        WmsWarehouse warehouse = warehouseMapper.selectById(id);
        if (warehouse == null || Boolean.TRUE.equals(warehouse.getDeleted())) {
            return false;
        }
        warehouse.setDeleted(true);
        warehouse.setUpdateTime(LocalDateTime.now());
        return warehouseMapper.updateById(warehouse) > 0;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean enable(String id) {
        WmsWarehouse warehouse = warehouseMapper.selectById(id);
        if (warehouse == null) {
            log.warn("[enable] 仓库不存在: id={}", id);
            return false;
        }
        warehouse.setEnabled(true);
        warehouse.setUpdateTime(LocalDateTime.now());
        boolean success = warehouseMapper.updateById(warehouse) > 0;
        if (success) {
            log.info("仓库已启用: id={}, name={}", id, warehouse.getWarehouseName());
        }
        return success;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean disable(String id) {
        WmsWarehouse warehouse = warehouseMapper.selectById(id);
        if (warehouse == null) {
            log.warn("[disable] 仓库不存在: id={}", id);
            return false;
        }
        warehouse.setEnabled(false);
        warehouse.setUpdateTime(LocalDateTime.now());
        boolean success = warehouseMapper.updateById(warehouse) > 0;
        if (success) {
            log.info("仓库已停用: id={}, name={}", id, warehouse.getWarehouseName());
        }
        return success;
    }
}
```

- [ ] **Step 2: 重写 WmsWarehouseController**

```java
package com.scmcloud.warehouse.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.warehouse.domain.entity.WmsWarehouse;
import com.scmcloud.warehouse.service.command.WmsWarehouseCommandService;
import com.scmcloud.warehouse.service.query.WmsWarehouseQueryService;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/wms-warehouse")
public class WmsWarehouseController {

    private final WmsWarehouseCommandService warehouseCommandService;
    private final WmsWarehouseQueryService warehouseQueryService;

    @PostMapping
    public ApiResponse<WmsWarehouse> create(@RequestBody WmsWarehouse warehouse) {
        log.info("[API] 创建仓库: code={}, name={}", warehouse.getWarehouseCode(), warehouse.getWarehouseName());
        try {
            WmsWarehouse created = warehouseCommandService.create(warehouse);
            log.info("[API] 仓库创建成功: id={}", created.getId());
            return ApiResponse.success(created);
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<WmsWarehouse> update(@PathVariable String id, @RequestBody WmsWarehouse warehouse) {
        log.info("[API] 更新仓库: id={}", id);
        warehouse.setId(id);
        boolean success = warehouseCommandService.update(warehouse);
        if (!success) {
            return ApiResponse.fail(404, "仓库不存在");
        }
        return ApiResponse.success(warehouseQueryService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("[API] 删除仓库: id={}", id);
        boolean success = warehouseCommandService.softDeleteById(id);
        return success ? ApiResponse.success() : ApiResponse.fail(404, "仓库不存在");
    }

    @GetMapping("/{id}")
    public ApiResponse<WmsWarehouse> getById(@PathVariable String id) {
        WmsWarehouse warehouse = warehouseQueryService.getById(id);
        if (warehouse == null || Boolean.TRUE.equals(warehouse.getDeleted())) {
            return ApiResponse.fail(404, "仓库不存在");
        }
        return ApiResponse.success(warehouse);
    }

    @GetMapping("/list")
    public ApiResponse<List<WmsWarehouse>> listEnabled() {
        return ApiResponse.success(warehouseQueryService.listEnabled());
    }

    @GetMapping("/page")
    public ApiResponse<Page<WmsWarehouse>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String warehouseName,
            @RequestParam(required = false) Integer warehouseType,
            @RequestParam(required = false) Boolean enabled) {
        return ApiResponse.success(warehouseQueryService.pageList(page, size, warehouseName, warehouseType, enabled));
    }

    @PutMapping("/{id}/enable")
    public ApiResponse<Void> enable(@PathVariable String id) {
        log.info("[API] 启用仓库: id={}", id);
        boolean success = warehouseCommandService.enable(id);
        return success ? ApiResponse.success() : ApiResponse.fail(400, "启用失败");
    }

    @PutMapping("/{id}/disable")
    public ApiResponse<Void> disable(@PathVariable String id) {
        log.info("[API] 停用仓库: id={}", id);
        boolean success = warehouseCommandService.disable(id);
        return success ? ApiResponse.success() : ApiResponse.fail(400, "停用失败");
    }
}
```

- [ ] **Step 3: 清理 IWmsWarehouseService**

```java
package com.scmcloud.warehouse.service;

import com.scmcloud.warehouse.domain.entity.WmsWarehouse;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IWmsWarehouseService extends IService<WmsWarehouse> {
}
```

- [ ] **Step 4: 清理 WmsWarehouseServiceImpl**

```java
package com.scmcloud.warehouse.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.scmcloud.warehouse.domain.entity.WmsWarehouse;
import com.scmcloud.warehouse.mapper.WmsWarehouseMapper;
import com.scmcloud.warehouse.service.IWmsWarehouseService;

@RequiredArgsConstructor
@Slf4j
@Service
public class WmsWarehouseServiceImpl extends ServiceImpl<WmsWarehouseMapper, WmsWarehouse>
        implements IWmsWarehouseService {
}
```

---

### Task 6: 重构 WmsLocation — CommandService + Controller + 清理

**Files:**
- Modify: `scm-warehouse/service/src/main/java/com/scmcloud/warehouse/service/command/WmsLocationCommandService.java`
- Modify: `scm-warehouse/service/src/main/java/com/scmcloud/warehouse/controller/WmsLocationController.java`
- Modify: `scm-warehouse/service/src/main/java/com/scmcloud/warehouse/service/IWmsLocationService.java`
- Modify: `scm-warehouse/service/src/main/java/com/scmcloud/warehouse/service/impl/WmsLocationServiceImpl.java`

- [ ] **Step 1: 重写 WmsLocationCommandService — 添加 create（含唯一性校验）**

```java
package com.scmcloud.warehouse.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.common.util.UUIDv7Util;
import com.scmcloud.warehouse.domain.entity.WmsLocation;
import com.scmcloud.warehouse.mapper.WmsLocationMapper;
import com.scmcloud.warehouse.service.query.WmsLocationQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WmsLocationCommandService {

    private final WmsLocationMapper locationMapper;
    private final WmsLocationQueryService locationQueryService;

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public WmsLocation create(WmsLocation location) {
        if (locationQueryService.existsByWarehouseIdAndCode(location.getWarehouseId(), location.getLocationCode())) {
            throw new IllegalStateException("同一仓库下库位编码已存在: " + location.getLocationCode());
        }
        location.setId(UUIDv7Util.generateString());
        location.setCurrentCapacity(0);
        location.setStatus(1);
        location.setEnabled(true);
        location.setDeleted(false);
        location.setCreateTime(LocalDateTime.now());
        location.setUpdateTime(LocalDateTime.now());
        locationMapper.insert(location);
        log.info("库位创建成功: id={}, code={}", location.getId(), location.getLocationCode());
        return location;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean update(WmsLocation location) {
        WmsLocation existing = locationMapper.selectById(location.getId());
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            return false;
        }
        location.setUpdateTime(LocalDateTime.now());
        return locationMapper.updateById(location) > 0;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean softDeleteById(String id) {
        WmsLocation location = locationMapper.selectById(id);
        if (location == null || Boolean.TRUE.equals(location.getDeleted())) {
            return false;
        }
        location.setDeleted(true);
        location.setUpdateTime(LocalDateTime.now());
        return locationMapper.updateById(location) > 0;
    }
}
```

- [ ] **Step 2: 重写 WmsLocationController**

```java
package com.scmcloud.warehouse.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.warehouse.domain.entity.WmsLocation;
import com.scmcloud.warehouse.service.command.WmsLocationCommandService;
import com.scmcloud.warehouse.service.query.WmsLocationQueryService;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/wms-location")
public class WmsLocationController {

    private final WmsLocationCommandService locationCommandService;
    private final WmsLocationQueryService locationQueryService;

    @PostMapping
    public ApiResponse<WmsLocation> create(@RequestBody WmsLocation location) {
        log.info("[API] 创建库位: warehouseId={}, code={}", location.getWarehouseId(), location.getLocationCode());
        try {
            WmsLocation created = locationCommandService.create(location);
            log.info("[API] 库位创建成功: id={}", created.getId());
            return ApiResponse.success(created);
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<WmsLocation> update(@PathVariable String id, @RequestBody WmsLocation location) {
        log.info("[API] 更新库位: id={}", id);
        location.setId(id);
        boolean success = locationCommandService.update(location);
        if (!success) {
            return ApiResponse.fail(404, "库位不存在");
        }
        return ApiResponse.success(locationQueryService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("[API] 删除库位: id={}", id);
        boolean success = locationCommandService.softDeleteById(id);
        return success ? ApiResponse.success() : ApiResponse.fail(404, "库位不存在");
    }

    @GetMapping("/{id}")
    public ApiResponse<WmsLocation> getById(@PathVariable String id) {
        WmsLocation location = locationQueryService.getById(id);
        if (location == null || Boolean.TRUE.equals(location.getDeleted())) {
            return ApiResponse.fail(404, "库位不存在");
        }
        return ApiResponse.success(location);
    }

    @GetMapping("/list")
    public ApiResponse<List<WmsLocation>> listByWarehouseId(
            @RequestParam String warehouseId) {
        return ApiResponse.success(locationQueryService.listByWarehouseId(warehouseId));
    }

    @GetMapping("/page")
    public ApiResponse<Page<WmsLocation>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) Integer locationType,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.success(locationQueryService.pageList(page, size, warehouseId, locationType, status));
    }
}
```

- [ ] **Step 3: 清理 IWmsLocationService**

```java
package com.scmcloud.warehouse.service;

import com.scmcloud.warehouse.domain.entity.WmsLocation;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IWmsLocationService extends IService<WmsLocation> {
}
```

- [ ] **Step 4: 清理 WmsLocationServiceImpl**

```java
package com.scmcloud.warehouse.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.scmcloud.warehouse.domain.entity.WmsLocation;
import com.scmcloud.warehouse.mapper.WmsLocationMapper;
import com.scmcloud.warehouse.service.IWmsLocationService;

@RequiredArgsConstructor
@Slf4j
@Service
public class WmsLocationServiceImpl extends ServiceImpl<WmsLocationMapper, WmsLocation>
        implements IWmsLocationService {
}
```

---

### Task 7: 重构 WmsInboundItem — CommandService + Controller + 清理

**Files:**
- Modify: `scm-warehouse/service/src/main/java/com/scmcloud/warehouse/service/command/WmsInboundItemCommandService.java`
- Modify: `scm-warehouse/service/src/main/java/com/scmcloud/warehouse/controller/WmsInboundItemController.java`
- Modify: `scm-warehouse/service/src/main/java/com/scmcloud/warehouse/service/IWmsInboundItemService.java`
- Modify: `scm-warehouse/service/src/main/java/com/scmcloud/warehouse/service/impl/WmsInboundItemServiceImpl.java`

- [ ] **Step 1: 重写 WmsInboundItemCommandService — 添加 create**

```java
package com.scmcloud.warehouse.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.common.util.UUIDv7Util;
import com.scmcloud.warehouse.domain.entity.WmsInboundItem;
import com.scmcloud.warehouse.mapper.WmsInboundItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WmsInboundItemCommandService {

    private final WmsInboundItemMapper inboundItemMapper;

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public WmsInboundItem create(WmsInboundItem item) {
        item.setId(UUIDv7Util.generateString());
        item.setActualQuantity(0);
        item.setQualityStatus(1);
        item.setDeleted(false);
        item.setCreateTime(LocalDateTime.now());
        item.setUpdateTime(LocalDateTime.now());
        inboundItemMapper.insert(item);
        log.info("入库明细创建成功: id={}", item.getId());
        return item;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean update(WmsInboundItem item) {
        WmsInboundItem existing = inboundItemMapper.selectById(item.getId());
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            return false;
        }
        item.setUpdateTime(LocalDateTime.now());
        return inboundItemMapper.updateById(item) > 0;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean softDeleteById(String id) {
        WmsInboundItem item = inboundItemMapper.selectById(id);
        if (item == null || Boolean.TRUE.equals(item.getDeleted())) {
            return false;
        }
        item.setDeleted(true);
        item.setUpdateTime(LocalDateTime.now());
        return inboundItemMapper.updateById(item) > 0;
    }
}
```

- [ ] **Step 2: 重写 WmsInboundItemController**

```java
package com.scmcloud.warehouse.controller;

import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.warehouse.domain.entity.WmsInboundItem;
import com.scmcloud.warehouse.service.command.WmsInboundItemCommandService;
import com.scmcloud.warehouse.service.query.WmsInboundItemQueryService;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/wms-inbound-item")
public class WmsInboundItemController {

    private final WmsInboundItemCommandService inboundItemCommandService;
    private final WmsInboundItemQueryService inboundItemQueryService;

    @PostMapping
    public ApiResponse<WmsInboundItem> create(@RequestBody WmsInboundItem item) {
        log.info("[API] 创建入库明细: inboundId={}, skuId={}", item.getInboundId(), item.getSkuId());
        WmsInboundItem created = inboundItemCommandService.create(item);
        log.info("[API] 入库明细创建成功: id={}", created.getId());
        return ApiResponse.success(created);
    }

    @PutMapping("/{id}")
    public ApiResponse<WmsInboundItem> update(@PathVariable String id, @RequestBody WmsInboundItem item) {
        log.info("[API] 更新入库明细: id={}", id);
        item.setId(id);
        boolean success = inboundItemCommandService.update(item);
        if (!success) {
            return ApiResponse.fail(404, "入库明细不存在");
        }
        return ApiResponse.success(inboundItemQueryService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("[API] 删除入库明细: id={}", id);
        boolean success = inboundItemCommandService.softDeleteById(id);
        return success ? ApiResponse.success() : ApiResponse.fail(404, "入库明细不存在");
    }

    @GetMapping("/{id}")
    public ApiResponse<WmsInboundItem> getById(@PathVariable String id) {
        WmsInboundItem item = inboundItemQueryService.getById(id);
        if (item == null || Boolean.TRUE.equals(item.getDeleted())) {
            return ApiResponse.fail(404, "入库明细不存在");
        }
        return ApiResponse.success(item);
    }

    @GetMapping("/list")
    public ApiResponse<List<WmsInboundItem>> listByInboundId(
            @RequestParam String inboundId) {
        return ApiResponse.success(inboundItemQueryService.listByInboundId(inboundId));
    }
}
```

- [ ] **Step 3: 清理 IWmsInboundItemService**

```java
package com.scmcloud.warehouse.service;

import com.scmcloud.warehouse.domain.entity.WmsInboundItem;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IWmsInboundItemService extends IService<WmsInboundItem> {
}
```

- [ ] **Step 4: 清理 WmsInboundItemServiceImpl**

```java
package com.scmcloud.warehouse.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.scmcloud.warehouse.domain.entity.WmsInboundItem;
import com.scmcloud.warehouse.mapper.WmsInboundItemMapper;
import com.scmcloud.warehouse.service.IWmsInboundItemService;

@RequiredArgsConstructor
@Slf4j
@Service
public class WmsInboundItemServiceImpl extends ServiceImpl<WmsInboundItemMapper, WmsInboundItem>
        implements IWmsInboundItemService {
}
```

---

### Task 8: 重构 WmsOutboundItem — CommandService + Controller + 清理

**Files:**
- Modify: `scm-warehouse/service/src/main/java/com/scmcloud/warehouse/service/command/WmsOutboundItemCommandService.java`
- Modify: `scm-warehouse/service/src/main/java/com/scmcloud/warehouse/controller/WmsOutboundItemController.java`
- Modify: `scm-warehouse/service/src/main/java/com/scmcloud/warehouse/service/IWmsOutboundItemService.java`
- Modify: `scm-warehouse/service/src/main/java/com/scmcloud/warehouse/service/impl/WmsOutboundItemServiceImpl.java`

- [ ] **Step 1: 重写 WmsOutboundItemCommandService — 添加 create**

```java
package com.scmcloud.warehouse.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.common.util.UUIDv7Util;
import com.scmcloud.warehouse.domain.entity.WmsOutboundItem;
import com.scmcloud.warehouse.mapper.WmsOutboundItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WmsOutboundItemCommandService {

    private final WmsOutboundItemMapper outboundItemMapper;

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public WmsOutboundItem create(WmsOutboundItem item) {
        item.setId(UUIDv7Util.generateString());
        item.setActualQuantity(0);
        item.setDeleted(false);
        item.setCreateTime(LocalDateTime.now());
        item.setUpdateTime(LocalDateTime.now());
        outboundItemMapper.insert(item);
        log.info("出库明细创建成功: id={}", item.getId());
        return item;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean update(WmsOutboundItem item) {
        WmsOutboundItem existing = outboundItemMapper.selectById(item.getId());
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            return false;
        }
        item.setUpdateTime(LocalDateTime.now());
        return outboundItemMapper.updateById(item) > 0;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean softDeleteById(String id) {
        WmsOutboundItem item = outboundItemMapper.selectById(id);
        if (item == null || Boolean.TRUE.equals(item.getDeleted())) {
            return false;
        }
        item.setDeleted(true);
        item.setUpdateTime(LocalDateTime.now());
        return outboundItemMapper.updateById(item) > 0;
    }
}
```

- [ ] **Step 2: 重写 WmsOutboundItemController**

```java
package com.scmcloud.warehouse.controller;

import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.warehouse.domain.entity.WmsOutboundItem;
import com.scmcloud.warehouse.service.command.WmsOutboundItemCommandService;
import com.scmcloud.warehouse.service.query.WmsOutboundItemQueryService;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/wms-outbound-item")
public class WmsOutboundItemController {

    private final WmsOutboundItemCommandService outboundItemCommandService;
    private final WmsOutboundItemQueryService outboundItemQueryService;

    @PostMapping
    public ApiResponse<WmsOutboundItem> create(@RequestBody WmsOutboundItem item) {
        log.info("[API] 创建出库明细: outboundId={}, skuId={}", item.getOutboundId(), item.getSkuId());
        WmsOutboundItem created = outboundItemCommandService.create(item);
        log.info("[API] 出库明细创建成功: id={}", created.getId());
        return ApiResponse.success(created);
    }

    @PutMapping("/{id}")
    public ApiResponse<WmsOutboundItem> update(@PathVariable String id, @RequestBody WmsOutboundItem item) {
        log.info("[API] 更新出库明细: id={}", id);
        item.setId(id);
        boolean success = outboundItemCommandService.update(item);
        if (!success) {
            return ApiResponse.fail(404, "出库明细不存在");
        }
        return ApiResponse.success(outboundItemQueryService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("[API] 删除出库明细: id={}", id);
        boolean success = outboundItemCommandService.softDeleteById(id);
        return success ? ApiResponse.success() : ApiResponse.fail(404, "出库明细不存在");
    }

    @GetMapping("/{id}")
    public ApiResponse<WmsOutboundItem> getById(@PathVariable String id) {
        WmsOutboundItem item = outboundItemQueryService.getById(id);
        if (item == null || Boolean.TRUE.equals(item.getDeleted())) {
            return ApiResponse.fail(404, "出库明细不存在");
        }
        return ApiResponse.success(item);
    }

    @GetMapping("/list")
    public ApiResponse<List<WmsOutboundItem>> listByOutboundId(
            @RequestParam String outboundId) {
        return ApiResponse.success(outboundItemQueryService.listByOutboundId(outboundId));
    }
}
```

- [ ] **Step 3: 清理 IWmsOutboundItemService**

```java
package com.scmcloud.warehouse.service;

import com.scmcloud.warehouse.domain.entity.WmsOutboundItem;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IWmsOutboundItemService extends IService<WmsOutboundItem> {
}
```

- [ ] **Step 4: 清理 WmsOutboundItemServiceImpl**

```java
package com.scmcloud.warehouse.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.scmcloud.warehouse.domain.entity.WmsOutboundItem;
import com.scmcloud.warehouse.mapper.WmsOutboundItemMapper;
import com.scmcloud.warehouse.service.IWmsOutboundItemService;

@RequiredArgsConstructor
@Slf4j
@Service
public class WmsOutboundItemServiceImpl extends ServiceImpl<WmsOutboundItemMapper, WmsOutboundItem>
        implements IWmsOutboundItemService {
}
```

---

## 注意事项

1. **Lombok @Getter**: 枚举类使用 `@Getter` 注解，不手动定义 getter 方法
2. **日志区分**: 同一类中相似的 warn/info 日志添加操作前缀 `[start]`/`[complete]`/`[cancel]` 等
3. **int 比较**: `InboundStatus.getCode()` 返回 `int` 原始类型，使用 `!=` 而非 `.equals()`
4. **switch 合并**: 终态分支合并为 `case COMPLETED, CANCELLED -> Set.of()`
5. **软删除**: WmsWavePicking 原为物理删除，统一改为软删除

# Project Layering Refactoring Design

Date: 2026-06-01  
Branch: `refactor/layering-unification`  
Author: —  
Reviewer: —

---

## Scope

Unify the entire SCM platform's package structure, standardize layering conventions, eliminate entity duplication, and implement CQRS separation across all business modules.

---

## 1. Package Naming Standardization

### New Convention

All modules use `com.scmcloud.{module}` as the base package.

**Decision Rationale**: The legacy `com.frog.*` prefix was a historical artifact from the initial prototype. Standardizing to `com.scmcloud` aligns the codebase with the registered organization namespace and eliminates confusion caused by mixed prefixes (`com.frog.*`, `scm.*`) across different modules.

### Mapping Table

| Module | Current Package | New Package |
|--------|----------------|-------------|
| scm-common/core | `com.frog.common` | `com.scmcloud.common` |
| scm-common/data | `com.frog.common` | `com.scmcloud.common` |
| scm-common/web | `com.frog.common` | `com.scmcloud.common` |
| scm-common/monitoring | `com.frog.common` | `com.scmcloud.common` |
| scm-common/integration | `com.frog.common` | `com.scmcloud.common` |
| scm-common/security/core | `com.frog.common.web` | `com.scmcloud.common.web` |
| scm-common/security/api | `com.frog.common.security` | `com.scmcloud.common.security` |
| scm-gateway | `com.frog.gateway` | `com.scmcloud.gateway` |
| scm-auth | `com.frog.auth` | `com.scmcloud.auth` |
| scm-system/service | `com.frog.system` | `com.scmcloud.system` |
| scm-system/api | `com.frog.system.api` | `com.scmcloud.system.api` |
| scm-inventory/service | `com.frog.inventory` | `com.scmcloud.inventory` |
| scm-inventory/api | `com.frog.inventory.api` | `com.scmcloud.inventory.api` |
| scm-order/service | `scm.order` | `com.scmcloud.order` |
| scm-order/api | `com.frog.order.api` | `com.scmcloud.order.api` |
| scm-product/service | `scm.product` | `com.scmcloud.product` |
| scm-product/api | `com.frog.product.api` | `com.scmcloud.product.api` |
| scm-warehouse/service | `scm.warehouse` | `com.scmcloud.warehouse` |
| scm-warehouse/api | `com.frog.warehouse.api` | `com.scmcloud.warehouse.api` |
| scm-logistics/service | `scm.logistics` | `com.scmcloud.logistics` |
| scm-logistics/api | `com.frog.logistics.api` | `com.scmcloud.logistics.api` |
| scm-supplier/service | `scm.supplier` | `com.scmcloud.supplier` |
| scm-supplier/api | `com.frog.supplier.api` | `com.scmcloud.supplier.api` |
| scm-purchase/service | `scm.purchase` | `com.scmcloud.purchase` |
| scm-purchase/api | `com.frog.purchase.api` | `com.scmcloud.purchase.api` |
| scm-finance/service | `scm.finance` | `com.scmcloud.finance` |
| scm-finance/api | `com.frog.finance.api` | `com.scmcloud.finance.api` |
| scm-approval/service | `scm.approval` | `com.scmcloud.approval` |
| scm-approval/api | `com.frog.approval.api` | `com.scmcloud.approval.api` |
| scm-audit/service | `scm.audit` | `com.scmcloud.audit` |
| scm-audit/api | `com.frog.audit.api` | `com.scmcloud.audit.api` |
| scm-notify/service | `scm.notify` | `com.scmcloud.notify` |
| scm-notify/api | `com.frog.notify.api` | `com.scmcloud.notify.api` |
| scm-tenant/service | `scm.tenant` | `com.scmcloud.tenant` |
| scm-tenant/api | `com.frog.tenant.api` | `com.scmcloud.tenant.api` |

### Files to Update Per Module

1. Java source: `package` declarations and `import` statements
2. Directory structure: move from `src/main/java/{oldPkgPath}/` to `src/main/java/{newPkgPath}/`
3. MyBatis XML mappers: `namespace` attribute
4. `application.yml` / `application.yaml`: `mybatis.type-aliases-package`, `mapper-locations`
5. Spring Boot main class: `@MapperScan`, `@ComponentScan`, `@SpringBootApplication` scanBasePackages
6. Dubbo configuration: `@DubboService.scanBasePackages`, `dubbo.scan.base-packages`
7. Any configuration classes referencing old packages

---

## 2. Dubbo Service Path Unification

### Current State

`scm-system` places Dubbo implementations in `rpc/` package instead of `service/dubbo/`.

### Target

```
Old: com.scmcloud.system.rpc.UserDubboServiceImpl
     com.scmcloud.system.rpc.PermissionDubboServiceImpl
     com.scmcloud.system.rpc.adapter.DubboPermissionServiceAdapter

New: com.scmcloud.system.service.dubbo.UserDubboServiceImpl
     com.scmcloud.system.service.dubbo.PermissionDubboServiceImpl
     com.scmcloud.system.service.dubbo.adapter.DubboPermissionServiceAdapter
```

### Changes

- Move 3 files to new package
- Update imports in all referencing files
- Update `@DubboService` annotation if package-scanned

---

## 3. DTO Location Standardization

### Convention

| Layer | Location | Naming |
|-------|----------|--------|
| API DTOs | `api/dto/` | `{Entity}VO` |
| API Requests | `api/request/` | `{Action}Request` |
| Internal DTOs | `service/dto/` | `{Entity}DTO` or descriptive name |

### Changes

| Module | Current | Target |
|--------|---------|--------|
| scm-warehouse | `service/vo/NearExpiryProductVO` | `service/dto/NearExpiryProductVO` |
| scm-supplier | `service/vo/OverdueApprovalTaskVO` | `service/dto/OverdueApprovalTaskVO` |
| scm-inventory | `service/domain/dto/*` | `service/dto/*` |

---

## 4. Entity ID Type Consistency

### Issue

`OrdOrder.id` is `String` (MyBatis Plus `IdType.ASSIGN_ID`) while `OrderVO.id` from the API module is `Long`.

### Fix

- Change `OrdOrder.id` from `String` to `Long`
- Verify `OrdOrderItem`, `OrdPayment`, `OrdRefund`, `OrdStatusHistory` for same `String id` issue
- Update all mappers and queries that reference the ID type
- Keep `IdType.ASSIGN_ID` strategy

### Impact Analysis

此变更影响链比 Risk 表中描述的更广，需要逐项确认：

```
OrdOrder.id (String → Long)
  ├── OrdOrderItem.orderId          外键，需同步修改
  ├── OrdPayment.orderId            外键，需同步修改
  ├── OrdRefund.orderId             外键，需同步修改
  ├── OrdStatusHistory.orderId      外键，需同步修改
  ├── Kafka 消息体中序列化的 order_id   消息格式变更，需确认消费方兼容性
  ├── scm-logistics 中引用 orderId 的 DTO
  ├── scm-warehouse 中引用 orderId 的 DTO
  └── 前端已存储或展示的订单号（书签/分享链接等）
```

**验证步骤：**

1. 全局搜索 `orderId` / `order_id` 字符串，梳理完整引用列表
2. 确认雪花算法产生的 ID 值处于 `Long` 范围内（ASSIGN_ID 默认使用雪花，数值安全）
3. **前端精度问题**：Java `Long` 最大值超过 JS `Number.MAX_SAFE_INTEGER`（2^53-1），前端必须将订单 ID 作为字符串处理；API 响应中 `Long` 类型 ID 字段需配置 Jackson `@JsonSerialize(using = ToStringSerializer.class)`
4. 若存在历史存量数据，需提供数据迁移脚本（当前文档未覆盖，需单独立项）

---

## 5. Entity Deduplication

### Duplicated Entities

| Entity | Owner Module | Remove From | Strategy |
|--------|-------------|-------------|----------|
| `SysAuditLog` | scm-audit | scm-common/web | Remove from common；消费方通过 scm-audit Dubbo API 访问 |
| `SysSensitiveOperationLog` | scm-audit | scm-system | Remove from system；system 调用 scm-audit Dubbo API |
| `SysPermissionApproval` | scm-approval | scm-system | Remove from system；system 调用 scm-approval Dubbo API |
| `SysNotificationTemplate` | scm-notify | scm-system | Remove from system；system 调用 scm-notify Dubbo API |
| `SysUserNotificationPreference` | scm-notify | scm-system | Remove from system；system 调用 scm-notify Dubbo API |

### Procedure Per Entity

1. 确认 Owner Module 已有完整 CRUD Service + Dubbo API
2. 若缺失，在 Owner Module 的 `api/` 补充所需 Dubbo 接口方法
3. 将消费模块中的直接引用替换为 Dubbo Client 调用
4. 从移除模块中删除实体类、Mapper、XML
5. 更新引用了已删除实体的配置文件

### SysAuditLog 写入策略说明

`SysAuditLog` 是高频写操作，审计记录在每次业务操作后触发。若直接改为**同步 Dubbo 调用**，存在以下问题：

- 审计写入阻塞业务主链路，增加 P99 延迟
- scm-audit 服务不可用时，会导致所有模块业务操作失败

**推荐方案：异步 Kafka 事件**

```
业务模块 → 发布 AuditLogEvent → Kafka Topic: scm.audit.log → scm-audit 消费写入
```

与本次重构的关系：
- 实体删除按原计划执行，不阻塞重构进度
- Dubbo 同步调用作为**过渡方案**，满足功能正确性
- Kafka 异步化作为后续独立优化项，建议在重构合并后新建 Issue 跟踪

---

## 6. CQRS Separation

### Pattern（应用于所有模块）

```
service/
├── query/
│   └── XxxQueryService          -- 只读方法，@Slave
├── command/
│   └── XxxCommandService        -- 写入方法，@Master
└── impl/
    └── XxxServiceImpl           -- 仅在需要跨 Query/Command 协调时保留
```

### Migration Per Module

对每个 `I{Entity}Service` 接口执行：

1. **识别读方法**：`get*`、`find*`、`query*`、`list*`、`page*`、`search*`
2. **识别写方法**：`create*`、`save*`、`update*`、`delete*`、`batch*`
3. **创建 `XxxQueryService`**：复制读方法签名 + 实现，加 `@Slave`
4. **创建 `XxxCommandService`**：复制写方法签名 + 实现，加 `@Master`
5. **更新 Controller**：读操作注入 `XxxQueryService`，写操作注入 `XxxCommandService`
6. **保留 `XxxServiceImpl`**：仅在 Query 和 Command 路径之间存在协调逻辑时保留

### Exceptions

- **简单 CRUD 模块**（scm-audit、scm-notify）：QueryService 和 CommandService 是对 Mapper 的薄封装
- **复杂模块**（scm-order、scm-inventory、scm-product）：可保留 `XxxServiceImpl` 处理事务协调，内部委托给 Query/Command Service

### Transaction Boundary Rules

CQRS 拆分后，事务边界需明确约定，避免因注入关系混乱导致事务失效：

| 场景 | 处理方式 |
|------|----------|
| 单模块写操作 | `@Transactional` 标注在 CommandService 方法上 |
| 写后立即读（同一请求内）| Controller 先调 CommandService 写入（事务提交），再调 QueryService 读取，两次独立事务；**禁止**在同一事务内读写混用 |
| 跨模块写操作 | `@GlobalTransactional` 标注在 CommandService 上；QueryService 上**禁止**添加全局事务 |
| CommandService 内部需要查询 | 允许 CommandService 注入 QueryService；**禁止** QueryService 反向注入 CommandService（防止循环依赖和事务污染）|
| 报表 / 复杂聚合查询 | QueryService 中走独立只读事务（`@Transactional(readOnly = true)`），不得混入写逻辑 |

---

## 7. Implementation Order

重构按以下顺序执行，保证每个阶段目标明确、风险可控：

| Phase | Description | Est. Days | Buildable After | Owner |
|-------|-------------|-----------|-----------------|-------|
| 1 | Package rename + directory move（全部模块） | 2d | No | — |
| 2 | Dubbo path unification | 0.5d | No | — |
| 3 | DTO location standardization | 1d | No | — |
| 4 | Entity ID type fix（含影响链全量排查）| 1.5d | No | — |
| 5 | Entity deduplication（5 个实体）| 2d | No | — |
| 6 | CQRS separation（15 个模块）| 5d | No | — |
| 7 | Fix all cross-module imports + config | 2d | **Yes** | — |
| 8 | Build & test（全量 + 回归）| 1d | Verify | — |
| **Total** | | **~15d** | | |

### Build Freeze Management

由于 Phase 1~6 期间项目处于不可构建状态，需执行以下管控措施：

**分支策略**
- 所有重构在 `refactor/layering-unification` 分支进行
- 主干（`master` / `main`）在重构期间保持可发布状态
- 业务需求按模块粒度在主干合并；重构 PR 合并后由指定人统一处理 cherry-pick 冲突

**WIP Checkpoint**
- Phase 1~3 完成后：提交 WIP commit，记录当前进度，供团队 Code Review 跟踪
- Phase 4~5 完成后：提交 WIP commit，重点 Review ID 类型变更影响链
- Phase 6 完成后：提交 WIP commit，Review CQRS 拆分结果
- Phase 7 完成后：执行 `mvn clean install -f com.scm.parent/pom.xml`，通过后发起 PR

**并行开发协议**
- 重构期间，其他开发人员**禁止**在涉及重构的模块中新增类或调整包结构
- 如有紧急业务需求落在同一模块，需与重构负责人协商后再动

---

## 8. Risk Assessment

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Package rename 破坏 Dubbo RPC | 服务间通信失败 | 高 | 更新所有 `@DubboReference` 扫描路径；Phase 7 重点验证 |
| Entity dedup 破坏跨模块查询 | 运行时异常 | 中 | 用 Dubbo API 调用替代直接 DB 访问；删除前确认 Owner Module API 完整 |
| CQRS 拆分引入事务 Bug | 数据不一致 | 中 | 严格遵守 Section 6 事务边界规则；`@GlobalTransactional` 只加在 CommandService |
| `String → Long` ID 破坏存量数据 | 数据完整性问题 | 低（新项目）/ 高（有存量）| 确认无存量数据；有存量时需迁移脚本；前端 ID 序列化改为字符串传输 |
| Kafka 消息体 orderId 类型变更 | 消费方反序列化失败 | 中 | 消息 Schema 同步更新；消费方灰度升级或增加类型兼容处理 |
| Build Freeze 阻塞并行开发 | 开发效率下降 | 高 | 在独立分支操作；冻结期协议通知全团队；约定 WIP Checkpoint |
| SysAuditLog 同步 Dubbo 调用影响性能 | 业务 P99 延迟上升 | 中 | 过渡期接受；后续 Kafka 异步化独立跟踪（Issue 待建） |

---

## Appendix: Checklist Per Module

每个模块完成重构后，由负责人逐项确认：

- [ ] `package` 声明已更新为 `com.scmcloud.{module}`
- [ ] 所有 `import` 已更新，无遗留 `com.frog.*` 或 `scm.*` 引用
- [ ] MyBatis XML `namespace` 已更新
- [ ] `application.yml` 中 `type-aliases-package`、`mapper-locations` 已更新
- [ ] Spring Boot 主类 `@MapperScan`、`@ComponentScan` 已更新
- [ ] Dubbo `scan.base-packages` 已更新
- [ ] DTO 位置符合 Section 3 约定
- [ ] CQRS 拆分完成，Controller 注入已切换
- [ ] 事务边界符合 Section 6 规则
- [ ] `mvn clean compile` 单模块通过（Phase 7 后）
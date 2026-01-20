# SCM Platform 代码规范化方案

## 📋 问题概述

通过对比scm-system模块（标准参考模块）与其他业务模块，发现以下不规范问题：

## 🔍 发现的问题

### 1. 包名不一致

**scm-system（标准）**: `com.frog.system`

**不规范的模块**:
- ✅ `scm-inventory`: `com.frog.inventory` （符合规范）
- ❌ `scm-product`: `scm.product` （不符合规范）
- ❌ `scm-order`: `scm.order` （不符合规范）
- ❌ `scm-warehouse`: 需检查
- ❌ `scm-logistics`: `scm.logistics` （不符合规范）
- ❌ `scm-supplier`: 需检查
- ❌ `scm-purchase`: `scm.purchase` （不符合规范）
- ❌ `scm-finance`: `scm.finance` （不符合规范）
- ❌ `scm-approval`: `scm.approval` （不符合规范）
- ❌ `scm-audit`: `scm.audit` （不符合规范）
- ❌ `scm-notify`: `scm.notify` （不符合规范）
- ❌ `scm-tenant`: `scm.tenant` （不符合规范）

**建议**: 统一使用 `com.frog.{module}` 格式

### 2. Controller层代码不规范

#### scm-system标准规范:
```java
@RestController
@RequestMapping("/api/system/users")
@RequiredArgsConstructor  // ✅ Lombok构造器注入
@Tag(name = "用户模块")     // ✅ Swagger文档
public class SysUserController {
    private final ISysUserService userService;  // ✅ final字段

    @GetMapping
    @PreAuthorize("hasAuthority('system:user:list')")  // ✅ 权限控制
    @Operation(summary = "查询用户列表")                // ✅ Swagger
    @AuditLog(operation = "查询用户", businessType = "USER")  // ✅ 审计日志
    public ApiResponse<PageResult<UserDTO>> list(...) {  // ✅ 统一返回类型
        // ...
        return ApiResponse.success(data);
    }
}
```

#### scm-product不规范示例:
```java
@RestController
@RequestMapping("/prod-brand")  // ❌ URL不规范（应该是/api/v1/brands）
public class ProdBrandController {
    // ❌ 空实现，没有任何业务方法
}
```

#### scm-inventory部分规范示例:
```java
@RestController
@RequestMapping("/api/v1/inventory")  // ✅ URL规范
@Tag(name = "库存管理")                 // ✅ Swagger
public class InvInventoryController {
    @Autowired  // ⚠️  应该使用@RequiredArgsConstructor
    private IInvInventoryService inventoryService;

    @GetMapping
    @Operation(summary = "查询库存")  // ✅ Swagger
    public InventoryResponse getInventory(...) {  // ❌ 应该返回ApiResponse<T>
        // ❌ 缺少@PreAuthorize权限控制
        // ❌ 缺少@AuditLog审计日志
    }
}
```

### 3. Service层规范

**scm-system标准**:
```java
public interface ISysUserService {
    Page<UserDTO> listUsers(Integer page, Integer size, String username, Integer status);
    UserDTO getUserById(UUID id);
    void addUser(UserDTO userDTO);
    void updateUser(UserDTO userDTO);
    void deleteUser(UUID id);
}

@Service
@RequiredArgsConstructor
@DS("user")  // ✅ 多租户数据源路由
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser>
    implements ISysUserService {

    private final SysUserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addUser(UserDTO userDTO) {
        // 完整的业务逻辑实现
    }
}
```

**其他模块问题**:
- ❌ 部分Service接口和实现类为空或只有简单的CRUD
- ❌ 缺少@Transactional事务管理
- ❌ 缺少@DS多租户数据源路由
- ❌ 缺少完整的业务逻辑实现

### 4. Entity层规范

**scm-system标准**:
```java
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_user")
public class SysUser implements Serializable {

    @TableId(type = IdType.ASSIGN_UUID)
    private UUID id;

    @TableField("username")
    private String username;

    @TableField("tenant_id")  // ✅ 多租户字段
    private String tenantId;

    @TableField(fill = FieldFill.INSERT)  // ✅ 自动填充
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField("created_by")
    private UUID createdBy;

    @TableField("updated_by")
    private UUID updatedBy;

    @TableLogic  // ✅ 逻辑删除
    @TableField("deleted")
    private Boolean deleted;
}
```

**其他模块问题**:
- ⚠️  部分Entity缺少tenant_id字段
- ⚠️  部分Entity缺少审计字段（created_at, updated_at, created_by, updated_by）
- ⚠️  部分Entity缺少@TableLogic逻辑删除

### 5. 统一返回类型

**scm-system标准**: 所有Controller方法统一返回 `ApiResponse<T>`

```java
// 成功返回数据
return ApiResponse.success(data);

// 成功无数据
return ApiResponse.success();

// 分页返回
return ApiResponse.success(PageResult.of(page));
```

**其他模块问题**: 直接返回业务对象，没有统一包装

### 6. API路径规范

**scm-system标准**:
- `/api/{module}/{resource}` 格式
- 例如: `/api/system/users`, `/api/system/roles`

**其他模块问题**:
- ❌ `/prod-brand` （不规范）
- ✅ `/api/v1/inventory` （规范）
- 需统一为: `/api/v1/{module}/{resource}` 或 `/api/{module}/{resource}`

---

## 🎯 规范化方案

### 阶段一：包名统一（优先级：高）

#### 影响范围：
- Java源代码包路径
- import语句
- 配置文件中的包扫描路径
- MyBatis Mapper XML namespace

#### 重构步骤：
1. 使用IDE重构功能批量重命名包名
2. 更新application.yml中的mybatis-plus配置
3. 更新所有import语句
4. 验证编译通过

#### 受影响模块列表：
```
scm-product: scm.product → com.frog.product
scm-order: scm.order → com.frog.order
scm-warehouse: scm.warehouse → com.frog.warehouse
scm-logistics: scm.logistics → com.frog.logistics
scm-supplier: scm.supplier → com.frog.supplier
scm-purchase: scm.purchase → com.frog.purchase
scm-finance: scm.finance → com.frog.finance
scm-approval: scm.approval → com.frog.approval
scm-audit: scm.audit → com.frog.audit
scm-notify: scm.notify → com.frog.notify
scm-tenant: scm.tenant → com.frog.tenant
```

### 阶段二：Controller层规范化（优先级：高）

#### 规范清单：
- [ ] 使用`@RequiredArgsConstructor` + `private final`依赖注入
- [ ] 统一返回`ApiResponse<T>`类型
- [ ] 添加`@PreAuthorize`权限控制
- [ ] 添加`@AuditLog`审计日志（敏感操作）
- [ ] 补充完整的CRUD方法
- [ ] 统一URL格式：`/api/v1/{module}/{resource}`
- [ ] 添加完整的Swagger注解（@Tag, @Operation, @Parameter）
- [ ] 添加参数校验（@Validated, @Valid）

#### 模板参考：
```java
@RestController
@RequestMapping("/api/v1/{module}/{resource}")
@RequiredArgsConstructor
@Tag(name = "{资源名称}")
public class {Resource}Controller {

    private final I{Resource}Service {resource}Service;

    @GetMapping
    @PreAuthorize("hasAuthority('{module}:{resource}:list')")
    @Operation(summary = "查询{资源}列表")
    public ApiResponse<PageResult<{Resource}DTO>> list(
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "10") Integer size) {
        // 实现
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('{module}:{resource}:list')")
    @Operation(summary = "查询{资源}详情")
    public ApiResponse<{Resource}DTO> getById(@PathVariable Long id) {
        // 实现
    }

    @PostMapping
    @PreAuthorize("hasAuthority('{module}:{resource}:add')")
    @AuditLog(operation = "新增{资源}", businessType = "{RESOURCE}")
    @Operation(summary = "新增{资源}")
    public ApiResponse<Void> add(@Validated @RequestBody {Resource}DTO dto) {
        // 实现
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('{module}:{resource}:edit')")
    @AuditLog(operation = "修改{资源}", businessType = "{RESOURCE}")
    @Operation(summary = "修改{资源}")
    public ApiResponse<Void> update(
        @PathVariable Long id,
        @Validated @RequestBody {Resource}DTO dto) {
        // 实现
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('{module}:{resource}:delete')")
    @AuditLog(operation = "删除{资源}", businessType = "{RESOURCE}", riskLevel = 4)
    @Operation(summary = "删除{资源}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        // 实现
    }
}
```

### 阶段三：Service层规范化（优先级：中）

#### 规范清单：
- [ ] 补充完整的业务逻辑实现
- [ ] 添加`@Transactional`事务管理
- [ ] 添加`@DS`多租户数据源路由
- [ ] 完善异常处理
- [ ] 添加日志记录
- [ ] 补充业务校验逻辑

#### 模板参考：
```java
@Service
@RequiredArgsConstructor
@Slf4j
@DS("{datasource}")  // 例如: "product", "order", "inventory"
public class {Resource}ServiceImpl extends ServiceImpl<{Resource}Mapper, {Resource}>
    implements I{Resource}Service {

    private final {Resource}Mapper {resource}Mapper;
    // 其他依赖...

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add{Resource}({Resource}DTO dto) {
        log.info("新增{资源}: {}", dto);

        // 1. 参数校验
        validate{Resource}(dto);

        // 2. 业务逻辑
        {Resource} entity = convert(dto);
        {resource}Mapper.insert(entity);

        // 3. 后续操作（如发送事件、更新缓存等）

        log.info("新增{资源}成功: id={}", entity.getId());
    }

    private void validate{Resource}({Resource}DTO dto) {
        // 业务校验逻辑
    }
}
```

### 阶段四：Entity层规范化（优先级：中）

#### 规范清单：
- [ ] 统一使用UUID作为主键
- [ ] 添加tenant_id多租户字段
- [ ] 添加审计字段（created_at, updated_at, created_by, updated_by）
- [ ] 添加逻辑删除字段（deleted）
- [ ] 使用@TableField自动填充

#### 模板参考：
```java
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("{table_name}")
public class {Entity} implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_UUID)
    private UUID id;

    // 业务字段...

    @TableField("tenant_id")
    private String tenantId;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField("created_by")
    private UUID createdBy;

    @TableField("updated_by")
    private UUID updatedBy;

    @TableLogic
    @TableField("deleted")
    private Boolean deleted;
}
```

### 阶段五：补充缺失的业务功能（优先级：低）

根据各模块的业务特性，补充完整的CRUD以外的业务方法。

---

## 📝 执行计划

### Week 1: 包名统一
- [ ] scm-product, scm-order, scm-warehouse 包名重构
- [ ] scm-logistics, scm-supplier, scm-purchase 包名重构
- [ ] scm-finance, scm-approval, scm-audit 包名重构
- [ ] scm-notify, scm-tenant 包名重构
- [ ] 全量编译测试

### Week 2-3: Controller层规范化
- [ ] scm-product Controller完善
- [ ] scm-order Controller完善
- [ ] scm-warehouse Controller完善
- [ ] scm-logistics Controller完善
- [ ] scm-supplier Controller完善
- [ ] scm-purchase Controller完善
- [ ] scm-finance Controller完善

### Week 4: Service层规范化
- [ ] 补充完整的Service实现
- [ ] 添加事务管理
- [ ] 添加多租户数据源路由
- [ ] 完善业务逻辑

### Week 5: Entity层规范化
- [ ] 统一Entity字段
- [ ] 更新数据库表结构（如需要）
- [ ] 数据迁移脚本（如需要）

### Week 6: 测试与验证
- [ ] 单元测试
- [ ] 集成测试
- [ ] API测试
- [ ] 性能测试

---

## ⚠️  注意事项

1. **数据库兼容性**: 包名修改不影响数据库，但需要验证MyBatis Mapper映射
2. **向后兼容**: 如果有外部系统调用，需要考虑API路径变更的影响
3. **分支管理**: 建议创建feature分支进行重构，避免影响主分支
4. **渐进式重构**: 一次只重构一个模块，验证通过后再继续下一个
5. **文档更新**: 同步更新API文档、开发文档

---

## 🔧 工具推荐

1. **IntelliJ IDEA Refactor**: 使用IDE的重构功能批量重命名包名
2. **批量替换工具**: 用于更新配置文件中的包路径
3. **Maven编译验证**: `mvn clean install -DskipTests`
4. **代码格式化**: `mvn spotless:apply`（如果配置了）

---

## 📊 规范化收益

1. **代码一致性**: 统一的代码风格，降低维护成本
2. **可读性提升**: 清晰的包结构，易于理解项目架构
3. **安全性增强**: 统一的权限控制和审计日志
4. **多租户支持**: 完整的多租户数据隔离
5. **开发效率**: 统一的模板和规范，减少重复工作

---

**文档版本**: v1.0
**创建时间**: 2025-12-26
**维护者**: SCM Platform Team
# Spring Cloud LoadBalancer 负载均衡配置指南

## 概述

本项目使用 **Spring Cloud LoadBalancer** 替代 Ribbon,为 RestClient + @HttpExchange 提供客户端负载均衡能力。支持多种负载均衡策略,可根据不同场景灵活配置。

## 支持的负载均衡策略

### 1. Round Robin (轮询) - 默认策略

**特点**:
- 依次选择服务实例,循环往复
- 适用于实例性能相近的场景
- 请求分配均匀,实现简单

**配置**:
```yaml
spring:
  cloud:
    loadbalancer:
      strategy: round-robin  # 默认值,可省略
```

**示例场景**:
- 所有服务实例配置相同(CPU、内存、网络)
- 无状态服务,任何实例都能处理请求

### 2. Random (随机)

**特点**:
- 随机选择服务实例
- 适用于快速分散请求,避免热点
- 长期看请求分配趋于均匀

**配置**:
```yaml
spring:
  cloud:
    loadbalancer:
      strategy: random
```

**示例场景**:
- 需要快速分散流量的高并发场景
- 测试环境,验证所有实例是否正常工作

### 3. Weighted Round Robin (加权轮询)

**特点**:
- 根据实例权重分配请求
- 适用于实例性能差异较大的场景
- 支持动态调整权重(通过 Nacos 控制台)

**配置**:
```yaml
spring:
  cloud:
    loadbalancer:
      strategy: weighted-round-robin
      nacos:
        enabled: true  # 启用 Nacos 权重支持
```

**Nacos 实例权重配置**:

1. 打开 Nacos 控制台: http://localhost:8848/nacos
2. 进入 "服务管理" → "服务列表"
3. 点击服务详情,编辑实例
4. 在 "元数据" 中添加:
   ```json
   {
     "weight": 80
   }
   ```
5. 权重范围: 0-100
   - 100 (默认): 正常权重
   - 50: 减半流量(适用于性能较弱的实例)
   - 0: 不接收流量(临时下线或预热中)

**示例场景**:
- 新旧实例混部,新实例权重设为 50 进行灰度
- 实例配置不同(8C16G 权重 100,4C8G 权重 50)
- 临时下线实例(权重设为 0,不中断服务)

## 配置文件结构

### 完整配置示例

`scm-common/web/src/main/resources/application-rest-client.yml`:

```yaml
spring:
  cloud:
    loadbalancer:
      # 负载均衡策略
      strategy: round-robin

      # Nacos 权重支持(仅 weighted-round-robin 生效)
      nacos:
        enabled: true

      # 健康检查配置
      health-check:
        path:
          enabled: true  # 过滤不健康的实例

      # 实例列表缓存
      cache:
        enabled: true
        ttl: 35  # 缓存 35 秒,减少 Nacos 查询
        capacity: 256

      # 重试配置
      retry:
        enabled: true
        max-retries-on-same-service-instance: 0  # 同实例不重试
        max-retries-on-next-service-instance: 1  # 换实例重试 1 次

# RestClient 配置
rest:
  client:
    connect-timeout: 5000  # 连接超时 5 秒
    read-timeout: 30000    # 读取超时 30 秒
```

### 环境差异化配置

**开发环境** (application-rest-client.yml):
```yaml
spring.config.activate.on-profile: dev

spring.cloud.loadbalancer:
  strategy: round-robin  # 开发环境使用简单轮询
```

**生产环境** (application-rest-client.yml):
```yaml
spring.config.activate.on-profile: prod

spring.cloud.loadbalancer:
  strategy: weighted-round-robin  # 生产环境使用加权轮询
  nacos.enabled: true
```

## 使用方式

### 1. 自动负载均衡

所有通过 `@HttpExchange` 定义的客户端都会**自动使用负载均衡**,无需额外配置:

```java
@HttpExchange("/api/system/users")
public interface SysUserServiceClient {
    @GetExchange("/{userId}")
    ApiResponse<SysUser> getUser(@PathVariable UUID userId);
}
```

调用 `sysUserServiceClient.getUser(userId)` 时:
1. LoadBalancer 从 Nacos 获取 `user-service` 实例列表
2. 根据配置的策略(Round Robin/Random/Weighted)选择实例
3. RestClient 自动将请求发送到选中的实例

### 2. 查看负载均衡日志

启用 DEBUG 日志查看实例选择过程:

```yaml
logging:
  level:
    com.frog.common.rest.config.RestClientHttpExchangeConfig: DEBUG
    org.springframework.cloud.loadbalancer: DEBUG
```

日志示例:
```
2025-12-29 10:30:15.123 DEBUG - Resolved service 'user-service' to URL: https://192.168.1.100:8081 (host: 192.168.1.100, metadata: {weight=100})
2025-12-29 10:30:16.456 DEBUG - Resolved service 'user-service' to URL: https://192.168.1.101:8081 (host: 192.168.1.101, metadata: {weight=80})
```

## 高级场景

### 场景1: 灰度发布

**需求**: 新版本服务部署到 2 台机器,旧版本 8 台机器,逐步切流量

**配置步骤**:
1. 设置负载均衡策略为 `weighted-round-robin`
2. 在 Nacos 控制台设置实例权重:
   - 旧版本实例: weight=100 (共 8 台,总权重 800)
   - 新版本实例: weight=10 (共 2 台,总权重 20)
3. 观察新版本稳定性,逐步调整权重:
   - 第 1 天: 新版本 weight=20 (约 4% 流量)
   - 第 2 天: 新版本 weight=50 (约 11% 流量)
   - 第 3 天: 新版本 weight=100 (约 20% 流量)
4. 新版本稳定后,下线旧版本实例

### 场景2: 临时下线实例维护

**需求**: 需要临时下线某台机器进行维护,不中断服务

**操作步骤**:
1. 在 Nacos 控制台将实例权重设为 0
2. 等待 35 秒(缓存 TTL),新请求不再路由到该实例
3. 查看监控,确认无活跃请求后进行维护
4. 维护完成后,将权重恢复为 100

**避免直接下线**: 直接从 Nacos 注销实例会导致正在处理的请求失败

### 场景3: 多机房部署

**需求**: 服务部署在 A/B 两个机房,优先访问同机房实例

**配置思路**:
1. 在 Nacos 实例 metadata 添加机房标识:
   ```json
   {
     "zone": "zone-a",
     "weight": 100
   }
   ```
2. 自定义 `ServiceInstanceListSupplier`,过滤同机房实例
3. 同机房无可用实例时,降级访问其他机房

**代码示例** (需自定义 LoadBalancer 配置):
```java
@Bean
public ServiceInstanceListSupplier zonePreferenceServiceInstanceListSupplier(
        ConfigurableApplicationContext context) {
    return ServiceInstanceListSupplier.builder()
        .withDiscoveryClient()
        .withZonePreference()  // 优先同 zone 实例
        .withCaching()
        .build(context);
}
```

## 监控与调试

### 1. Actuator 端点

查看 LoadBalancer 状态:
```bash
curl http://localhost:8761/actuator/health
```

### 2. Sentinel Dashboard

查看客户端调用的 QPS、RT、错误率:
- 访问: http://localhost:8080
- 登录: sentinel/sentinel
- 查看 "簇点链路" → 搜索 `user-service:getUser`

### 3. 常见问题排查

**问题1: 请求总是路由到同一个实例**
- 检查负载均衡策略是否正确配置
- 检查是否启用了缓存,缓存 TTL 是否过长
- 查看日志确认 LoadBalancer 是否被正确调用

**问题2: 实例权重不生效**
- 确认策略为 `weighted-round-robin`
- 确认 `spring.cloud.loadbalancer.nacos.enabled=true`
- 检查 Nacos 实例 metadata 是否正确设置

**问题3: 健康检查失败导致无可用实例**
- 检查服务实例是否真的不健康
- 调整健康检查配置或临时禁用: `spring.cloud.loadbalancer.health-check.path.enabled=false`

## 性能优化建议

1. **启用实例列表缓存**: 减少对 Nacos 的查询频率,降低延迟
   ```yaml
   spring.cloud.loadbalancer.cache:
     enabled: true
     ttl: 35
   ```

2. **合理配置超时时间**: 避免慢实例拖垮整体性能
   ```yaml
   rest.client:
     connect-timeout: 3000  # 生产环境建议 3 秒
     read-timeout: 15000    # 根据业务接口特点调整
   ```

3. **启用重试机制**: 提高容错能力
   ```yaml
   spring.cloud.loadbalancer.retry:
     enabled: true
     max-retries-on-next-service-instance: 1
   ```

4. **监控实例响应时间**: 根据监控数据动态调整实例权重

## 参考资料

- [Spring Cloud LoadBalancer 官方文档](https://docs.spring.io/spring-cloud-commons/docs/current/reference/html/#spring-cloud-loadbalancer)
- [Nacos 服务发现最佳实践](https://nacos.io/zh-cn/docs/quick-start-spring-cloud.html)
- [项目 CLAUDE.md](../../CLAUDE.md) - 项目架构说明

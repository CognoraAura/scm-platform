# DevOps and Testing - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Set up CI/CD pipeline with GitHub Actions, Kubernetes deployment manifests, Helm charts, and integration testing infrastructure.

**Architecture:** GitHub Actions for CI/CD. Kubernetes with Helm for deployment. Testcontainers for integration testing.

**Tech Stack:** GitHub Actions, Docker, Kubernetes, Helm 3, Testcontainers, JUnit 5

---

## File Structure

```
.github/
  workflows/
    ci.yml                             ← CREATE - CI pipeline
    cd.yml                             ← CREATE - CD pipeline

k8s/
  base/
    namespace.yaml                     ← CREATE
    configmap.yaml                     ← CREATE
    secrets.yaml                       ← CREATE
  services/
    scm-auth/
      deployment.yaml                  ← CREATE
      service.yaml                     ← CREATE
      ingress.yaml                     ← CREATE
    scm-order/
      deployment.yaml                  ← CREATE
      service.yaml                     ← CREATE
    scm-inventory/
      deployment.yaml                  ← CREATE
      service.yaml                     ← CREATE
    scm-gateway/
      deployment.yaml                  ← CREATE
      service.yaml                     ← CREATE

helm/
  scm-platform/
    Chart.yaml                         ← CREATE
    values.yaml                        ← CREATE
    values-dev.yaml                    ← CREATE
    values-staging.yaml                ← CREATE
    values-prod.yaml                   ← CREATE
    templates/
      _helpers.tpl                     ← CREATE
      deployment.yaml                  ← CREATE
      service.yaml                     ← CREATE
      ingress.yaml                     ← CREATE
      configmap.yaml                   ← CREATE
      secrets.yaml                     ← CREATE
    charts/
      scm-auth/
        Chart.yaml                     ← CREATE
        values.yaml                    ← CREATE
      scm-order/
        Chart.yaml                     ← CREATE
        values.yaml                    ← CREATE

scm-common/
  test/
    src/main/java/com/scmcloud/common/test/
      BaseIntegrationTest.java         ← CREATE - Testcontainers base
      PostgresContainer.java           ← CREATE
      RedisContainer.java              ← CREATE
      KafkaContainer.java              ← CREATE
```

---

### Task 1: Create GitHub Actions CI Pipeline

**Files:**
- Create: `.github/workflows/ci.yml`

- [ ] **Step 1: Create CI workflow**

```yaml
name: CI Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_USER: admin
          POSTGRES_PASSWORD: admin123
          POSTGRES_DB: scm_test
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
      
      redis:
        image: redis:7
        ports:
          - 6379:6379
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
      
      kafka:
        image: confluentinc/cp-kafka:7.5.0
        ports:
          - 9092:9092
        env:
          KAFKA_BROKER_ID: 1
          KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
          KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
          KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'
        cache: maven
    
    - name: Build with Maven
      run: mvn clean install -f com.scm.parent/pom.xml
    
    - name: Run tests
      run: mvn test -f com.scm.parent/pom.xml
      env:
        SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/scm_test
        SPRING_DATASOURCE_USERNAME: admin
        SPRING_DATASOURCE_PASSWORD: admin123
        SPRING_REDIS_HOST: localhost
        SPRING_REDIS_PORT: 6379
        SPRING_KAFKA_BOOTSTRAP_SERVERS: localhost:9092
    
    - name: Run JaCoCo
      run: mvn jacoco:report -f com.scm.parent/pom.xml
    
    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v4
      with:
        files: com.scm.parent/**/target/site/jacoco/jacoco.xml
    
    - name: Build Docker images
      run: |
        for service in scm-auth scm-order scm-inventory scm-gateway; do
          docker build -t scm-platform/${service}:${{ github.sha }} ${service}/
        done
    
    - name: Upload Docker images
      uses: actions/upload-artifact@v4
      with:
        name: docker-images
        path: |
          scm-auth/target/*.jar
          scm-order/target/*.jar
```

- [ ] **Step 2: Commit**

```bash
git add .github/workflows/ci.yml
git commit -m "ci: add GitHub Actions CI pipeline"
```

---

### Task 2: Create GitHub Actions CD Pipeline

**Files:**
- Create: `.github/workflows/cd.yml`

- [ ] **Step 1: Create CD workflow**

```yaml
name: CD Pipeline

on:
  push:
    branches: [ main ]
    tags: [ 'v*' ]

jobs:
  deploy-staging:
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'
        cache: maven
    
    - name: Build with Maven
      run: mvn clean package -DskipTests -f com.scm.parent/pom.xml
    
    - name: Login to Docker Registry
      uses: docker/login-action@v3
      with:
        registry: ${{ secrets.DOCKER_REGISTRY }}
        username: ${{ secrets.DOCKER_USERNAME }}
        password: ${{ secrets.DOCKER_PASSWORD }}
    
    - name: Build and push Docker images
      run: |
        for service in scm-auth scm-order scm-inventory scm-gateway; do
          docker build -t ${{ secrets.DOCKER_REGISTRY }}/scm-platform/${service}:${{ github.sha }} ${service}/
          docker push ${{ secrets.DOCKER_REGISTRY }}/scm-platform/${service}:${{ github.sha }}
        done
    
    - name: Deploy to staging
      run: |
        helm upgrade --install scm-platform ./helm/scm-platform \
          --namespace staging \
          --values ./helm/scm-platform/values-staging.yaml \
          --set image.tag=${{ github.sha }}
  
  deploy-production:
    runs-on: ubuntu-latest
    if: startsWith(github.ref, 'refs/tags/v')
    needs: deploy-staging
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Deploy to production
      run: |
        helm upgrade --install scm-platform ./helm/scm-platform \
          --namespace production \
          --values ./helm/scm-platform/values-prod.yaml \
          --set image.tag=${{ github.ref_name }}
```

- [ ] **Step 2: Commit**

```bash
git add .github/workflows/cd.yml
git commit -m "ci: add GitHub Actions CD pipeline with Helm"
```

---

### Task 3: Create Kubernetes Manifests

**Files:**
- Create: `k8s/base/namespace.yaml`
- Create: `k8s/services/scm-auth/deployment.yaml`
- Create: `k8s/services/scm-auth/service.yaml`

- [ ] **Step 1: Create namespace**

```yaml
# k8s/base/namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: scm-platform
  labels:
    app.kubernetes.io/name: scm-platform
```

- [ ] **Step 2: Create auth deployment**

```yaml
# k8s/services/scm-auth/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: scm-auth
  namespace: scm-platform
  labels:
    app.kubernetes.io/name: scm-auth
spec:
  replicas: 2
  selector:
    matchLabels:
      app.kubernetes.io/name: scm-auth
  template:
    metadata:
      labels:
        app.kubernetes.io/name: scm-auth
    spec:
      containers:
      - name: scm-auth
        image: scm-platform/scm-auth:latest
        ports:
        - containerPort: 8106
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: NACOS_SERVER_ADDR
          valueFrom:
            configMapKeyRef:
              name: scm-config
              key: nacos-server-addr
        - name: DB_HOST
          valueFrom:
            secretKeyRef:
              name: scm-secrets
              key: db-host
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: scm-secrets
              key: db-password
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8106
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8106
          initialDelaySeconds: 30
          periodSeconds: 5
```

- [ ] **Step 3: Create auth service**

```yaml
# k8s/services/scm-auth/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: scm-auth
  namespace: scm-platform
spec:
  selector:
    app.kubernetes.io/name: scm-auth
  ports:
  - port: 8106
    targetPort: 8106
  type: ClusterIP
```

- [ ] **Step 4: Commit**

```bash
git add k8s/
git commit -m "k8s: add Kubernetes deployment manifests"
```

---

### Task 4: Create Helm Chart

**Files:**
- Create: `helm/scm-platform/Chart.yaml`
- Create: `helm/scm-platform/values.yaml`
- Create: `helm/scm-platform/values-staging.yaml`
- Create: `helm/scm-platform/values-prod.yaml`

- [ ] **Step 1: Create Chart.yaml**

```yaml
# helm/scm-platform/Chart.yaml
apiVersion: v2
name: scm-platform
description: SCM Platform Helm Chart
type: application
version: 1.0.0
appVersion: "1.0.0"
dependencies:
  - name: postgresql
    version: 13.2.24
    repository: https://charts.bitnami.com/bitnami
    condition: postgresql.enabled
  - name: redis
    version: 18.6.1
    repository: https://charts.bitnami.com/bitnami
    condition: redis.enabled
  - name: kafka
    version: 26.8.5
    repository: https://charts.bitnami.com/bitnami
    condition: kafka.enabled
```

- [ ] **Step 2: Create values.yaml**

```yaml
# helm/scm-platform/values.yaml
global:
  imageRegistry: ""
  imagePullSecrets: []
  storageClass: ""

# Service configurations
services:
  scm-auth:
    enabled: true
    replicaCount: 2
    image:
      repository: scm-platform/scm-auth
      tag: latest
      pullPolicy: IfNotPresent
    service:
      type: ClusterIP
      port: 8106
    resources:
      requests:
        memory: "512Mi"
        cpu: "250m"
      limits:
        memory: "1Gi"
        cpu: "500m"
  
  scm-order:
    enabled: true
    replicaCount: 2
    image:
      repository: scm-platform/scm-order
      tag: latest
    service:
      port: 8201
  
  scm-inventory:
    enabled: true
    replicaCount: 2
    image:
      repository: scm-platform/scm-inventory
      tag: latest
    service:
      port: 8202
  
  scm-gateway:
    enabled: true
    replicaCount: 2
    image:
      repository: scm-platform/scm-gateway
      tag: latest
    service:
      port: 8761

# PostgreSQL
postgresql:
  enabled: true
  auth:
    postgresPassword: admin123
    database: scm_platform
  primary:
    persistence:
      size: 10Gi

# Redis
redis:
  enabled: true
  auth:
    enabled: true
    password: admin123
  master:
    persistence:
      size: 2Gi

# Kafka
kafka:
  enabled: true
  persistence:
    size: 5Gi
```

- [ ] **Step 3: Create values-staging.yaml**

```yaml
# helm/scm-platform/values-staging.yaml
global:
  environment: staging

services:
  scm-auth:
    replicaCount: 1
    resources:
      requests:
        memory: "256Mi"
        cpu: "100m"
      limits:
        memory: "512Mi"
        cpu: "250m"

postgresql:
  primary:
    persistence:
      size: 5Gi

redis:
  master:
    persistence:
      size: 1Gi
```

- [ ] **Step 4: Create values-prod.yaml**

```yaml
# helm/scm-platform/values-prod.yaml
global:
  environment: production

services:
  scm-auth:
    replicaCount: 3
    resources:
      requests:
        memory: "1Gi"
        cpu: "500m"
      limits:
        memory: "2Gi"
        cpu: "1000m"

postgresql:
  primary:
    persistence:
      size: 50Gi
  readReplicas:
    replicaCount: 2

redis:
  master:
    persistence:
      size: 10Gi
  replica:
    replicaCount: 2

kafka:
  persistence:
    size: 20Gi
```

- [ ] **Step 5: Commit**

```bash
git add helm/
git commit -m "helm: add Helm chart for SCM Platform"
```

---

### Task 5: Create Testcontainers Base

**Files:**
- Create: `scm-common/test/src/main/java/com/scmcloud/common/test/BaseIntegrationTest.java`
- Create: `scm-common/test/src/main/java/com/scmcloud/common/test/PostgresContainer.java`
- Create: `scm-common/test/src/main/java/com/scmcloud/common/test/RedisContainer.java`

- [ ] **Step 1: Create PostgresContainer**

```java
package com.scmcloud.common.test;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class PostgresContainer extends PostgreSQLContainer<PostgresContainer> {

    private static final String IMAGE_VERSION = "postgres:16";
    private static PostgresContainer container;

    private PostgresContainer() {
        super(DockerImageName.parse(IMAGE_VERSION));
    }

    public static PostgresContainer getInstance() {
        if (container == null) {
            container = new PostgresContainer()
                    .withDatabaseName("scm_test")
                    .withUsername("test")
                    .withPassword("test")
                    .withInitScript("db/init-test.sql");
        }
        return container;
    }

    @Override
    public void start() {
        super.start();
        System.setProperty("spring.datasource.url", container.getJdbcUrl());
        System.setProperty("spring.datasource.username", container.getUsername());
        System.setProperty("spring.datasource.password", container.getPassword());
    }

    @Override
    public void stop() {
        // Do not stop between tests
    }
}
```

- [ ] **Step 2: Create RedisContainer**

```java
package com.scmcloud.common.test;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class RedisContainer extends GenericContainer<RedisContainer> {

    private static final String IMAGE_VERSION = "redis:7";
    private static RedisContainer container;

    private RedisContainer() {
        super(DockerImageName.parse(IMAGE_VERSION));
        withExposedPorts(6379);
    }

    public static RedisContainer getInstance() {
        if (container == null) {
            container = new RedisContainer();
        }
        return container;
    }

    @Override
    public void start() {
        super.start();
        System.setProperty("spring.redis.host", container.getHost());
        System.setProperty("spring.redis.port", container.getFirstMappedPort().toString());
    }

    @Override
    public void stop() {
        // Do not stop between tests
    }
}
```

- [ ] **Step 3: Create BaseIntegrationTest**

```java
package com.scmcloud.common.test;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @BeforeAll
    static void startContainers() {
        PostgresContainer.getInstance().start();
        RedisContainer.getInstance().start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        PostgresContainer pg = PostgresContainer.getInstance();
        registry.add("spring.datasource.url", pg::getJdbcUrl);
        registry.add("spring.datasource.username", pg::getUsername);
        registry.add("spring.datasource.password", pg::getPassword);

        // Redis
        RedisContainer redis = RedisContainer.getInstance();
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getFirstMappedPort());
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add scm-common/test/
git commit -m "test: add Testcontainers base for integration tests"
```

---

### Task 6: Create Integration Test Examples

**Files:**
- Create: `scm-order/service/src/test/java/com/scmcloud/order/integration/OrderIntegrationTest.java`

- [ ] **Step 1: Create order integration test**

```java
package com.scmcloud.order.integration;

import com.scmcloud.common.test.BaseIntegrationTest;
import com.scmcloud.order.domain.entity.OrdOrder;
import com.scmcloud.order.domain.entity.OrderStatus;
import com.scmcloud.order.domain.repository.OrdOrderRepository;
import com.scmcloud.order.domain.CreateOrderContext;
import com.scmcloud.order.domain.OrderItemData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private OrdOrderRepository orderRepository;

    @Test
    @Transactional
    void shouldCreateAndPersistOrder() {
        // Given
        CreateOrderContext ctx = CreateOrderContext.builder()
                .userId("user-123")
                .username("testuser")
                .orderType(1)
                .orderSource("WEB")
                .items(List.of(new OrderItemData("sku-1", "Product 1", 2, new BigDecimal("100.00"))))
                .build();

        // When
        OrdOrder order = OrdOrder.create(ctx);
        OrdOrder saved = orderRepository.save(order);

        // Then
        assertNotNull(saved.getId());
        assertEquals(OrderStatus.PENDING_PAYMENT, saved.getStatusEnum());
        assertEquals(1, saved.getDomainEvents().size());
    }

    @Test
    @Transactional
    void shouldLoadOrderWithItems() {
        // Given
        CreateOrderContext ctx = CreateOrderContext.builder()
                .userId("user-123")
                .username("testuser")
                .orderType(1)
                .orderSource("WEB")
                .items(List.of(new OrderItemData("sku-1", "Product 1", 2, new BigDecimal("100.00"))))
                .build();

        OrdOrder order = OrdOrder.create(ctx);
        OrdOrder saved = orderRepository.save(order);

        // When
        Optional<OrdOrder> loaded = orderRepository.findById(saved.getId());

        // Then
        assertTrue(loaded.isPresent());
        assertEquals(saved.getId(), loaded.get().getId());
        assertNotNull(loaded.get().getItems());
        assertEquals(1, loaded.get().getItems().size());
    }

    @Test
    @Transactional
    void shouldTransitionOrderStatus() {
        // Given
        CreateOrderContext ctx = CreateOrderContext.builder()
                .userId("user-123")
                .username("testuser")
                .orderType(1)
                .orderSource("WEB")
                .items(List.of(new OrderItemData("sku-1", "Product 1", 2, new BigDecimal("100.00"))))
                .build();

        OrdOrder order = OrdOrder.create(ctx);
        OrdOrder saved = orderRepository.save(order);

        // When
        saved.pay(new BigDecimal("200.00"), "PAY-123");
        OrdOrder paid = orderRepository.save(saved);

        // Then
        assertEquals(OrderStatus.PAID, paid.getStatusEnum());
        assertEquals("PAY-123", paid.getPaymentNo());
    }
}
```

- [ ] **Step 2: Run the test**

```bash
mvn test -pl scm-order/service -Dtest=OrderIntegrationTest -f com.scm.parent/pom.xml
```

Expected: PASS (requires Docker)

- [ ] **Step 3: Commit**

```bash
git add scm-order/service/src/test/java/com/scmcloud/order/integration/OrderIntegrationTest.java
git commit -m "test: add order integration test with Testcontainers"
```

---

### Task 7: Build Verification

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

- [ ] **Step 3: Build Docker images**

```bash
for service in scm-auth scm-order scm-inventory scm-gateway; do
  docker build -t scm-platform/${service}:latest ${service}/
done
```

Expected: All images build successfully

- [ ] **Step 4: Verify Helm chart**

```bash
helm lint ./helm/scm-platform
```

Expected: Chart is valid

---

## Summary

| Task | Description | Status |
|------|-------------|--------|
| T060 | GitHub Actions CI pipeline | PENDING |
| T061 | GitHub Actions CD pipeline | PENDING |
| T082 | Kubernetes manifests | PENDING |
| T083 | Helm chart | PENDING |
| T092 | Testcontainers base | PENDING |
| T093 | Integration test examples | PENDING |
| T094 | Build verification | PENDING |

**Total Estimated Effort:** 5-7 days

**Priority:** P1 - Start in Week 17

**Dependencies:**
- All backend services must be complete
- Docker must be available for testing

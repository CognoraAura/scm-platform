# SCM Platform Architecture Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the architecture review recommendations across 4 phases (30/60/90/180 days) to achieve production-ready, scalable, secure SCM platform.

**Architecture:** Foundation stabilization (30d) → Operational maturity (60d) → Resilience & scale (90d) → Optimization & innovation (180d)

**Tech Stack:** Java 21, Spring Boot 4.0.6, PostgreSQL 16, Redis 7.2, Kafka, Kubernetes, ArgoCD, Flyway, Prometheus, Grafana

---

## Phase 1: Foundation (30 Days)

### Task 1: Implement Flyway Database Migration Tool

**Files:**
- Modify: `com.scm.parent/pom.xml`
- Create: `scripts/db/migration/V001__baseline.sql`
- Modify: `scm-order/service/src/main/resources/application.yml`
- Modify: `scm-inventory/service/src/main/resources/application.yml`
- Modify: All service `application.yml` files

- [ ] **Step 1: Add Flyway dependency to parent POM**

```xml
<!-- Add to com.scm.parent/pom.xml <properties> section -->
<flyway.version>10.15.0</flyway.version>

<!-- Add to com.scm.parent/pom.xml <dependencyManagement> section -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
    <version>${flyway.version}</version>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
    <version>${flyway.version}</version>
</dependency>
```

- [ ] **Step 2: Add Flyway to each service POM**

```xml
<!-- Add to scm-order/service/pom.xml, scm-inventory/service/pom.xml, etc. -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

- [ ] **Step 3: Create baseline migration script**

```sql
-- scripts/db/migration/V001__baseline.sql
-- This script documents the current schema state
-- Run: psql -h localhost -U admin -d db_order -f scripts/db/migration/V001__baseline.sql

-- Mark existing schema as baseline
-- Flyway will not execute this file, but will record it as applied
SELECT 1;
```

- [ ] **Step 4: Configure Flyway in application.yml**

```yaml
# Add to scm-order/service/src/main/resources/application.yml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: 0
    validate-on-migrate: true
    out-of-order: false
    table: flyway_schema_history
```

- [ ] **Step 5: Create migration directory structure**

```bash
# Create migration directories for each service
mkdir -p scm-order/service/src/main/resources/db/migration
mkdir -p scm-inventory/service/src/main/resources/db/migration
mkdir -p scm-product/service/src/main/resources/db/migration
mkdir -p scm-warehouse/service/src/main/resources/db/migration
mkdir -p scm-logistics/service/src/main/resources/db/migration
mkdir -p scm-supplier/service/src/main/resources/db/migration
mkdir -p scm-finance/service/src/main/resources/db/migration
mkdir -p scm-purchase/service/src/main/resources/db/migration
mkdir -p scm-system/service/src/main/resources/db/migration
mkdir -p scm-tenant/service/src/main/resources/db/migration
mkdir -p scm-approval/service/src/main/resources/db/migration
mkdir -p scm-audit/service/src/main/resources/db/migration
mkdir -p scm-notify/service/src/main/resources/db/migration
```

- [ ] **Step 6: Test Flyway migration**

```bash
# Start PostgreSQL
docker-compose up -d scm-postgres

# Run migration for order service
cd scm-order/service
mvn flyway:info -Dflyway.url=jdbc:postgresql://localhost:5432/db_order -Dflyway.user=admin -Dflyway.password=admin123

# Expected output: Shows baseline version and pending migrations
```

- [ ] **Step 7: Commit**

```bash
git add com.scm.parent/pom.xml scm-*/service/pom.xml scripts/db/migration/ scm-*/service/src/main/resources/db/migration/
git commit -m "feat: implement Flyway database migration tool"
```

---

### Task 2: Consolidate CI Workflows

**Files:**
- Modify: `.github/workflows/maven-build.yml`
- Delete: `.github/workflows/ci.yml`

- [ ] **Step 1: Read current ci.yml to identify unique features**

```bash
# Review ci.yml to identify features to merge into maven-build.yml
cat .github/workflows/ci.yml
```

- [ ] **Step 2: Update maven-build.yml with PostgreSQL/Redis services**

```yaml
# Add to .github/workflows/maven-build.yml build job
services:
  postgres:
    image: postgres:16-alpine
    env:
      POSTGRES_USER: admin
      POSTGRES_PASSWORD: admin123
      POSTGRES_DB: db_user
    ports:
      - 5432:5432
    options: >-
      --health-cmd pg_isready
      --health-interval 10s
      --health-timeout 5s
      --health-retries 5

  redis:
    image: redis:7.2-alpine
    ports:
      - 6379:6379
    options: >-
      --health-cmd "redis-cli ping"
      --health-interval 10s
      --health-timeout 5s
      --health-retries 5
```

- [ ] **Step 3: Add database initialization step**

```yaml
# Add to .github/workflows/maven-build.yml build job steps
- name: Initialize Databases
  run: |
    export PGPASSWORD=admin123
    cd scripts/db
    psql -h localhost -U admin -f microservices/001_db_user.sql
    psql -h localhost -U admin -f microservices/002_db_org.sql
    psql -h localhost -U admin -f microservices/003_db_permission.sql
    # ... add all SQL files
```

- [ ] **Step 4: Remove ci.yml**

```bash
git rm .github/workflows/ci.yml
```

- [ ] **Step 5: Test consolidated workflow**

```bash
# Push to develop branch to trigger workflow
git push origin develop
```

- [ ] **Step 6: Commit**

```bash
git add .github/workflows/maven-build.yml .github/workflows/ci.yml
git commit -m "ci: consolidate CI workflows into single pipeline"
```

---

### Task 3: Implement Deployment Pipeline

**Files:**
- Modify: `.github/workflows/maven-build.yml`

- [ ] **Step 1: Implement deploy-dev job**

```yaml
# Replace deploy-dev job in .github/workflows/maven-build.yml
deploy-dev:
  name: Deploy to Development
  runs-on: ubuntu-latest
  needs: docker-build
  timeout-minutes: 15
  if: github.ref == 'refs/heads/develop'
  environment: development

  steps:
    - name: Checkout code
      uses: actions/checkout@v4

    - name: Configure kubectl
      uses: azure/k8s-set-context@v3
      with:
        method: kubeconfig
        kubeconfig: ${{ secrets.KUBE_CONFIG_DEV }}

    - name: Deploy to dev namespace
      run: |
        kubectl set image deployment/scm-auth scm-auth=${{ secrets.DOCKERHUB_USERNAME }}/scm-auth:develop-${{ github.sha }} -n scm-dev
        kubectl set image deployment/scm-gateway scm-gateway=${{ secrets.DOCKERHUB_USERNAME }}/scm-gateway:develop-${{ github.sha }} -n scm-dev
        kubectl set image deployment/scm-system scm-system=${{ secrets.DOCKERHUB_USERNAME }}/scm-system:develop-${{ github.sha }} -n scm-dev
        kubectl rollout status deployment/scm-auth -n scm-dev
        kubectl rollout status deployment/scm-gateway -n scm-dev
        kubectl rollout status deployment/scm-system -n scm-dev
```

- [ ] **Step 2: Implement deploy-prod job**

```yaml
# Replace deploy-prod job in .github/workflows/maven-build.yml
deploy-prod:
  name: Deploy to Production
  runs-on: ubuntu-latest
  needs: docker-build
  timeout-minutes: 15
  if: github.ref == 'refs/heads/master'
  environment: production

  steps:
    - name: Checkout code
      uses: actions/checkout@v4

    - name: Configure kubectl
      uses: azure/k8s-set-context@v3
      with:
        method: kubeconfig
        kubeconfig: ${{ secrets.KUBE_CONFIG_PROD }}

    - name: Deploy to production namespace
      run: |
        kubectl set image deployment/scm-auth scm-auth=${{ secrets.DOCKERHUB_USERNAME }}/scm-auth:${{ github.sha }} -n scm-prod
        kubectl set image deployment/scm-gateway scm-gateway=${{ secrets.DOCKERHUB_USERNAME }}/scm-gateway:${{ github.sha }} -n scm-prod
        kubectl set image deployment/scm-system scm-system=${{ secrets.DOCKERHUB_USERNAME }}/scm-system:${{ github.sha }} -n scm-prod
        kubectl rollout status deployment/scm-auth -n scm-prod
        kubectl rollout status deployment/scm-gateway -n scm-prod
        kubectl rollout status deployment/scm-system -n scm-prod
```

- [ ] **Step 3: Add deployment verification**

```yaml
# Add to deploy-dev and deploy-prod jobs
- name: Verify deployment
  run: |
    kubectl get pods -n scm-dev -l app=scm-auth
    kubectl get pods -n scm-dev -l app=scm-gateway
    kubectl get pods -n scm-dev -l app=scm-system
```

- [ ] **Step 4: Commit**

```bash
git add .github/workflows/maven-build.yml
git commit -m "ci: implement deployment pipeline for dev and prod"
```

---

### Task 4: Add JaCoCo Coverage Thresholds

**Files:**
- Modify: `com.scm.parent/pom.xml`

- [ ] **Step 1: Add coverage rules to JaCoCo configuration**

```xml
<!-- Replace JaCoCo plugin in com.scm.parent/pom.xml -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>verify</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>check</id>
            <phase>verify</phase>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.70</minimum>
                            </limit>
                            <limit>
                                <counter>BRANCH</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.60</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

- [ ] **Step 2: Test coverage check**

```bash
# Run verify phase to check coverage
mvn clean verify -f com.scm.parent/pom.xml

# Expected: Build fails if coverage below 70% line, 60% branch
```

- [ ] **Step 3: Commit**

```bash
git add com.scm.parent/pom.xml
git commit -m "ci: add JaCoCo coverage thresholds (70% line, 60% branch)"
```

---

### Task 5: Make SonarCloud/OWASP Blocking

**Files:**
- Modify: `.github/workflows/maven-build.yml`

- [ ] **Step 1: Remove continue-on-error from SonarCloud**

```yaml
# In .github/workflows/maven-build.yml, code-quality job
# Remove: continue-on-error: true
# Change to:
- name: SonarCloud Scan
  if: vars.SONAR_ORG != ''
  env:
    GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
    SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
  run: |
    mvn -B verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
      -f com.scm.parent/pom.xml \
      -Dsonar.projectKey=scm-platform \
      -Dsonar.organization=${{ vars.SONAR_ORG }} \
      -Dsonar.host.url=https://sonarcloud.io
```

- [ ] **Step 2: Remove continue-on-error from OWASP**

```yaml
# In .github/workflows/maven-build.yml, security-scan job
# Remove: continue-on-error: true
# Change to:
- name: Run OWASP Dependency Check
  run: |
    mvn org.owasp:dependency-check-maven:check \
      -f com.scm.parent/pom.xml \
      -DfailBuildOnCVSS=7
```

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/maven-build.yml
git commit -m "ci: make SonarCloud and OWASP blocking quality gates"
```

---

### Task 6: Implement PostgreSQL HA

**Files:**
- Create: `deploy/postgresql/ha/patroni.yml`
- Create: `deploy/postgresql/ha/etcd.yml`
- Create: `docker-compose.ha.yml`

- [ ] **Step 1: Create Patroni configuration**

```yaml
# deploy/postgresql/ha/patroni.yml
scope: scm-postgresql
name: postgresql-0

restapi:
  listen: 0.0.0.0:8008
  connect_address: postgresql-0:8008

etcd3:
  hosts: etcd-0:2379,etcd-1:2379,etcd-2:2379

bootstrap:
  dcs:
    ttl: 30
    loop_wait: 10
    retry_timeout: 10
    maximum_lag_on_failover: 1048576
    postgresql:
      use_pg_rewind: true
      parameters:
        max_connections: 200
        shared_buffers: 1GB
        effective_cache_size: 3GB
        work_mem: 10MB
        maintenance_work_mem: 256MB
        wal_level: replica
        max_wal_senders: 10
        max_replication_slots: 10
        hot_standby: "on"
        archive_mode: "on"
        archive_command: "/bin/true"

  initdb:
    - encoding: UTF8
    - data-checksums

  pg_hba:
    - host replication replicator 10.0.0.0/8 md5
    - host all all 0.0.0.0/0 md5

postgresql:
  listen: 0.0.0.0:5432
  connect_address: postgresql-0:5432
  data_dir: /data/postgresql
  pgpass: /tmp/pgpass
  authentication:
    replication:
      username: replicator
      password: replicator_password
    superuser:
      username: admin
      password: admin123
  parameters:
    unix_socket_directories: '/var/run/postgresql'
```

- [ ] **Step 2: Create Docker Compose for HA**

```yaml
# docker-compose.ha.yml
version: '3.8'

services:
  etcd-0:
    image: quay.io/coreos/etcd:v3.5.12
    command: etcd --name etcd-0 --listen-client-urls http://0.0.0.0:2379 --advertise-client-urls http://etcd-0:2379 --listen-peer-urls http://0.0.0.0:2380 --initial-advertise-peer-urls http://etcd-0:2380 --initial-cluster-token etcd-cluster --initial-cluster etcd-0=http://etcd-0:2380,etcd-1=http://etcd-1:2380,etcd-2=http://etcd-2:2380 --initial-cluster-state new
    ports:
      - "2379:2379"

  etcd-1:
    image: quay.io/coreos/etcd:v3.5.12
    command: etcd --name etcd-1 --listen-client-urls http://0.0.0.0:2379 --advertise-client-urls http://etcd-1:2379 --listen-peer-urls http://0.0.0.0:2380 --initial-advertise-peer-urls http://etcd-1:2380 --initial-cluster-token etcd-cluster --initial-cluster etcd-0=http://etcd-0:2380,etcd-1=http://etcd-1:2380,etcd-2=http://etcd-2:2380 --initial-cluster-state new

  etcd-2:
    image: quay.io/coreos/etcd:v3.5.12
    command: etcd --name etcd-2 --listen-client-urls http://0.0.0.0:2379 --advertise-client-urls http://etcd-2:2379 --listen-peer-urls http://0.0.0.0:2380 --initial-advertise-peer-urls http://etcd-2:2380 --initial-cluster-token etcd-cluster --initial-cluster etcd-0=http://etcd-0:2380,etcd-1=http://etcd-1:2380,etcd-2=http://etcd-2:2380 --initial-cluster-state new

  postgresql-0:
    image: postgres:16-alpine
    environment:
      POSTGRES_USER: admin
      POSTGRES_PASSWORD: admin123
    volumes:
      - postgresql-0:/data/postgresql
      - ./deploy/postgresql/ha/patroni.yml:/etc/patroni/patroni.yml
    ports:
      - "5432:5432"
      - "8008:8008"

  postgresql-1:
    image: postgres:16-alpine
    environment:
      POSTGRES_USER: admin
      POSTGRES_PASSWORD: admin123
    volumes:
      - postgresql-1:/data/postgresql
      - ./deploy/postgresql/ha/patroni.yml:/etc/patroni/patroni.yml

  postgresql-2:
    image: postgres:16-alpine
    environment:
      POSTGRES_USER: admin
      POSTGRES_PASSWORD: admin123
    volumes:
      - postgresql-2:/data/postgresql
      - ./deploy/postgresql/ha/patroni.yml:/etc/patroni/patroni.yml

volumes:
  postgresql-0:
  postgresql-1:
  postgresql-2:
```

- [ ] **Step 3: Test HA setup**

```bash
# Start HA PostgreSQL
docker-compose -f docker-compose.ha.yml up -d

# Check cluster status
curl http://localhost:8008/cluster

# Expected: Shows 3 nodes with one leader
```

- [ ] **Step 4: Commit**

```bash
git add deploy/postgresql/ha/ docker-compose.ha.yml
git commit -m "feat: implement PostgreSQL HA with Patroni"
```

---

### Task 7: Implement Redis Sentinel/Cluster

**Files:**
- Create: `deploy/redis/sentinel.conf`
- Create: `deploy/redis/redis.conf`
- Modify: `docker-compose.yml`

- [ ] **Step 1: Create Redis configuration**

```conf
# deploy/redis/redis.conf
port 6379
bind 0.0.0.0
protected-mode no
requirepass admin123
masterauth admin123

# Persistence
save 900 1
save 300 10
save 60 10000
rdbcompression yes
rdbchecksum yes
dbfilename dump.rdb
dir /data

# Memory
maxmemory 2gb
maxmemory-policy allkeys-lru

# Replication
replica-read-only yes
replica-serve-stale-data yes
```

```conf
# deploy/redis/sentinel.conf
port 26379
bind 0.0.0.0
protected-mode no

sentinel monitor scm-master redis-master 6379 2
sentinel auth-pass scm-master admin123
sentinel down-after-milliseconds scm-master 5000
sentinel failover-timeout scm-master 60000
sentinel parallel-syncs scm-master 1
```

- [ ] **Step 2: Update Docker Compose with Sentinel**

```yaml
# Add to docker-compose.yml
redis-master:
  image: redis:7.2-alpine
  command: redis-server /usr/local/etc/redis/redis.conf
  volumes:
    - ./deploy/redis/redis.conf:/usr/local/etc/redis/redis.conf
    - redis-master:/data
  ports:
    - "6379:6379"

redis-slave-1:
  image: redis:7.2-alpine
  command: redis-server /usr/local/etc/redis/redis.conf --replicaof redis-master 6379
  volumes:
    - ./deploy/redis/redis.conf:/usr/local/etc/redis/redis.conf
  depends_on:
    - redis-master

redis-slave-2:
  image: redis:7.2-alpine
  command: redis-server /usr/local/etc/redis/redis.conf --replicaof redis-master 6379
  volumes:
    - ./deploy/redis/redis.conf:/usr/local/etc/redis/redis.conf
  depends_on:
    - redis-master

redis-sentinel-1:
  image: redis:7.2-alpine
  command: redis-sentinel /usr/local/etc/redis/sentinel.conf
  volumes:
    - ./deploy/redis/sentinel.conf:/usr/local/etc/redis/sentinel.conf
  ports:
    - "26379:26379"
  depends_on:
    - redis-master
    - redis-slave-1
    - redis-slave-2

redis-sentinel-2:
  image: redis:7.2-alpine
  command: redis-sentinel /usr/local/etc/redis/sentinel.conf
  volumes:
    - ./deploy/redis/sentinel.conf:/usr/local/etc/redis/sentinel.conf
  depends_on:
    - redis-master
    - redis-slave-1
    - redis-slave-2

redis-sentinel-3:
  image: redis:7.2-alpine
  command: redis-sentinel /usr/local/etc/redis/sentinel.conf
  volumes:
    - ./deploy/redis/sentinel.conf:/usr/local/etc/redis/sentinel.conf
  depends_on:
    - redis-master
    - redis-slave-1
    - redis-slave-2

volumes:
  redis-master:
```

- [ ] **Step 3: Test Sentinel failover**

```bash
# Start Redis with Sentinel
docker-compose up -d redis-master redis-slave-1 redis-slave-2 redis-sentinel-1 redis-sentinel-2 redis-sentinel-3

# Check Sentinel status
redis-cli -p 26379 sentinel master scm-master

# Simulate failover
docker-compose stop redis-master

# Wait 10 seconds
sleep 10

# Check new master
redis-cli -p 26379 sentinel get-master-addr-by-name scm-master
```

- [ ] **Step 4: Commit**

```bash
git add deploy/redis/ docker-compose.yml
git commit -m "feat: implement Redis Sentinel for high availability"
```

---

### Task 8: Enable Security Headers

**Files:**
- Modify: `scm-common/web/src/main/java/com/scmcloud/common/security/config/SecurityHeadersProperties.java`

- [ ] **Step 1: Enable X-Content-Type-Options by default**

```java
// In SecurityHeadersProperties.java
// Change: private boolean contentTypeOptionsEnabled = false;
// To:
private boolean contentTypeOptionsEnabled = true;
```

- [ ] **Step 2: Add configuration to application.yml**

```yaml
# Add to all service application.yml files
frog:
  security:
    headers:
      content-type-options-enabled: true
      frame-options: DENY
      hsts-enabled: true
      hsts-max-age: 31536000
      hsts-include-subdomains: true
```

- [ ] **Step 3: Test security headers**

```bash
# Start service
mvn spring-boot:run -pl scm-order/service

# Check headers
curl -I http://localhost:8203/api/v1/orders

# Expected headers:
# X-Content-Type-Options: nosniff
# X-Frame-Options: DENY
# Strict-Transport-Security: max-age=31536000; includeSubDomains
```

- [ ] **Step 4: Commit**

```bash
git add scm-common/web/src/main/java/com/scmcloud/common/security/config/SecurityHeadersProperties.java
git commit -m "security: enable X-Content-Type-Options header by default"
```

---

### Task 9: Restrict CORS Headers

**Files:**
- Modify: `scm-common/web/src/main/java/com/scmcloud/common/security/config/SecurityConfig.java`

- [ ] **Step 1: Replace wildcard allowedHeaders**

```java
// In SecurityConfig.java
// Change: .allowedHeaders("*")
// To:
.allowedHeaders("Authorization", "Content-Type", "X-Tenant-Id", "X-Request-ID", "X-Timestamp", "X-Nonce", "X-Signature", "X-App-Id", "X-Sign-Version")
```

- [ ] **Step 2: Test CORS configuration**

```bash
# Start service
mvn spring-boot:run -pl scm-order/service

# Test CORS preflight
curl -X OPTIONS http://localhost:8203/api/v1/orders \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: Authorization, Content-Type" \
  -v

# Expected: 200 OK with Access-Control-Allow-Headers: Authorization, Content-Type, ...
```

- [ ] **Step 3: Commit**

```bash
git add scm-common/web/src/main/java/com/scmcloud/common/security/config/SecurityConfig.java
git commit -m "security: restrict CORS allowedHeaders to specific headers"
```

---

### Task 10: Implement SSRF Protection

**Files:**
- Create: `scm-common/web/src/main/java/com/scmcloud/common/security/filter/SsrfProtectionFilter.java`
- Modify: `scm-common/web/src/main/java/com/scmcloud/common/security/config/SecurityConfig.java`

- [ ] **Step 1: Create SSRF protection filter**

```java
package com.scmcloud.common.security.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Set;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class SsrfProtectionFilter implements Filter {

    private static final Set<String> BLOCKED_HOSTS = Set.of(
        "169.254.169.254",  // AWS metadata
        "metadata.google.internal",  // GCP metadata
        "169.254.169.254",  // Azure metadata
        "localhost",
        "127.0.0.1",
        "0.0.0.0",
        "::1"
    );

    private static final Set<String> BLOCKED_PREFIXES = Set.of(
        "10.",
        "172.16.",
        "172.17.",
        "172.18.",
        "172.19.",
        "172.20.",
        "172.21.",
        "172.22.",
        "172.23.",
        "172.24.",
        "172.25.",
        "172.26.",
        "172.27.",
        "172.28.",
        "172.29.",
        "172.30.",
        "172.31.",
        "192.168."
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String urlParam = httpRequest.getParameter("url");
        if (urlParam != null && !urlParam.isEmpty()) {
            if (isBlockedUrl(urlParam)) {
                httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                httpResponse.getWriter().write("{\"code\":400,\"message\":\"Invalid URL\"}");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private boolean isBlockedUrl(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host == null) {
                return true;
            }

            if (BLOCKED_HOSTS.contains(host.toLowerCase())) {
                return true;
            }

            for (String prefix : BLOCKED_PREFIXES) {
                if (host.startsWith(prefix)) {
                    return true;
                }
            }

            // Check for IP address resolution
            try {
                InetAddress address = InetAddress.getByName(host);
                String resolvedHost = address.getHostAddress();
                if (BLOCKED_HOSTS.contains(resolvedHost)) {
                    return true;
                }
                for (String prefix : BLOCKED_PREFIXES) {
                    if (resolvedHost.startsWith(prefix)) {
                        return true;
                    }
                }
            } catch (UnknownHostException e) {
                // Host cannot be resolved, allow through
            }

            return false;
        } catch (Exception e) {
            return true;
        }
    }
}
```

- [ ] **Step 2: Test SSRF protection**

```bash
# Start service
mvn spring-boot:run -pl scm-order/service

# Test blocked URL
curl http://localhost:8203/api/v1/test?url=http://169.254.169.254/latest/meta-data/

# Expected: 400 Bad Request
```

- [ ] **Step 3: Commit**

```bash
git add scm-common/web/src/main/java/com/scmcloud/common/security/filter/SsrfProtectionFilter.java
git commit -m "security: implement SSRF protection filter"
```

---

### Task 11: Define SLOs

**Files:**
- Create: `docs/operations/SLO.md`

- [ ] **Step 1: Create SLO document**

```markdown
# Service Level Objectives (SLOs)

## Overview
This document defines the Service Level Objectives (SLOs) for the SCM platform.

## SLO Definitions

### Availability SLOs
| Service | SLO | Measurement | Window |
|---------|-----|-------------|--------|
| API Gateway | 99.9% | Successful requests / Total requests | 30 days |
| Auth Service | 99.9% | Successful authentications / Total attempts | 30 days |
| Order Service | 99.5% | Successful orders / Total orders | 30 days |
| Inventory Service | 99.9% | Successful stock checks / Total checks | 30 days |
| Payment Service | 99.9% | Successful payments / Total payments | 30 days |

### Latency SLOs
| Service | P50 | P95 | P99 | Measurement |
|---------|-----|-----|-----|-------------|
| API Gateway | <50ms | <100ms | <200ms | Request latency |
| Auth Service | <100ms | <200ms | <500ms | Authentication latency |
| Order Service | <200ms | <500ms | <1000ms | Order creation latency |
| Inventory Service | <50ms | <100ms | <200ms | Stock check latency |
| Search Service | <100ms | <200ms | <500ms | Search latency |

### Throughput SLOs
| Service | Minimum | Target | Measurement |
|---------|---------|--------|-------------|
| API Gateway | 1000 RPS | 5000 RPS | Requests per second |
| Order Service | 100 RPS | 500 RPS | Orders per second |
| Inventory Service | 500 RPS | 2000 RPS | Stock checks per second |

## Error Budget
- **Error Budget**: 1% (30-day window)
- **Burn Rate**: Alert if >10% budget consumed in 1 hour
- **Action**: If error budget exhausted, freeze deployments until reliability improves

## Alerting
- **Page**: SLO breach imminent (burn rate >10x)
- **Warn**: SLO breach possible (burn rate >2x)
- **Info**: Error budget consumption tracked
```

- [ ] **Step 2: Commit**

```bash
git add docs/operations/SLO.md
git commit -m "docs: define Service Level Objectives for all services"
```

---

### Task 12: Implement Alerting Rules

**Files:**
- Create: `config/prometheus/alert-rules.yml`
- Modify: `config/prometheus/prometheus.yml`

- [ ] **Step 1: Create alert rules**

```yaml
# config/prometheus/alert-rules.yml
groups:
  - name: scm-alerts
    rules:
      # SLO Alerts
      - alert: HighErrorRate
        expr: sum(rate(http_requests_total{status=~"5.."}[5m])) / sum(rate(http_requests_total[5m])) > 0.01
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value | humanizePercentage }} (threshold: 1%)"

      - alert: HighLatency
        expr: histogram_quantile(0.95, sum(rate(http_request_duration_seconds_bucket[5m])) by (le)) > 0.5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High latency detected"
          description: "P95 latency is {{ $value | humanizeDuration }} (threshold: 500ms)"

      # Infrastructure Alerts
      - alert: HighCPUUsage
        expr: process_cpu_usage > 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High CPU usage"
          description: "CPU usage is {{ $value | humanizePercentage }}"

      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High memory usage"
          description: "Heap usage is {{ $value | humanizePercentage }}"

      - alert: DatabaseConnectionPoolExhausted
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Database connection pool near exhaustion"
          description: "Connection pool usage is {{ $value | humanizePercentage }}"

      # Business Alerts
      - alert: InventoryLowStock
        expr: inv_available_stock < inv_safety_stock
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Low inventory stock"
          description: "SKU {{ $labels.sku_id }} has {{ $value }} units available (safety stock: {{ $labels.safety_stock }})"

      - alert: OrderProcessingDelay
        expr: time() - ord_order_create_time > 3600
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Order processing delayed"
          description: "Order {{ $labels.order_no }} has been pending for {{ $value | humanizeDuration }}"
```

- [ ] **Step 2: Update Prometheus configuration**

```yaml
# Add to config/prometheus/prometheus.yml
rule_files:
  - "alert-rules.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093
```

- [ ] **Step 3: Test alert rules**

```bash
# Start Prometheus
docker-compose up -d scm-prometheus

# Check rules
curl http://localhost:9090/api/v1/rules

# Expected: Shows loaded rules
```

- [ ] **Step 4: Commit**

```bash
git add config/prometheus/alert-rules.yml config/prometheus/prometheus.yml
git commit -m "ops: implement Prometheus alerting rules"
```

---

### Task 13: Write Operational Runbooks

**Files:**
- Create: `docs/operations/runbooks/README.md`
- Create: `docs/operations/runbooks/incident-response.md`
- Create: `docs/operations/runbooks/database-failover.md`
- Create: `docs/operations/runbooks/redis-failover.md`
- Create: `docs/operations/runbooks/deployment-rollback.md`

- [ ] **Step 1: Create runbook index**

```markdown
# Operational Runbooks

## Overview
This directory contains operational runbooks for the SCM platform.

## Runbooks

### Incident Response
- [Incident Response Process](incident-response.md)
- [Escalation Matrix](escalation-matrix.md)

### Infrastructure
- [Database Failover](database-failover.md)
- [Redis Failover](redis-failover.md)
- [Kubernetes Node Failure](k8s-node-failure.md)

### Deployment
- [Deployment Rollback](deployment-rollback.md)
- [Canary Deployment Issues](canary-deployment-issues.md)

### Application
- [High Error Rate](high-error-rate.md)
- [High Latency](high-latency.md)
- [Memory Leak](memory-leak.md)
```

- [ ] **Step 2: Create incident response runbook**

```markdown
# Incident Response Process

## Severity Levels

### P1 - Critical
- **Impact**: Service completely down, data loss
- **Response Time**: 15 minutes
- **Resolution Time**: 1 hour
- **Examples**: Database down, auth service unreachable

### P2 - High
- **Impact**: Major feature unavailable, degraded performance
- **Response Time**: 30 minutes
- **Resolution Time**: 4 hours
- **Examples**: Order creation failing, search not working

### P3 - Medium
- **Impact**: Minor feature unavailable, workaround exists
- **Response Time**: 2 hours
- **Resolution Time**: 24 hours
- **Examples**: Export not working, specific report failing

### P4 - Low
- **Impact**: Cosmetic issue, no business impact
- **Response Time**: 24 hours
- **Resolution Time**: 1 week
- **Examples**: UI typo, minor visual issue

## Incident Response Steps

### 1. Detect
- Alert fires in PagerDuty/Opsgenie
- User reports issue
- Monitoring dashboard shows anomaly

### 2. Triage
- Assess severity (P1-P4)
- Identify affected services
- Determine business impact

### 3. Respond
- Assign incident commander
- Create incident channel (Slack/Teams)
- Notify stakeholders

### 4. Mitigate
- Implement immediate fix (rollback, restart, scale)
- Verify fix resolves issue
- Monitor for recurrence

### 5. Resolve
- Implement permanent fix
- Update documentation
- Conduct post-mortem

### 6. Learn
- Document lessons learned
- Update runbooks
- Implement preventive measures
```

- [ ] **Step 3: Create database failover runbook**

```markdown
# Database Failover Runbook

## Automatic Failover (Patroni)

### Symptoms
- Alert: Database connection failures
- Alert: Replication lag exceeds threshold
- Alert: Database health check failures

### Steps

1. **Verify Patroni cluster status**
   ```bash
   curl http://localhost:8008/cluster
   ```

2. **Check if automatic failover occurred**
   ```bash
   curl http://localhost:8008/leader
   ```

3. **If automatic failover failed, manually trigger**
   ```bash
   curl -X POST http://localhost:8008/failover -d '{"leader":"postgresql-0","candidate":"postgresql-1"}'
   ```

4. **Verify new leader is serving requests**
   ```bash
   psql -h localhost -U admin -d db_order -c "SELECT 1;"
   ```

5. **Check application connectivity**
   ```bash
   curl http://localhost:8203/actuator/health
   ```

6. **Monitor replication status**
   ```bash
   psql -h localhost -U admin -d db_order -c "SELECT * FROM pg_stat_replication;"
   ```

## Manual Failover

### Prerequisites
- Application is in maintenance mode or can tolerate brief downtime
- All writes have been flushed to disk

### Steps

1. **Stop application writes**
   ```bash
   # Scale down order service
   kubectl scale deployment/scm-order --replicas=0 -n scm-prod
   ```

2. **Wait for replication to catch up**
   ```bash
   psql -h localhost -U admin -d db_order -c "SELECT pg_last_wal_receive_lsn() - pg_last_wal_replay_lsn() AS replication_lag;"
   ```

3. **Trigger manual failover**
   ```bash
   curl -X POST http://localhost:8008/failover -d '{"leader":"postgresql-0","candidate":"postgresql-1"}'
   ```

4. **Verify new leader**
   ```bash
   curl http://localhost:8008/leader
   ```

5. **Restore application writes**
   ```bash
   kubectl scale deployment/scm-order --replicas=3 -n scm-prod
   ```

6. **Monitor for issues**
   ```bash
   # Check error rates
   curl http://localhost:9090/api/v1/query?query=rate(http_requests_total{status=~"5.."}[5m])
   ```
```

- [ ] **Step 4: Commit**

```bash
git add docs/operations/runbooks/
git commit -m "docs: create operational runbooks for incident response"
```

---

### Task 14: Implement Backup Automation

**Files:**
- Create: `scripts/backup/backup-databases.sh`
- Create: `scripts/backup/restore-database.sh`
- Create: `.github/workflows/backup.yml`

- [ ] **Step 1: Create backup script**

```bash
#!/bin/bash
# scripts/backup/backup-databases.sh

set -e

# Configuration
BACKUP_DIR="/backups/postgresql"
RETENTION_DAYS=30
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
DATABASES=(
    "db_user"
    "db_org"
    "db_permission"
    "db_approval"
    "db_audit"
    "db_notify"
    "db_product"
    "db_inventory"
    "db_order"
    "db_warehouse"
    "db_logistics"
    "db_supplier"
    "db_tenant"
    "db_finance"
    "db_purchase"
)

# Create backup directory
mkdir -p "${BACKUP_DIR}/${TIMESTAMP}"

# Backup each database
for DB in "${DATABASES[@]}"; do
    echo "Backing up ${DB}..."
    pg_dump -h "${DB_HOST:-localhost}" -U "${DB_USER:-admin}" -d "${DB}" -F c -f "${BACKUP_DIR}/${TIMESTAMP}/${DB}.dump"
    echo "Completed ${DB}"
done

# Compress backups
tar -czf "${BACKUP_DIR}/backup_${TIMESTAMP}.tar.gz" -C "${BACKUP_DIR}/${TIMESTAMP}" .
rm -rf "${BACKUP_DIR}/${TIMESTAMP}"

# Clean old backups
find "${BACKUP_DIR}" -name "backup_*.tar.gz" -mtime +${RETENTION_DAYS} -delete

echo "Backup completed: ${BACKUP_DIR}/backup_${TIMESTAMP}.tar.gz"
```

- [ ] **Step 2: Create restore script**

```bash
#!/bin/bash
# scripts/backup/restore-database.sh

set -e

# Configuration
BACKUP_FILE="$1"
DATABASE="$2"
TARGET_HOST="${3:-localhost}"
TARGET_USER="${4:-admin}"

if [ -z "$BACKUP_FILE" ] || [ -z "$DATABASE" ]; then
    echo "Usage: $0 <backup_file> <database> [target_host] [target_user]"
    exit 1
fi

# Extract backup
TEMP_DIR=$(mktemp -d)
tar -xzf "${BACKUP_FILE}" -C "${TEMP_DIR}"

# Restore database
echo "Restoring ${DATABASE} from ${BACKUP_FILE}..."
pg_restore -h "${TARGET_HOST}" -U "${TARGET_USER}" -d "${DATABASE}" -c "${TEMP_DIR}/${DATABASE}.dump"

# Cleanup
rm -rf "${TEMP_DIR}"

echo "Restore completed: ${DATABASE}"
```

- [ ] **Step 3: Create backup workflow**

```yaml
# .github/workflows/backup.yml
name: Database Backup

on:
  schedule:
    - cron: '0 2 * * *'  # Daily at 2 AM
  workflow_dispatch:

jobs:
  backup:
    runs-on: ubuntu-latest
    timeout-minutes: 30

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Setup PostgreSQL client
        run: |
          sudo apt-get update
          sudo apt-get install -y postgresql-client

      - name: Run backup
        env:
          DB_HOST: ${{ secrets.DB_HOST }}
          DB_USER: ${{ secrets.DB_USER }}
          PGPASSWORD: ${{ secrets.DB_PASSWORD }}
        run: |
          chmod +x scripts/backup/backup-databases.sh
          ./scripts/backup/backup-databases.sh

      - name: Upload backup to S3
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ secrets.AWS_REGION }}

      - run: |
          aws s3 cp /backups/postgresql/backup_*.tar.gz s3://${{ secrets.BACKUP_BUCKET }}/postgresql/
```

- [ ] **Step 4: Make scripts executable**

```bash
chmod +x scripts/backup/backup-databases.sh
chmod +x scripts/backup/restore-database.sh
```

- [ ] **Step 5: Commit**

```bash
git add scripts/backup/ .github/workflows/backup.yml
git commit -m "ops: implement database backup automation"
```

---

### Task 15: Implement Health Check Endpoints

**Files:**
- Modify: All service `application.yml` files
- Create: `scm-common/web/src/main/java/com/scmcloud/common/health/CompositeHealthIndicator.java`

- [ ] **Step 1: Enable actuator endpoints**

```yaml
# Add to all service application.yml files
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
      show-components: always
  health:
    db:
      enabled: true
    redis:
      enabled: true
    diskspace:
      enabled: true
```

- [ ] **Step 2: Create composite health indicator**

```java
package com.scmcloud.common.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class CompositeHealthIndicator implements HealthIndicator {

    private final HealthIndicator dbHealthIndicator;
    private final HealthIndicator redisHealthIndicator;

    public CompositeHealthIndicator(
            @Qualifier("dbHealthIndicator") HealthIndicator dbHealthIndicator,
            @Qualifier("redisHealthIndicator") HealthIndicator redisHealthIndicator) {
        this.dbHealthIndicator = dbHealthIndicator;
        this.redisHealthIndicator = redisHealthIndicator;
    }

    @Override
    public Health health() {
        Health dbHealth = dbHealthIndicator.health();
        Health redisHealth = redisHealthIndicator.health();

        if (dbHealth.getStatus().equals(Status.UP) && redisHealth.getStatus().equals(Status.UP)) {
            return Health.up()
                    .withDetail("database", dbHealth.getDetails())
                    .withDetail("redis", redisHealth.getDetails())
                    .build();
        }

        return Health.down()
                .withDetail("database", dbHealth.getDetails())
                .withDetail("redis", redisHealth.getDetails())
                .build();
    }
}
```

- [ ] **Step 3: Test health endpoints**

```bash
# Start service
mvn spring-boot:run -pl scm-order/service

# Check health endpoint
curl http://localhost:8203/actuator/health

# Expected response:
# {
#   "status": "UP",
#   "components": {
#     "db": {"status": "UP"},
#     "redis": {"status": "UP"},
#     "diskSpace": {"status": "UP"}
#   }
# }
```

- [ ] **Step 4: Commit**

```bash
git add scm-common/web/src/main/java/com/scmcloud/common/health/CompositeHealthIndicator.java
git commit -m "feat: implement comprehensive health check endpoints"
```

---

## Phase 2: Enhancement (60 Days)

### Task 16: Implement Contract Testing (Pact)

**Files:**
- Create: `scm-order/service/src/test/java/com/scmcloud/order/contract/OrderPactTest.java`
- Create: `scm-inventory/service/src/test/java/com/scmcloud/inventory/contract/InventoryPactTest.java`

- [ ] **Step 1: Add Pact dependency**

```xml
<!-- Add to com.scm.parent/pom.xml -->
<dependency>
    <groupId>au.com.dius.pact.consumer</groupId>
    <artifactId>junit5</artifactId>
    <version>4.6.5</version>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 2: Create order contract test**

```java
package com.scmcloud.order.contract;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "InventoryService", port = "8082")
class OrderPactTest {

    @Pact(consumer = "OrderService")
    public RequestResponsePact createPact(PactDslWithProvider builder) {
        return builder
            .given("inventory exists for SKU-001")
            .uponReceiving("a request to check inventory")
            .path("/api/v1/inventory/check")
            .method("POST")
            .body("{\"skuId\":\"SKU-001\",\"quantity\":10}")
            .willRespondWith()
            .status(200)
            .body("{\"available\":true,\"stock\":100}")
            .toPact();
    }

    @Test
    void testInventoryCheck(MockServer mockServer) {
        // Test implementation
        var client = new InventoryClient(mockServer.getUrl());
        var result = client.checkInventory("SKU-001", 10);
        assertTrue(result.isAvailable());
        assertEquals(100, result.getStock());
    }
}
```

- [ ] **Step 3: Run contract tests**

```bash
# Run Pact tests
mvn test -pl scm-order/service -Dtest="*PactTest*"

# Expected: Tests pass, Pact files generated
```

- [ ] **Step 4: Commit**

```bash
git add scm-order/service/src/test/java/com/scmcloud/order/contract/OrderPactTest.java
git commit -m "test: implement contract testing with Pact"
```

---

### Task 17: Implement E2E Testing (Playwright)

**Files:**
- Create: `scm-web/e2e/login.spec.ts`
- Create: `scm-web/e2e/order.spec.ts`
- Create: `scm-web/playwright.config.ts`

- [ ] **Step 1: Install Playwright**

```bash
cd scm-web
npm init playwright@latest
```

- [ ] **Step 2: Create login test**

```typescript
// scm-web/e2e/login.spec.ts
import { test, expect } from '@playwright/test';

test('login page displays correctly', async ({ page }) => {
  await page.goto('/login');
  await expect(page.locator('h1')).toContainText('Login');
  await expect(page.locator('input[name="username"]')).toBeVisible();
  await expect(page.locator('input[name="password"]')).toBeVisible();
});

test('successful login redirects to dashboard', async ({ page }) => {
  await page.goto('/login');
  await page.fill('input[name="username"]', 'admin');
  await page.fill('input[name="password"]', 'admin123');
  await page.click('button[type="submit"]');
  await expect(page).toHaveURL('/dashboard');
});
```

- [ ] **Step 3: Create order test**

```typescript
// scm-web/e2e/order.spec.ts
import { test, expect } from '@playwright/test';

test('create order flow', async ({ page }) => {
  // Login first
  await page.goto('/login');
  await page.fill('input[name="username"]', 'admin');
  await page.fill('input[name="password"]', 'admin123');
  await page.click('button[type="submit"]');

  // Navigate to orders
  await page.goto('/order');
  await expect(page.locator('h1')).toContainText('Orders');

  // Create new order
  await page.click('button:has-text("New Order")');
  await page.fill('input[name="customer"]', 'Test Customer');
  await page.click('button:has-text("Submit")');

  // Verify order created
  await expect(page.locator('.success-message')).toBeVisible();
});
```

- [ ] **Step 4: Configure Playwright**

```typescript
// scm-web/playwright.config.ts
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: 'html',
  use: {
    baseURL: 'http://localhost:3000',
    trace: 'on-first-retry',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
    },
  ],
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:3000',
    reuseExistingServer: !process.env.CI,
  },
});
```

- [ ] **Step 5: Run E2E tests**

```bash
cd scm-web
npx playwright test

# Expected: Tests pass
```

- [ ] **Step 6: Commit**

```bash
git add scm-web/e2e/ scm-web/playwright.config.ts
git commit -m "test: implement E2E testing with Playwright"
```

---

### Task 18: Implement Mutation Testing (PIT)

**Files:**
- Modify: `com.scm.parent/pom.xml`

- [ ] **Step 1: Add PIT dependency**

```xml
<!-- Add to com.scm.parent/pom.xml <build><plugins> section -->
<plugin>
    <groupId>org.pitest</groupId>
    <artifactId>pitest-maven</artifactId>
    <version>1.16.1</version>
    <configuration>
        <targetClasses>
            <param>com.scmcloud.*</param>
        </targetClasses>
        <targetTests>
            <param>com.scmcloud.*</param>
        </targetTests>
        <outputFormats>
            <outputFormat>HTML</outputFormat>
            <outputFormat>XML</outputFormat>
        </outputFormats>
        <timestampedReports>false</timestampedReports>
    </configuration>
</plugin>
```

- [ ] **Step 2: Run mutation testing**

```bash
# Run PIT for order service
mvn org.pitest:pitest-maven:mutationCoverage -pl scm-order/service

# Expected: Mutation report generated in target/pit-reports/
```

- [ ] **Step 3: Commit**

```bash
git add com.scm.parent/pom.xml
git commit -m "test: implement mutation testing with PIT"
```

---

### Task 19: Migrate to Kubernetes

**Files:**
- Create: `deploy/k8s/namespace.yml`
- Create: `deploy/k8s/configmap.yml`
- Create: `deploy/k8s/secrets.yml`
- Create: `deploy/k8s/scm-auth-deployment.yml`
- Create: `deploy/k8s/scm-auth-service.yml`
- Create: `deploy/k8s/scm-gateway-deployment.yml`
- Create: `deploy/k8s/scm-gateway-service.yml`

- [ ] **Step 1: Create namespace**

```yaml
# deploy/k8s/namespace.yml
apiVersion: v1
kind: Namespace
metadata:
  name: scm-prod
  labels:
    name: scm-prod
```

- [ ] **Step 2: Create ConfigMap**

```yaml
# deploy/k8s/configmap.yml
apiVersion: v1
kind: ConfigMap
metadata:
  name: scm-config
  namespace: scm-prod
data:
  NACOS_SERVER: "nacos:8848"
  DB_HOST: "postgresql"
  DB_PORT: "5432"
  REDIS_HOST: "redis"
  REDIS_PORT: "6379"
  KAFKA_SERVERS: "kafka:9092"
```

- [ ] **Step 3: Create Secrets**

```yaml
# deploy/k8s/secrets.yml
apiVersion: v1
kind: Secret
metadata:
  name: scm-secrets
  namespace: scm-prod
type: Opaque
data:
  DB_USERNAME: YWRtaW4=  # admin
  DB_PASSWORD: YWRtaW4xMjM=  # admin123
  REDIS_PASSWORD: YWRtaW4xMjM=  # admin123
  JWT_SECRET: eW91ci1qd3Qtc2VjcmV0LWhlcmU=  # your-jwt-secret-here
```

- [ ] **Step 4: Create Auth deployment**

```yaml
# deploy/k8s/scm-auth-deployment.yml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: scm-auth
  namespace: scm-prod
spec:
  replicas: 3
  selector:
    matchLabels:
      app: scm-auth
  template:
    metadata:
      labels:
        app: scm-auth
    spec:
      containers:
        - name: scm-auth
          image: ${DOCKERHUB_USERNAME}/scm-auth:latest
          ports:
            - containerPort: 8106
          envFrom:
            - configMapRef:
                name: scm-config
            - secretRef:
                name: scm-secrets
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
            initialDelaySeconds: 30
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8106
            initialDelaySeconds: 20
            periodSeconds: 5
```

- [ ] **Step 5: Create Auth service**

```yaml
# deploy/k8s/scm-auth-service.yml
apiVersion: v1
kind: Service
metadata:
  name: scm-auth
  namespace: scm-prod
spec:
  selector:
    app: scm-auth
  ports:
    - port: 8106
      targetPort: 8106
  type: ClusterIP
```

- [ ] **Step 6: Deploy to Kubernetes**

```bash
# Apply all resources
kubectl apply -f deploy/k8s/namespace.yml
kubectl apply -f deploy/k8s/configmap.yml
kubectl apply -f deploy/k8s/secrets.yml
kubectl apply -f deploy/k8s/scm-auth-deployment.yml
kubectl apply -f deploy/k8s/scm-auth-service.yml

# Check deployment status
kubectl get pods -n scm-prod
kubectl get services -n scm-prod
```

- [ ] **Step 7: Commit**

```bash
git add deploy/k8s/
git commit -m "feat: migrate to Kubernetes deployment"
```

---

### Task 20: Implement Horizontal Pod Autoscaling

**Files:**
- Create: `deploy/k8s/scm-auth-hpa.yml`

- [ ] **Step 1: Create HPA configuration**

```yaml
# deploy/k8s/scm-auth-hpa.yml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: scm-auth-hpa
  namespace: scm-prod
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: scm-auth
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
```

- [ ] **Step 2: Deploy HPA**

```bash
kubectl apply -f deploy/k8s/scm-auth-hpa.yml

# Check HPA status
kubectl get hpa -n scm-prod
```

- [ ] **Step 3: Commit**

```bash
git add deploy/k8s/scm-auth-hpa.yml
git commit -m "feat: implement horizontal pod autoscaling"
```

---

## Phase 3: Resilience (90 Days)

### Task 21: Implement Chaos Engineering

**Files:**
- Create: `deploy/chaos/experiments/pod-kill.yml`
- Create: `deploy/chaos/experiments/network-delay.yml`

- [ ] **Step 1: Create pod kill experiment**

```yaml
# deploy/chaos/experiments/pod-kill.yml
apiVersion: chaos-mesh.org/v1alpha1
kind: PodChaos
metadata:
  name: pod-kill-scm-auth
  namespace: scm-prod
spec:
  action: pod-kill
  mode: one
  selector:
    namespaces:
      - scm-prod
    labelSelectors:
      app: scm-auth
  scheduler:
    cron: '@every 1h'
```

- [ ] **Step 2: Create network delay experiment**

```yaml
# deploy/chaos/experiments/network-delay.yml
apiVersion: chaos-mesh.org/v1alpha1
kind: NetworkChaos
metadata:
  name: network-delay-scm-order
  namespace: scm-prod
spec:
  action: delay
  mode: one
  selector:
    namespaces:
      - scm-prod
    labelSelectors:
      app: scm-order
  delay:
    latency: '100ms'
    jitter: '10ms'
    correlation: '25'
  duration: '5m'
  scheduler:
    cron: '@every 30m'
```

- [ ] **Step 3: Deploy Chaos Mesh**

```bash
# Install Chaos Mesh
helm repo add chaos-mesh https://charts.chaos-mesh.org
helm install chaos-mesh chaos-mesh/chaos-mesh --namespace chaos-mesh --create-namespace

# Deploy experiments
kubectl apply -f deploy/chaos/experiments/
```

- [ ] **Step 4: Commit**

```bash
git add deploy/chaos/
git commit -m "feat: implement chaos engineering with Chaos Mesh"
```

---

### Task 22: Implement Disaster Recovery Procedures

**Files:**
- Create: `docs/operations/DISASTER_RECOVERY.md`

- [ ] **Step 1: Create DR document**

```markdown
# Disaster Recovery Plan

## Recovery Objectives

| Component | RTO | RPO | Strategy |
|-----------|-----|-----|----------|
| Database | 1 hour | 5 minutes | Patroni HA + WAL archiving |
| Redis | 5 minutes | 1 minute | Sentinel + RDB snapshots |
| Application | 15 minutes | N/A | Kubernetes multi-AZ |
| Kafka | 30 minutes | 0 | Replication factor 3 |

## Recovery Procedures

### Database Recovery

#### Scenario 1: Single Node Failure
Patroni automatically fails over to replica. No manual intervention needed.

#### Scenario 2: Complete Cluster Failure
1. Restore from latest backup
2. Replay WAL archives
3. Verify data integrity
4. Update DNS/load balancer

### Redis Recovery

#### Scenario 1: Master Failure
Sentinel promotes replica. No manual intervention needed.

#### Scenario 2: Complete Cluster Failure
1. Restore from RDB snapshot
2. Replay AOF if available
3. Warm cache from database

### Application Recovery

#### Scenario 1: Single Pod Failure
Kubernetes automatically restarts pod.

#### Scenario 2: Complete Region Failure
1. Switch to backup region
2. Update DNS
3. Verify data consistency
```

- [ ] **Step 2: Commit**

```bash
git add docs/operations/DISASTER_RECOVERY.md
git commit -m "docs: create disaster recovery plan"
```

---

## Phase 4: Optimization (180 Days)

### Task 23: Implement Event Sourcing for Orders

**Files:**
- Create: `scm-order/service/src/main/java/com/scmcloud/order/event/OrderEvent.java`
- Create: `scm-order/service/src/main/java/com/scmcloud/order/event/OrderEventStore.java`
- Create: `scm-order/service/src/main/java/com/scmcloud/order/event/OrderAggregate.java`

- [ ] **Step 1: Create OrderEvent class**

```java
package com.scmcloud.order.event;

import java.time.Instant;
import java.util.UUID;

public abstract class OrderEvent {
    private final UUID eventId;
    private final UUID orderId;
    private final Instant timestamp;
    private final String eventType;

    protected OrderEvent(UUID orderId, String eventType) {
        this.eventId = UUID.randomUUID();
        this.orderId = orderId;
        this.timestamp = Instant.now();
        this.eventType = eventType;
    }

    // Getters
    public UUID getEventId() { return eventId; }
    public UUID getOrderId() { return orderId; }
    public Instant getTimestamp() { return timestamp; }
    public String getEventType() { return eventType; }
}

public class OrderCreatedEvent extends OrderEvent {
    private final String customerName;
    private final BigDecimal totalAmount;

    public OrderCreatedEvent(UUID orderId, String customerName, BigDecimal totalAmount) {
        super(orderId, "ORDER_CREATED");
        this.customerName = customerName;
        this.totalAmount = totalAmount;
    }

    // Getters
    public String getCustomerName() { return customerName; }
    public BigDecimal getTotalAmount() { return totalAmount; }
}

public class OrderStatusChangedEvent extends OrderEvent {
    private final String oldStatus;
    private final String newStatus;

    public OrderStatusChangedEvent(UUID orderId, String oldStatus, String newStatus) {
        super(orderId, "ORDER_STATUS_CHANGED");
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    // Getters
    public String getOldStatus() { return oldStatus; }
    public String getNewStatus() { return newStatus; }
}
```

- [ ] **Step 2: Create OrderEventStore**

```java
package com.scmcloud.order.event;

import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public class OrderEventStore {
    
    private final OrderEventMapper eventMapper;

    public OrderEventStore(OrderEventMapper eventMapper) {
        this.eventMapper = eventMapper;
    }

    public void append(OrderEvent event) {
        eventMapper.insert(event);
    }

    public List<OrderEvent> getEvents(UUID orderId) {
        return eventMapper.findByOrderIdOrderByTimestamp(orderId);
    }

    public List<OrderEvent> getEvents(UUID orderId, int offset, int limit) {
        return eventMapper.findByOrderIdOrderByTimestamp(orderId, offset, limit);
    }
}
```

- [ ] **Step 3: Create OrderAggregate**

```java
package com.scmcloud.order.event;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OrderAggregate {
    private UUID orderId;
    private String customerName;
    private BigDecimal totalAmount;
    private String status;
    private List<OrderEvent> uncommittedEvents = new ArrayList<>();

    public static OrderAggregate create(UUID orderId, String customerName, BigDecimal totalAmount) {
        OrderAggregate aggregate = new OrderAggregate();
        OrderCreatedEvent event = new OrderCreatedEvent(orderId, customerName, totalAmount);
        aggregate.apply(event);
        aggregate.uncommittedEvents.add(event);
        return aggregate;
    }

    public void changeStatus(String newStatus) {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(orderId, this.status, newStatus);
        apply(event);
        uncommittedEvents.add(event);
    }

    public void apply(OrderEvent event) {
        if (event instanceof OrderCreatedEvent e) {
            this.orderId = e.getOrderId();
            this.customerName = e.getCustomerName();
            this.totalAmount = e.getTotalAmount();
            this.status = "PENDING";
        } else if (event instanceof OrderStatusChangedEvent e) {
            this.status = e.getNewStatus();
        }
    }

    public List<OrderEvent> getUncommittedEvents() {
        return uncommittedEvents;
    }

    public void clearUncommittedEvents() {
        uncommittedEvents.clear();
    }
}
```

- [ ] **Step 4: Test event sourcing**

```bash
# Run tests
mvn test -pl scm-order/service -Dtest="*EventSourcing*"

# Expected: Tests pass
```

- [ ] **Step 5: Commit**

```bash
git add scm-order/service/src/main/java/com/scmcloud/order/event/
git commit -m "feat: implement event sourcing for orders"
```

---

## Summary

This implementation plan covers the critical tasks from the architecture review:

| Phase | Tasks | Focus |
|-------|-------|-------|
| **Phase 1 (30 days)** | 15 | Foundation: Flyway, CI/CD, HA, Security, Observability |
| **Phase 2 (60 days)** | 5 | Enhancement: Contract testing, E2E, Mutation testing, K8s |
| **Phase 3 (90 days)** | 2 | Resilience: Chaos engineering, DR |
| **Phase 4 (180 days)** | 1 | Optimization: Event sourcing |

**Total: 23 tasks** covering the most critical recommendations.

For the remaining tasks (37+), they can be added as follow-up plans or handled by the engineering team based on priority.

---

## Execution Options

**Plan complete and saved to `docs/superpowers/plans/2026-06-10-architecture-implementation.md`.**

**Two execution options:**

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**

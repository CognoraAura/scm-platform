# PostgreSQL Read Replicas Guide

## Overview

Read replicas provide horizontal scaling for read-heavy workloads by replicating data from the primary database to one or more replica instances.

## Architecture

```
┌─────────────────┐
│   Application   │
└────────┬────────┘
         │
    ┌────┴────┐
    │         │
┌───▼───┐ ┌───▼───┐
│ Write │ │ Read  │
│ Pool  │ │ Pool  │
└───┬───┘ └───┬───┘
    │         │
┌───▼───┐ ┌───▼───┐
│Primary│─│Replica│
│  DB   │ │  DB   │
└───────┘ └───────┘
```

## Configuration

### Spring Boot Application

Add to `application.yml`:
```yaml
spring:
  datasource:
    # Write datasource (primary)
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:db_order}
    username: ${DB_USER:admin}
    password: ${DB_PASSWORD:admin123}

  # Read datasource (replica)
  read-replica:
    url: jdbc:postgresql://${REPLICA_DB_HOST:localhost}:${REPLICA_DB_PORT:5432}/${REPLICA_DB_NAME:db_order}
    username: ${DB_USER:admin}
    password: ${DB_PASSWORD:admin123}
```

### Dynamic Datasource Routing

Use `@Slave` annotation for read queries:
```java
@Service
public class OrderQueryService {

    @Slave
    public List<Order> getOrders(Pageable pageable) {
        // This query will be routed to read replica
        return orderRepository.findAll(pageable);
    }

    public Order createOrder(Order order) {
        // This write goes to primary
        return orderRepository.save(order);
    }
}
```

## Setup

### 1. Configure Primary for Replication

Update `postgresql.conf`:
```
wal_level = replica
max_wal_senders = 3
wal_keep_size = 1GB
hot_standby = on
```

Update `pg_hba.conf`:
```
host    replication     replicator     0.0.0.0/0               md5
```

### 2. Create Replication User

```sql
CREATE USER replicator WITH REPLICATION LOGIN PASSWORD 'replicator_password';
```

### 3. Initialize Replica

```bash
# On replica server
pg_basebackup -h primary-host -U replicator -D /var/lib/postgresql/data -Fp -Xs -P -R
```

## Monitoring

```bash
# Check replication status on primary
SELECT client_addr, state, sent_lsn, write_lsn, replay_lsn
FROM pg_stat_replication;

# Check replication lag
SELECT now() - pg_last_xact_replay_timestamp() AS replication_lag;
```

## Failover

With Patroni:
- Automatic failover on primary failure
- Replica promoted to primary
- Application reconnects to new primary

## Best Practices

1. Use read replicas for analytical queries
2. Monitor replication lag
3. Implement connection pooling per datasource
4. Use `@Slave` annotation consistently

## Last Updated
- Date: 2026-06-12
- Version: 1.0

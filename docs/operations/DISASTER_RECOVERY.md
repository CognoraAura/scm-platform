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

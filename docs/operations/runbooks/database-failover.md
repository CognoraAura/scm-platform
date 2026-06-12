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
   curl http://localhost:9090/api/v1/query?query=rate(http_requests_total{status=~"5.."}[5m])
   ```

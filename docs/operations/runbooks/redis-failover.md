# Redis Failover Runbook

## Automatic Failover (Sentinel)

### Symptoms
- Alert: Redis connection failures
- Alert: Cache miss rate spikes
- Application errors: Cannot connect to Redis

### Steps

1. **Check Sentinel status**
   ```bash
   redis-cli -p 26379 sentinel master scm-master
   ```

2. **Verify current master**
   ```bash
   redis-cli -p 26379 sentinel get-master-addr-by-name scm-master
   ```

3. **Check replicas**
   ```bash
   redis-cli -p 26379 sentinel replicas scm-master
   ```

4. **If automatic failover failed, restart Sentinels**
   ```bash
   docker-compose restart redis-sentinel-1 redis-sentinel-2 redis-sentinel-3
   ```

5. **Monitor failover**
   ```bash
   redis-cli -p 26379 sentinel failover scm-master
   ```

6. **Verify application reconnection**
   ```bash
   curl http://localhost:8203/actuator/health
   ```

# Deployment Rollback Runbook

## Rollback Steps

### Using kubectl

1. **Rollback to previous revision**
   ```bash
   kubectl rollout undo deployment/scm-auth -n scm-prod
   ```

2. **Rollback to specific revision**
   ```bash
   kubectl rollout undo deployment/scm-auth -n scm-prod --to-revision=3
   ```

3. **Verify rollback**
   ```bash
   kubectl rollout status deployment/scm-auth -n scm-prod
   ```

### Using GitHub Actions

1. **Redeploy previous Docker image**
   ```bash
   kubectl set image deployment/scm-auth scm-auth=${{ secrets.DOCKERHUB_USERNAME }}/scm-auth:master-<previous-sha> -n scm-prod
   ```

2. **Monitor rollback**
   ```bash
   kubectl get pods -n scm-prod -l app=scm-auth -w
   ```

## Quick Rollback (git revert + redeploy)

1. Revert the merge commit
2. Push to master
3. CI/CD pipeline automatically builds and deploys

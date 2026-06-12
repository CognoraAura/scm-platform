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

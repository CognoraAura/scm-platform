# Web Application Firewall (WAF) Guide

## Overview

The WAF protects against common web attacks using ModSecurity with OWASP Core Rule Set (CRS).

## Architecture

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│    Client    │────▶│     WAF      │────▶│   Backend    │
│              │     │ ModSecurity  │     │   Service    │
└──────────────┘     │ + OWASP CRS  │     └──────────────┘
                     └──────────────┘
```

## Features

- **SQL Injection Protection**: Detects and blocks SQL injection attempts
- **XSS Protection**: Filters cross-site scripting attacks
- **Remote Code Execution**: Blocks command injection attempts
- **Local File Inclusion**: Prevents path traversal attacks
- **Remote File Inclusion**: Blocks remote file inclusion attempts
- **Protocol Attack**: Detects protocol violations

## Configuration

### ModSecurity Settings

| Setting | Value | Description |
|---------|-------|-------------|
| SecRuleEngine | On | Enable rule processing |
| SecRequestBodyAccess | On | Inspect request bodies |
| SecResponseBodyAccess | Off | Don't inspect responses (API) |
| SecRequestBodyLimit | 13107200 | Max request body (12.5MB) |

### OWASP CRS Settings

| Setting | Value | Description |
|---------|-------|-------------|
| Paranoia Level | 2 | Medium sensitivity |
| Inbound Threshold | 5 | Block at 5 points |
| Outbound Threshold | 4 | Block at 4 points |

## Deployment

### Docker

```bash
# Build WAF image
docker build -t scm-waf:latest -f deploy/waf/Dockerfile .

# Run with backend
docker-compose up -d waf backend
```

### Kubernetes

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: scm-ingress
  annotations:
    nginx.ingress.kubernetes.io/enable-modsecurity: "true"
    nginx.ingress.kubernetes.io/modsecurity-snippet: |
      Include /etc/nginx/modsecurity/modsecurity.conf
spec:
  rules:
    - host: scm.example.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: scm-gateway
                port:
                  number: 8761
```

## Monitoring

### Logs

```bash
# Access logs
tail -f /var/log/modsecurity/modsec_audit.log

# Debug logs (debug level > 0)
tail -f /var/log/modsecurity/modsec_debug.log
```

### Metrics

Key metrics to monitor:
- `modsecurity_rules_matched_total` - Total matched rules
- `modsecurity_request_blocked_total` - Blocked requests
- `modsecurity_anomaly_score_total` - Anomaly scores

### Grafana Dashboard

Import WAF dashboard for visualization:
- Request rate by rule
- Blocked requests by type
- False positive rate

## Tuning

### Adding Exclusions

To exclude a rule for specific endpoint:

```nginx
SecRule REQUEST_URI "@beginsWith /api/v1/orders" \
    "id:2001,phase:1,pass,nolog,ctl:ruleRemoveById=941100"
```

### Adjusting Thresholds

For stricter security:
```
setvar:tx.inbound_anomaly_score_threshold=3
```

For fewer false positives:
```
setvar:tx.inbound_anomaly_score_threshold=8
```

## False Positives

### Common False Positives

| Rule ID | Description | Resolution |
|---------|-------------|------------|
| 941100 | SQL injection | Add exclusion |
| 942100 | SQL injection | Whitelist parameter |
| 941340 | XSS | Encode output |

### Reporting False Positives

1. Document the request causing the false positive
2. Create exclusion rule
3. Test thoroughly
4. Document in runbook

## Best Practices

1. Start with paranoia level 2
2. Monitor false positive rate
3. Review blocked requests daily
4. Keep CRS updated
5. Test rules in detect-only mode first

## Last Updated
- Date: 2026-06-12
- Version: 1.0

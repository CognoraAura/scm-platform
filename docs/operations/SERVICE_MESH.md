# Service Mesh (Istio) Guide

## Overview

Istio service mesh provides:
- **mTLS**: Automatic encryption between services
- **Traffic Management**: Load balancing, retries, timeouts
- **Observability**: Distributed tracing, metrics, logging
- **Security**: Authorization policies, access control

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                    Istio Control Plane              │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐            │
│  │  Pilot  │  │ Citadel │  │  Galley │            │
│  └────┬────┘  └────┬────┘  └────┬────┘            │
└───────┼────────────┼────────────┼──────────────────┘
        │            │            │
┌───────▼────────────▼────────────▼──────────────────┐
│                    Istio Data Plane                 │
│  ┌─────────────────────────────────────────────┐   │
│  │  Envoy Sidecar Proxy (per service pod)     │   │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐   │   │
│  │  │scm-auth │  │scm-order│  │scm-inv  │   │   │
│  │  └─────────┘  └─────────┘  └─────────┘   │   │
│  └─────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘
```

## Setup

### 1. Install Istio

```bash
# Download Istio
curl -L https://istio.io/downloadIstio | sh -
cd istio-*

# Install Istio
istioctl install --set profile=demo -y

# Enable sidecar injection
kubectl label namespace scm-prod istio-injection=enabled
```

### 2. Apply Configuration

```bash
# Apply all Istio configurations
kubectl apply -f deploy/istio/

# Verify mTLS is working
istioctl authn tls-check scm-auth.scm-prod.svc.cluster.local
```

## Traffic Management

### Retries

```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: scm-auth
spec:
  hosts:
    - scm-auth
  http:
    - route:
        - destination:
            host: scm-auth
      retries:
        attempts: 3
        perTryTimeout: 2s
        retryOn: 5xx
```

### Timeouts

```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: scm-order
spec:
  hosts:
    - scm-order
  http:
    - route:
        - destination:
            host: scm-order
      timeout: 10s
```

## Observability

### Kiali Dashboard

```bash
# Access Kiali
istioctl dashboard kiali
```

### Metrics

```bash
# Access Grafana
istioctl dashboard grafana
```

### Tracing

```bash
# Access Jaeger
istioctl dashboard jaeger
```

## Best Practices

1. Enable mTLS STRICT for all services
2. Configure outlier detection for resilience
3. Use circuit breakers for critical paths
4. Monitor with Kiali, Grafana, Jaeger
5. Start with permissive mode, then switch to strict

## Last Updated
- Date: 2026-06-12
- Version: 1.0

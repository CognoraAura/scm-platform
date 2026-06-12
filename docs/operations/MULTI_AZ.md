# Multi-AZ Deployment Guide

## Overview

Multi-AZ deployment distributes workload across multiple Availability Zones (AZs) to provide high availability and fault tolerance.

## Architecture

```
                    ┌─────────────────┐
                    │   Load Balancer  │
                    └────────┬────────┘
                             │
            ┌────────────────┼────────────────┐
            │                │                │
     ┌──────▼──────┐  ┌─────▼──────┐  ┌──────▼──────┐
     │   AZ-1a     │  │   AZ-1b    │  │   AZ-1c     │
     │  ┌───────┐  │  │  ┌───────┐ │  │  ┌───────┐  │
     │  │ Pod 1 │  │  │  │ Pod 2 │ │  │  │ Pod 3 │  │
     │  └───────┘  │  │  └───────┘ │  │  └───────┘  │
     └─────────────┘  └────────────┘  └─────────────┘
```

## Configuration

### Topology Spread Constraints

Ensures even distribution across AZs:
- `maxSkew: 1` - Maximum difference in pod count between AZs
- `topologyKey: topology.kubernetes.io/zone` - Distribute by AZ
- `whenUnsatisfiable: DoNotSchedule` - Don't schedule if constraint can't be met

### Pod Anti-Affinity

Prevents multiple pods of same service on same node:
- `preferredDuringSchedulingIgnoredDuringExecution` - Soft preference
- `weight: 100` - Highest priority

## Prerequisites

1. Kubernetes cluster with nodes in multiple AZs
2. Nodes labeled with `topology.kubernetes.io/zone`
3. Storage classes configured for each AZ

## Deployment

```bash
# Apply topology spread constraints
kubectl apply -f deploy/k8s/multi-az topology-spread.yaml

# Verify pod distribution across AZs
kubectl get pods -n scm-prod -o wide | awk '{print $7}' | sort | uniq -c
```

## Monitoring

```bash
# Check pod distribution
kubectl get pods -n scm-prod -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.spec.nodeName}{"\n"}{end}'

# Verify AZ labels
kubectl get nodes --show-labels | grep topology.kubernetes.io/zone
```

## Failover

When an AZ fails:
1. Automatic pod rescheduling to remaining AZs
2. Topology spread constraints ensure new pods are placed correctly
3. Load balancer routes traffic to healthy AZs

## Last Updated
- Date: 2026-06-12
- Version: 1.0

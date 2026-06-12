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

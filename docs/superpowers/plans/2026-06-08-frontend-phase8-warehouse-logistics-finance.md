# Frontend Phase 8: Warehouse, Logistics & Finance Modules

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build warehouse, logistics, and finance management modules with CRUD pages.

**Architecture:** Feature module pattern. Mock data for development.

**Tech Stack:** Next.js 15, React 19, Ant Design 5, TanStack Query 5

---

## Tasks

### Task 1: Warehouse Module

Create `scm-web/src/features/warehouse/` with:
- types/warehouse.types.ts
- services/warehouse.service.ts
- hooks/use-warehouse.ts
- components/warehouse-list.tsx
- components/inbound-list.tsx
- components/outbound-list.tsx
- barrel exports + pages

### Task 2: Logistics Module

Create `scm-web/src/features/logistics/` with:
- types/logistics.types.ts
- services/logistics.service.ts
- hooks/use-logistics.ts
- components/waybill-list.tsx
- components/tracking-timeline.tsx
- barrel exports + pages

### Task 3: Finance Module

Create `scm-web/src/features/finance/` with:
- types/finance.types.ts
- services/finance.service.ts
- hooks/use-finance.ts
- components/settlement-list.tsx
- components/invoice-list.tsx
- barrel exports + pages

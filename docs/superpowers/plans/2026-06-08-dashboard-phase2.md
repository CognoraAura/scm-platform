# Dashboard Phase 2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create dashboard types, mock service, hooks, and KPI cards component for the SCM platform dashboard.

**Architecture:** Feature-based structure under `src/features/dashboard/` with types, services, hooks, and components directories. Uses Ant Design for UI, TanStack Query for data fetching.

**Tech Stack:** Next.js 15, React 19, Ant Design 5, TanStack Query 5, TypeScript 5.8

---

## File Structure

```
scm-web/src/features/dashboard/
├── types/
│   ├── dashboard.types.ts    # All dashboard type definitions
│   └── index.ts              # Re-exports
├── services/
│   └── dashboard.service.ts  # Mock data service
├── hooks/
│   ├── use-dashboard-stats.ts # TanStack Query hook
│   └── index.ts              # Re-exports
└── components/
    └── kpi-cards.tsx         # KPI cards component
```

---

### Task 1: Create Dashboard Types

**Files:**
- Create: `scm-web/src/features/dashboard/types/dashboard.types.ts`
- Create: `scm-web/src/features/dashboard/types/index.ts`

- [ ] **Step 1: Create types directory and dashboard.types.ts**

Create `scm-web/src/features/dashboard/types/dashboard.types.ts`:

```typescript
export interface KPIData {
  title: string
  value: number | string
  prefix?: string
  suffix?: string
  trend?: {
    value: number
    direction: 'up' | 'down' | 'flat'
    period: string
  }
  icon: string
  color: string
}

export interface SalesTrendData {
  dates: string[]
  series: Array<{
    name: string
    data: number[]
    color?: string
  }>
}

export interface OrderStatusData {
  status: string
  count: number
  color: string
}

export interface RecentOrder {
  id: string
  orderNo: string
  customerName: string
  amount: number
  status: string
  statusLabel: string
  createdAt: string
}

export interface InventoryAlert {
  id: string
  productName: string
  skuCode: string
  currentStock: number
  threshold: number
  status: 'low' | 'critical' | 'out'
}

export interface DashboardStats {
  kpis: KPIData[]
  salesTrend: SalesTrendData
  orderStatus: OrderStatusData[]
  recentOrders: RecentOrder[]
  inventoryAlerts: InventoryAlert[]
}
```

- [ ] **Step 2: Create index.ts**

Create `scm-web/src/features/dashboard/types/index.ts`:

```typescript
export * from './dashboard.types'
```

- [ ] **Step 3: Verify types compile**

Run: `npx tsc --noEmit`
Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add scm-web/src/features/dashboard/types/
git commit -m "feat(frontend): add dashboard types"
```

---

### Task 2: Create Dashboard Service and Hooks

**Files:**
- Create: `scm-web/src/features/dashboard/services/dashboard.service.ts`
- Create: `scm-web/src/features/dashboard/hooks/use-dashboard-stats.ts`
- Create: `scm-web/src/features/dashboard/hooks/index.ts`

- [ ] **Step 1: Create dashboard.service.ts**

Create `scm-web/src/features/dashboard/services/dashboard.service.ts`:

```typescript
import type { DashboardStats, KPIData } from '../types'

const mockKPIs: KPIData[] = [
  { title: '总营收', value: '¥128,500', trend: { value: 12.5, direction: 'up', period: '较上月' }, icon: 'DollarOutlined', color: '#1677ff' },
  { title: '订单数', value: '1,234', trend: { value: 8.3, direction: 'up', period: '较上月' }, icon: 'ShoppingCartOutlined', color: '#52c41a' },
  { title: '库存量', value: '5,678', trend: { value: 2.1, direction: 'down', period: '较上月' }, icon: 'InboxOutlined', color: '#faad14' },
  { title: '供应商', value: '89', trend: { value: 5, direction: 'up', period: '较上月' }, icon: 'TeamOutlined', color: '#722ed1' },
]

export const dashboardService = {
  getStats: async (): Promise<DashboardStats> => {
    return {
      kpis: mockKPIs,
      salesTrend: {
        dates: ['06-01', '06-02', '06-03', '06-04', '06-05', '06-06', '06-07'],
        series: [
          { name: '销售额', data: [1200, 1500, 1800, 1400, 2100, 1900, 2300], color: '#1677ff' },
          { name: '订单量', data: [80, 95, 120, 88, 140, 125, 155], color: '#52c41a' },
        ],
      },
      orderStatus: [
        { status: '待付款', count: 45, color: '#faad14' },
        { status: '已付款', count: 120, color: '#1677ff' },
        { status: '已发货', count: 85, color: '#722ed1' },
        { status: '已完成', count: 200, color: '#52c41a' },
        { status: '已取消', count: 15, color: '#ff4d4f' },
      ],
      recentOrders: [
        { id: '1', orderNo: 'ORD-2026-001', customerName: '张三', amount: 1280, status: 'PAID', statusLabel: '已付款', createdAt: '2026-06-08 14:30' },
        { id: '2', orderNo: 'ORD-2026-002', customerName: '李四', amount: 2560, status: 'PENDING', statusLabel: '待付款', createdAt: '2026-06-08 13:15' },
        { id: '3', orderNo: 'ORD-2026-003', customerName: '王五', amount: 890, status: 'SHIPPED', statusLabel: '已发货', createdAt: '2026-06-08 11:20' },
        { id: '4', orderNo: 'ORD-2026-004', customerName: '赵六', amount: 3200, status: 'COMPLETED', statusLabel: '已完成', createdAt: '2026-06-08 10:05' },
        { id: '5', orderNo: 'ORD-2026-005', customerName: '钱七', amount: 450, status: 'CANCELLED', statusLabel: '已取消', createdAt: '2026-06-08 09:30' },
      ],
      inventoryAlerts: [
        { id: '1', productName: 'iPhone 15 Pro', skuCode: 'SKU-001', currentStock: 5, threshold: 20, status: 'critical' },
        { id: '2', productName: 'MacBook Air M3', skuCode: 'SKU-002', currentStock: 12, threshold: 30, status: 'low' },
        { id: '3', productName: 'AirPods Pro 2', skuCode: 'SKU-003', currentStock: 0, threshold: 50, status: 'out' },
        { id: '4', productName: 'iPad Air', skuCode: 'SKU-004', currentStock: 8, threshold: 25, status: 'low' },
      ],
    }
  },
}
```

- [ ] **Step 2: Create use-dashboard-stats.ts**

Create `scm-web/src/features/dashboard/hooks/use-dashboard-stats.ts`:

```typescript
'use client'

import { useQuery } from '@tanstack/react-query'
import { dashboardService } from '../services/dashboard.service'

export function useDashboardStats() {
  return useQuery({
    queryKey: ['dashboard', 'stats'],
    queryFn: () => dashboardService.getStats(),
    staleTime: 60 * 1000,
    refetchInterval: 5 * 60 * 1000,
  })
}
```

- [ ] **Step 3: Create hooks/index.ts**

Create `scm-web/src/features/dashboard/hooks/index.ts`:

```typescript
export { useDashboardStats } from './use-dashboard-stats'
```

- [ ] **Step 4: Verify types compile**

Run: `npx tsc --noEmit`
Expected: No errors

- [ ] **Step 5: Commit**

```bash
git add scm-web/src/features/dashboard/services/ scm-web/src/features/dashboard/hooks/
git commit -m "feat(frontend): add dashboard service and hooks"
```

---

### Task 3: Create KPI Cards Component

**Files:**
- Create: `scm-web/src/features/dashboard/components/kpi-cards.tsx`

- [ ] **Step 1: Create kpi-cards.tsx**

Create `scm-web/src/features/dashboard/components/kpi-cards.tsx`:

```tsx
'use client'

import { Row, Col, Card, Statistic } from 'antd'
import { ArrowUpOutlined, ArrowDownOutlined, DollarOutlined, ShoppingCartOutlined, InboxOutlined, TeamOutlined } from '@ant-design/icons'
import type { KPIData } from '../types'

const iconMap: Record<string, React.ReactNode> = {
  DollarOutlined: <DollarOutlined />,
  ShoppingCartOutlined: <ShoppingCartOutlined />,
  InboxOutlined: <InboxOutlined />,
  TeamOutlined: <TeamOutlined />,
}

interface KPICardsProps {
  kpis: KPIData[]
  loading?: boolean
}

export default function KPICards({ kpis, loading }: KPICardsProps) {
  return (
    <Row gutter={[16, 16]}>
      {kpis.map((kpi, index) => (
        <Col key={index} xs={24} sm={12} lg={6}>
          <Card loading={loading}>
            <Statistic
              title={kpi.title}
              value={kpi.value}
              prefix={iconMap[kpi.icon]}
              suffix={
                kpi.trend && (
                  <span style={{ fontSize: 12, color: kpi.trend.direction === 'up' ? '#52c41a' : kpi.trend.direction === 'down' ? '#ff4d4f' : '#8c8c8c' }}>
                    {kpi.trend.direction === 'up' ? <ArrowUpOutlined /> : kpi.trend.direction === 'down' ? <ArrowDownOutlined /> : null}{' '}
                    {kpi.trend.value}% {kpi.trend.period}
                  </span>
                )
              }
            />
          </Card>
        </Col>
      ))}
    </Row>
  )
}
```

- [ ] **Step 2: Verify types compile**

Run: `npx tsc --noEmit`
Expected: No errors

- [ ] **Step 3: Commit**

```bash
git add scm-web/src/features/dashboard/components/
git commit -m "feat(frontend): add KPI cards component"
```

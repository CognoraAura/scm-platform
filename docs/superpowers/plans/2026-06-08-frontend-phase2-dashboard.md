# Frontend Phase 2: Dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the dashboard page with KPI cards, charts (sales trend, order status), data panels (recent orders, inventory alerts), and proper loading/error states.

**Architecture:** Dashboard uses feature-based module structure. Types define KPI/chart data shapes. Service wraps API calls. Hooks use TanStack Query for caching. Components are composed in the dashboard page with responsive grid layout.

**Tech Stack:** Next.js 15, React 19, Ant Design 5, ECharts, TanStack Query 5

---

## File Structure

### Files to Create

| File | Responsibility |
|------|---------------|
| `src/features/dashboard/types/dashboard.types.ts` | KPI, ChartData, Widget types |
| `src/features/dashboard/services/dashboard.service.ts` | Dashboard API calls |
| `src/features/dashboard/hooks/use-dashboard-stats.ts` | Dashboard statistics hook |
| `src/features/dashboard/hooks/use-kpi-data.ts` | KPI metrics hook |
| `src/features/dashboard/hooks/index.ts` | Barrel export |
| `src/features/dashboard/components/kpi-cards.tsx` | KPI cards row |
| `src/features/dashboard/components/sales-chart.tsx` | Sales trend line chart |
| `src/features/dashboard/components/order-status-chart.tsx` | Order status pie chart |
| `src/features/dashboard/components/recent-orders.tsx` | Recent orders table |
| `src/features/dashboard/components/inventory-alerts.tsx` | Low stock alerts list |
| `src/features/dashboard/components/index.ts` | Barrel export |
| `src/features/dashboard/index.ts` | Module barrel export |

### Files to Modify

| File | Changes |
|------|---------|
| `src/app/[locale]/(app)/dashboard/page.tsx` | Compose dashboard components |

---

## Tasks

### Task 1: Create Dashboard Types

**Files:**
- Create: `scm-web/src/features/dashboard/types/dashboard.types.ts`
- Create: `scm-web/src/features/dashboard/types/index.ts`

- [ ] **Step 1: Create dashboard.types.ts**

```typescript
// scm-web/src/features/dashboard/types/dashboard.types.ts

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

export interface ChartDataPoint {
  date: string
  value: number
  category?: string
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

- [ ] **Step 2: Create types barrel export**

```typescript
// scm-web/src/features/dashboard/types/index.ts
export * from './dashboard.types'
```

- [ ] **Step 3: Verify compilation**

Run: `cd scm-web && npx tsc --noEmit`
Expected: No new errors

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

```typescript
// scm-web/src/features/dashboard/services/dashboard.service.ts
import apiClient from '@/lib/api/client'
import type { DashboardStats, KPIData, RecentOrder, InventoryAlert } from '../types'

// Mock data for development (replace with real API calls when backend is ready)
const mockKPIs: KPIData[] = [
  {
    title: '总营收',
    value: '¥128,500',
    trend: { value: 12.5, direction: 'up', period: '较上月' },
    icon: 'DollarOutlined',
    color: '#1677ff',
  },
  {
    title: '订单数',
    value: '1,234',
    trend: { value: 8.3, direction: 'up', period: '较上月' },
    icon: 'ShoppingCartOutlined',
    color: '#52c41a',
  },
  {
    title: '库存量',
    value: '5,678',
    trend: { value: 2.1, direction: 'down', period: '较上月' },
    icon: 'InboxOutlined',
    color: '#faad14',
  },
  {
    title: '供应商',
    value: '89',
    trend: { value: 5, direction: 'up', period: '较上月' },
    icon: 'TeamOutlined',
    color: '#722ed1',
  },
]

const mockRecentOrders: RecentOrder[] = [
  { id: '1', orderNo: 'ORD-2026-001', customerName: '张三', amount: 1280, status: 'PAID', statusLabel: '已付款', createdAt: '2026-06-08 14:30' },
  { id: '2', orderNo: 'ORD-2026-002', customerName: '李四', amount: 2560, status: 'PENDING', statusLabel: '待付款', createdAt: '2026-06-08 13:15' },
  { id: '3', orderNo: 'ORD-2026-003', customerName: '王五', amount: 890, status: 'SHIPPED', statusLabel: '已发货', createdAt: '2026-06-08 11:20' },
  { id: '4', orderNo: 'ORD-2026-004', customerName: '赵六', amount: 3200, status: 'COMPLETED', statusLabel: '已完成', createdAt: '2026-06-08 10:05' },
  { id: '5', orderNo: 'ORD-2026-005', customerName: '钱七', amount: 450, status: 'CANCELLED', statusLabel: '已取消', createdAt: '2026-06-08 09:30' },
]

const mockInventoryAlerts: InventoryAlert[] = [
  { id: '1', productName: 'iPhone 15 Pro', skuCode: 'SKU-001', currentStock: 5, threshold: 20, status: 'critical' },
  { id: '2', productName: 'MacBook Air M3', skuCode: 'SKU-002', currentStock: 12, threshold: 30, status: 'low' },
  { id: '3', productName: 'AirPods Pro 2', skuCode: 'SKU-003', currentStock: 0, threshold: 50, status: 'out' },
  { id: '4', productName: 'iPad Air', skuCode: 'SKU-004', currentStock: 8, threshold: 25, status: 'low' },
]

export const dashboardService = {
  getStats: async (): Promise<DashboardStats> => {
    // TODO: Replace with real API call
    // return apiClient.get('/api/dashboard/stats')
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
      recentOrders: mockRecentOrders,
      inventoryAlerts: mockInventoryAlerts,
    }
  },
}
```

- [ ] **Step 2: Create use-dashboard-stats.ts**

```typescript
// scm-web/src/features/dashboard/hooks/use-dashboard-stats.ts
'use client'

import { useQuery } from '@tanstack/react-query'
import { dashboardService } from '../services/dashboard.service'

export function useDashboardStats() {
  return useQuery({
    queryKey: ['dashboard', 'stats'],
    queryFn: () => dashboardService.getStats(),
    staleTime: 60 * 1000, // 1 minute
    refetchInterval: 5 * 60 * 1000, // 5 minutes
  })
}
```

- [ ] **Step 3: Create hooks barrel export**

```typescript
// scm-web/src/features/dashboard/hooks/index.ts
export { useDashboardStats } from './use-dashboard-stats'
```

- [ ] **Step 4: Verify compilation**

Run: `cd scm-web && npx tsc --noEmit`
Expected: No new errors

- [ ] **Step 5: Commit**

```bash
git add scm-web/src/features/dashboard/
git commit -m "feat(frontend): add dashboard service and hooks"
```

---

### Task 3: Create KPI Cards Component

**Files:**
- Create: `scm-web/src/features/dashboard/components/kpi-cards.tsx`

- [ ] **Step 1: Create kpi-cards.tsx**

```tsx
// scm-web/src/features/dashboard/components/kpi-cards.tsx
'use client'

import { Row, Col, Card, Statistic } from 'antd'
import {
  ArrowUpOutlined,
  ArrowDownOutlined,
  DollarOutlined,
  ShoppingCartOutlined,
  InboxOutlined,
  TeamOutlined,
} from '@ant-design/icons'
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
                  <span
                    style={{
                      fontSize: 12,
                      color:
                        kpi.trend.direction === 'up'
                          ? '#52c41a'
                          : kpi.trend.direction === 'down'
                          ? '#ff4d4f'
                          : '#8c8c8c',
                    }}
                  >
                    {kpi.trend.direction === 'up' ? (
                      <ArrowUpOutlined />
                    ) : kpi.trend.direction === 'down' ? (
                      <ArrowDownOutlined />
                    ) : null}{' '}
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

- [ ] **Step 2: Verify compilation**

Run: `cd scm-web && npx tsc --noEmit`
Expected: No new errors

- [ ] **Step 3: Commit**

```bash
git add scm-web/src/features/dashboard/components/kpi-cards.tsx
git commit -m "feat(frontend): add KPI cards component"
```

---

### Task 4: Create Chart Components

**Files:**
- Create: `scm-web/src/features/dashboard/components/sales-chart.tsx`
- Create: `scm-web/src/features/dashboard/components/order-status-chart.tsx`

- [ ] **Step 1: Create sales-chart.tsx**

```tsx
// scm-web/src/features/dashboard/components/sales-chart.tsx
'use client'

import dynamic from 'next/dynamic'
import { Card } from 'antd'
import type { SalesTrendData } from '../types'

const ReactECharts = dynamic(() => import('echarts-for-react'), { ssr: false })

interface SalesChartProps {
  data: SalesTrendData
  loading?: boolean
}

export default function SalesChart({ data, loading }: SalesChartProps) {
  const option = {
    tooltip: {
      trigger: 'axis' as const,
    },
    legend: {
      data: data.series.map((s) => s.name),
      bottom: 0,
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '10%',
      containLabel: true,
    },
    xAxis: {
      type: 'category' as const,
      boundaryGap: false,
      data: data.dates,
    },
    yAxis: {
      type: 'value' as const,
    },
    series: data.series.map((s) => ({
      name: s.name,
      type: 'line',
      smooth: true,
      data: s.data,
      itemStyle: { color: s.color },
      areaStyle: {
        color: {
          type: 'linear' as const,
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: `${s.color}33` },
            { offset: 1, color: `${s.color}05` },
          ],
        },
      },
    })),
  }

  return (
    <Card title="销售趋势" loading={loading}>
      <ReactECharts option={option} style={{ height: 300 }} />
    </Card>
  )
}
```

- [ ] **Step 2: Create order-status-chart.tsx**

```tsx
// scm-web/src/features/dashboard/components/order-status-chart.tsx
'use client'

import dynamic from 'next/dynamic'
import { Card } from 'antd'
import type { OrderStatusData } from '../types'

const ReactECharts = dynamic(() => import('echarts-for-react'), { ssr: false })

interface OrderStatusChartProps {
  data: OrderStatusData[]
  loading?: boolean
}

export default function OrderStatusChart({ data, loading }: OrderStatusChartProps) {
  const option = {
    tooltip: {
      trigger: 'item' as const,
      formatter: '{b}: {c} ({d}%)',
    },
    legend: {
      bottom: 0,
      data: data.map((d) => d.status),
    },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2,
        },
        label: {
          show: false,
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold',
          },
        },
        data: data.map((d) => ({
          name: d.status,
          value: d.count,
          itemStyle: { color: d.color },
        })),
      },
    ],
  }

  return (
    <Card title="订单状态分布" loading={loading}>
      <ReactECharts option={option} style={{ height: 300 }} />
    </Card>
  )
}
```

- [ ] **Step 3: Verify compilation**

Run: `cd scm-web && npx tsc --noEmit`
Expected: No new errors

- [ ] **Step 4: Commit**

```bash
git add scm-web/src/features/dashboard/components/sales-chart.tsx scm-web/src/features/dashboard/components/order-status-chart.tsx
git commit -m "feat(frontend): add sales trend and order status charts"
```

---

### Task 5: Create Data Panel Components

**Files:**
- Create: `scm-web/src/features/dashboard/components/recent-orders.tsx`
- Create: `scm-web/src/features/dashboard/components/inventory-alerts.tsx`

- [ ] **Step 1: Create recent-orders.tsx**

```tsx
// scm-web/src/features/dashboard/components/recent-orders.tsx
'use client'

import { Card, Table, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { RecentOrder } from '../types'

interface RecentOrdersProps {
  orders: RecentOrder[]
  loading?: boolean
}

const statusColors: Record<string, string> = {
  PENDING: 'warning',
  PAID: 'processing',
  SHIPPED: 'purple',
  COMPLETED: 'success',
  CANCELLED: 'error',
}

const columns: ColumnsType<RecentOrder> = [
  {
    title: '订单号',
    dataIndex: 'orderNo',
    key: 'orderNo',
    render: (text: string) => <a>{text}</a>,
  },
  {
    title: '客户',
    dataIndex: 'customerName',
    key: 'customerName',
  },
  {
    title: '金额',
    dataIndex: 'amount',
    key: 'amount',
    align: 'right',
    render: (val: number) => `¥${val.toLocaleString()}`,
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    render: (status: string, record) => (
      <Tag color={statusColors[status] || 'default'}>{record.statusLabel}</Tag>
    ),
  },
  {
    title: '时间',
    dataIndex: 'createdAt',
    key: 'createdAt',
  },
]

export default function RecentOrders({ orders, loading }: RecentOrdersProps) {
  return (
    <Card title="最近订单">
      <Table
        columns={columns}
        dataSource={orders}
        loading={loading}
        rowKey="id"
        pagination={false}
        size="small"
      />
    </Card>
  )
}
```

- [ ] **Step 2: Create inventory-alerts.tsx**

```tsx
// scm-web/src/features/dashboard/components/inventory-alerts.tsx
'use client'

import { Card, List, Tag, Progress } from 'antd'
import { WarningOutlined } from '@ant-design/icons'
import type { InventoryAlert } from '../types'

interface InventoryAlertsProps {
  alerts: InventoryAlert[]
  loading?: boolean
}

const statusConfig: Record<string, { color: string; label: string }> = {
  critical: { color: '#ff4d4f', label: '紧急' },
  low: { color: '#faad14', label: '偏低' },
  out: { color: '#8c8c8c', label: '缺货' },
}

export default function InventoryAlerts({ alerts, loading }: InventoryAlertsProps) {
  return (
    <Card title="库存预警" extra={<WarningOutlined style={{ color: '#faad14' }} />}>
      <List
        loading={loading}
        dataSource={alerts}
        renderItem={(item) => {
          const config = statusConfig[item.status]
          const percent = Math.round((item.currentStock / item.threshold) * 100)
          return (
            <List.Item>
              <List.Item.Meta
                title={
                  <span>
                    {item.productName}{' '}
                    <Tag color={config.color}>{config.label}</Tag>
                  </span>
                }
                description={
                  <span style={{ fontSize: 12, color: '#8c8c8c' }}>
                    {item.skuCode} · 库存 {item.currentStock} / 阈值 {item.threshold}
                  </span>
                }
              />
              <Progress
                percent={Math.min(percent, 100)}
                size="small"
                status={item.status === 'out' ? 'exception' : item.status === 'critical' ? 'exception' : 'active'}
                style={{ width: 100 }}
              />
            </List.Item>
          )
        }}
      />
    </Card>
  )
}
```

- [ ] **Step 3: Verify compilation**

Run: `cd scm-web && npx tsc --noEmit`
Expected: No new errors

- [ ] **Step 4: Commit**

```bash
git add scm-web/src/features/dashboard/components/recent-orders.tsx scm-web/src/features/dashboard/components/inventory-alerts.tsx
git commit -m "feat(frontend): add recent orders and inventory alerts panels"
```

---

### Task 6: Create Component Barrel Export and Module Export

**Files:**
- Create: `scm-web/src/features/dashboard/components/index.ts`
- Create: `scm-web/src/features/dashboard/index.ts`

- [ ] **Step 1: Create components barrel export**

```typescript
// scm-web/src/features/dashboard/components/index.ts
export { default as KPICards } from './kpi-cards'
export { default as SalesChart } from './sales-chart'
export { default as OrderStatusChart } from './order-status-chart'
export { default as RecentOrders } from './recent-orders'
export { default as InventoryAlerts } from './inventory-alerts'
```

- [ ] **Step 2: Create module barrel export**

```typescript
// scm-web/src/features/dashboard/index.ts
export * from './types'
export * from './hooks'
export * from './components'
```

- [ ] **Step 3: Verify compilation**

Run: `cd scm-web && npx tsc --noEmit`
Expected: No new errors

- [ ] **Step 4: Commit**

```bash
git add scm-web/src/features/dashboard/components/index.ts scm-web/src/features/dashboard/index.ts
git commit -m "feat(frontend): add dashboard barrel exports"
```

---

### Task 7: Update Dashboard Page

**Files:**
- Modify: `scm-web/src/app/[locale]/(app)/dashboard/page.tsx`

- [ ] **Step 1: Replace dashboard page with composed components**

```tsx
// scm-web/src/app/[locale]/(app)/dashboard/page.tsx
'use client'

import { Row, Col } from 'antd'
import { useDashboardStats } from '@/features/dashboard/hooks'
import KPICards from '@/features/dashboard/components/kpi-cards'
import SalesChart from '@/features/dashboard/components/sales-chart'
import OrderStatusChart from '@/features/dashboard/components/order-status-chart'
import RecentOrders from '@/features/dashboard/components/recent-orders'
import InventoryAlerts from '@/features/dashboard/components/inventory-alerts'

export default function DashboardPage() {
  const { data, isLoading } = useDashboardStats()

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <KPICards kpis={data?.kpis || []} loading={isLoading} />

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={16}>
          <SalesChart data={data?.salesTrend || { dates: [], series: [] }} loading={isLoading} />
        </Col>
        <Col xs={24} lg={8}>
          <OrderStatusChart data={data?.orderStatus || []} loading={isLoading} />
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <RecentOrders orders={data?.recentOrders || []} loading={isLoading} />
        </Col>
        <Col xs={24} lg={12}>
          <InventoryAlerts alerts={data?.inventoryAlerts || []} loading={isLoading} />
        </Col>
      </Row>
    </div>
  )
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd scm-web && npx tsc --noEmit`
Expected: No new errors

- [ ] **Step 3: Commit**

```bash
git add scm-web/src/app/\[locale\]/\(app\)/dashboard/page.tsx
git commit -m "feat(frontend): compose dashboard page with KPIs, charts, and panels"
```

---

### Task 8: Verify Full Build

**Files:**
- None (verification only)

- [ ] **Step 1: Run TypeScript type check**

Run: `cd scm-web && npx tsc --noEmit`
Expected: No new errors

- [ ] **Step 2: Run build**

Run: `cd scm-web && npm run build`
Expected: Build succeeds

- [ ] **Step 3: Commit**

```bash
git add scm-web/
git commit -m "chore(frontend): verify Phase 2 dashboard build"
```

---

## Summary

| Task | Files Created | Files Modified |
|------|--------------|----------------|
| 1. Dashboard Types | 2 | 0 |
| 2. Service and Hooks | 3 | 0 |
| 3. KPI Cards | 1 | 0 |
| 4. Chart Components | 2 | 0 |
| 5. Data Panels | 2 | 0 |
| 6. Barrel Exports | 2 | 0 |
| 7. Dashboard Page | 0 | 1 |
| 8. Verify Build | 0 | 0 |
| **Total** | **12** | **1** |

**Phase 2 Deliverables:**
- Dashboard types (KPI, chart, order, inventory data shapes)
- Dashboard service with mock data (ready for real API)
- Dashboard stats hook with TanStack Query
- KPI cards with trend indicators
- Sales trend line chart (ECharts)
- Order status donut chart (ECharts)
- Recent orders table
- Inventory alerts list with progress bars
- Composed dashboard page with responsive grid

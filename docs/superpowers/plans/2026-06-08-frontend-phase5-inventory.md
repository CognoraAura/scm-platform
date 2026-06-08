# Frontend Phase 5: Inventory Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build inventory management module with stock list, stock alerts, and stock adjustment functionality.

**Architecture:** Feature module pattern with types, service, hooks, components, pages. Uses mock data for development.

**Tech Stack:** Next.js 15, React 19, Ant Design 5, TanStack Query 5

---

## Tasks

### Task 1: Create Inventory Types, Service, and Hooks

**Files:**
- Create: `scm-web/src/features/inventory/types/inventory.types.ts`
- Create: `scm-web/src/features/inventory/types/index.ts`
- Create: `scm-web/src/features/inventory/services/inventory.service.ts`
- Create: `scm-web/src/features/inventory/services/index.ts`
- Create: `scm-web/src/features/inventory/hooks/use-inventory.ts`
- Create: `scm-web/src/features/inventory/hooks/index.ts`

- [ ] **Step 1: Create inventory.types.ts**

```typescript
export interface InventoryItem {
  id: string
  skuId: string
  skuCode: string
  productName: string
  warehouseId: string
  warehouseName: string
  totalStock: number
  availableStock: number
  reservedStock: number
  threshold: number
  status: 'normal' | 'low' | 'critical' | 'out'
  lastUpdatedAt: string
}

export interface StockMovement {
  id: string
  skuId: string
  skuCode: string
  productName: string
  type: 'inbound' | 'outbound' | 'adjust' | 'transfer'
  quantity: number
  beforeStock: number
  afterStock: number
  reason: string
  operator: string
  createdAt: string
}

export interface StockAlert {
  id: string
  skuId: string
  skuCode: string
  productName: string
  currentStock: number
  threshold: number
  status: 'low' | 'critical' | 'out'
  warehouseName: string
}

export interface InventorySearchParams {
  current?: number
  pageSize?: number
  keyword?: string
  warehouseId?: string
  status?: string
  lowStock?: boolean
}
```

- [ ] **Step 2: Create inventory.service.ts**

```typescript
import type { InventoryItem, StockMovement, StockAlert, InventorySearchParams } from '../types'

const mockInventory: InventoryItem[] = [
  { id: '1', skuId: '11', skuCode: 'SKU-001-128', productName: 'iPhone 15 Pro 128G', warehouseId: '1', warehouseName: '主仓库', totalStock: 50, availableStock: 45, reservedStock: 5, threshold: 20, status: 'normal', lastUpdatedAt: '2026-06-08 14:30' },
  { id: '2', skuId: '12', skuCode: 'SKU-001-256', productName: 'iPhone 15 Pro 256G', warehouseId: '1', warehouseName: '主仓库', totalStock: 12, availableStock: 10, reservedStock: 2, threshold: 20, status: 'low', lastUpdatedAt: '2026-06-08 13:15' },
  { id: '3', skuId: '21', skuCode: 'SKU-002-256', productName: 'MacBook Air M3 256G', warehouseId: '1', warehouseName: '主仓库', totalStock: 5, availableStock: 3, reservedStock: 2, threshold: 15, status: 'critical', lastUpdatedAt: '2026-06-08 12:00' },
  { id: '4', skuId: '31', skuCode: 'SKU-003', productName: 'AirPods Pro 2', warehouseId: '1', warehouseName: '主仓库', totalStock: 0, availableStock: 0, reservedStock: 0, threshold: 50, status: 'out', lastUpdatedAt: '2026-06-08 10:00' },
  { id: '5', skuId: '11', skuCode: 'SKU-001-128', productName: 'iPhone 15 Pro 128G', warehouseId: '2', warehouseName: '华东仓', totalStock: 30, availableStock: 28, reservedStock: 2, threshold: 20, status: 'normal', lastUpdatedAt: '2026-06-08 11:00' },
]

const mockMovements: StockMovement[] = [
  { id: '1', skuId: '11', skuCode: 'SKU-001-128', productName: 'iPhone 15 Pro 128G', type: 'inbound', quantity: 100, beforeStock: 0, afterStock: 100, reason: '采购入库', operator: '管理员', createdAt: '2026-06-01 10:00' },
  { id: '2', skuId: '11', skuCode: 'SKU-001-128', productName: 'iPhone 15 Pro 128G', type: 'outbound', quantity: 30, beforeStock: 100, afterStock: 70, reason: '销售出库', operator: '张三', createdAt: '2026-06-05 14:30' },
  { id: '3', skuId: '11', skuCode: 'SKU-001-128', productName: 'iPhone 15 Pro 128G', type: 'adjust', quantity: -20, beforeStock: 70, afterStock: 50, reason: '盘点调整', operator: '管理员', createdAt: '2026-06-08 09:00' },
]

const mockAlerts: StockAlert[] = [
  { id: '2', skuId: '12', skuCode: 'SKU-001-256', productName: 'iPhone 15 Pro 256G', currentStock: 12, threshold: 20, status: 'low', warehouseName: '主仓库' },
  { id: '3', skuId: '21', skuCode: 'SKU-002-256', productName: 'MacBook Air M3 256G', currentStock: 5, threshold: 15, status: 'critical', warehouseName: '主仓库' },
  { id: '4', skuId: '31', skuCode: 'SKU-003', productName: 'AirPods Pro 2', currentStock: 0, threshold: 50, status: 'out', warehouseName: '主仓库' },
]

export const inventoryService = {
  list: async (params?: InventorySearchParams) => {
    let filtered = [...mockInventory]
    if (params?.keyword) filtered = filtered.filter(i => i.productName.includes(params.keyword!) || i.skuCode.includes(params.keyword!))
    if (params?.status) filtered = filtered.filter(i => i.status === params.status)
    if (params?.lowStock) filtered = filtered.filter(i => i.status !== 'normal')
    const start = ((params?.current || 1) - 1) * (params?.pageSize || 20)
    return { records: filtered.slice(start, start + (params?.pageSize || 20)), total: filtered.length, current: params?.current || 1, pageSize: params?.pageSize || 20 }
  },
  getAlerts: async (): Promise<StockAlert[]> => mockAlerts,
  getMovements: async (skuId?: string): Promise<StockMovement[]> => {
    if (skuId) return mockMovements.filter(m => m.skuId === skuId)
    return mockMovements
  },
  adjustStock: async (skuId: string, quantity: number, reason: string): Promise<void> => {},
}
```

- [ ] **Step 3: Create use-inventory.ts**

```typescript
'use client'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { inventoryService } from '../services/inventory.service'
import type { InventorySearchParams } from '../types'

export function useInventoryList(params?: InventorySearchParams) {
  return useQuery({ queryKey: ['inventory', 'list', params], queryFn: () => inventoryService.list(params) })
}
export function useStockAlerts() {
  return useQuery({ queryKey: ['inventory', 'alerts'], queryFn: () => inventoryService.getAlerts() })
}
export function useStockMovements(skuId?: string) {
  return useQuery({ queryKey: ['inventory', 'movements', skuId], queryFn: () => inventoryService.getMovements(skuId) })
}
export function useAdjustStock() {
  const qc = useQueryClient()
  return useMutation({ mutationFn: ({ skuId, quantity, reason }: { skuId: string; quantity: number; reason: string }) => inventoryService.adjustStock(skuId, quantity, reason), onSuccess: () => { qc.invalidateQueries({ queryKey: ['inventory'] }); message.success('库存调整成功') } })
}
```

- [ ] **Step 4: Create barrel exports**

`types/index.ts`: `export * from './inventory.types'`
`services/index.ts`: `export * from './inventory.service'`
`hooks/index.ts`: `export * from './use-inventory'`

- [ ] **Step 5: Commit**

```bash
git add scm-web/src/features/inventory/
git commit -m "feat(frontend): add inventory types, service, and hooks"
```

---

### Task 2: Create Inventory Components and Pages

**Files:**
- Create: `scm-web/src/features/inventory/components/inventory-list.tsx`
- Create: `scm-web/src/features/inventory/components/stock-alerts.tsx`
- Create: `scm-web/src/features/inventory/components/stock-movements.tsx`
- Create: `scm-web/src/features/inventory/components/index.ts`
- Create: `scm-web/src/features/inventory/index.ts`
- Modify: `scm-web/src/app/[locale]/(app)/inventory/page.tsx`
- Modify: `scm-web/src/app/[locale]/(app)/inventory/alerts/page.tsx`

- [ ] **Step 1: Create inventory-list.tsx**

```tsx
'use client'
import { useState } from 'react'
import { Button, Tag, Space, Table, Card, Input, Select, Progress } from 'antd'
import { WarningOutlined } from '@ant-design/icons'
import { useInventoryList } from '../hooks'
import type { InventoryItem } from '../types'

const statusConfig: Record<string, { color: string; label: string }> = {
  normal: { color: 'success', label: '正常' },
  low: { color: 'warning', label: '偏低' },
  critical: { color: 'error', label: '紧急' },
  out: { color: 'default', label: '缺货' },
}

export default function InventoryList() {
  const [current, setCurrent] = useState(1)
  const [keyword, setKeyword] = useState('')
  const [status, setStatus] = useState<string>()
  const { data, isLoading } = useInventoryList({ current, keyword, status })

  const columns = [
    { title: 'SKU编码', dataIndex: 'skuCode', key: 'skuCode', width: 140 },
    { title: '商品名称', dataIndex: 'productName', key: 'productName' },
    { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 100 },
    { title: '总库存', dataIndex: 'totalStock', key: 'totalStock', width: 80, align: 'center' as const },
    { title: '可用', dataIndex: 'availableStock', key: 'availableStock', width: 80, align: 'center' as const },
    { title: '预留', dataIndex: 'reservedStock', key: 'reservedStock', width: 80, align: 'center' as const },
    { title: '库存状态', key: 'stockLevel', width: 120, render: (_: unknown, r: InventoryItem) => {
      const percent = Math.round((r.totalStock / r.threshold) * 100)
      return <Progress percent={Math.min(percent, 100)} size="small" status={r.status === 'out' || r.status === 'critical' ? 'exception' : r.status === 'low' ? 'active' : 'success'} />
    }},
    { title: '状态', dataIndex: 'status', key: 'status', width: 80, render: (s: string) => { const c = statusConfig[s]; return c ? <Tag color={c.color}>{c.label}</Tag> : s } },
    { title: '更新时间', dataIndex: 'lastUpdatedAt', key: 'lastUpdatedAt', width: 160 },
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>库存管理</h2>
      </div>
      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input.Search placeholder="搜索SKU/商品名称" style={{ width: 240 }} onSearch={(v) => { setKeyword(v); setCurrent(1) }} allowClear />
          <Select placeholder="库存状态" style={{ width: 120 }} allowClear onChange={(v) => { setStatus(v); setCurrent(1) }}
            options={[{ label: '正常', value: 'normal' }, { label: '偏低', value: 'low' }, { label: '紧急', value: 'critical' }, { label: '缺货', value: 'out' }]} />
        </Space>
      </Card>
      <Card>
        <Table columns={columns} dataSource={data?.records || []} loading={isLoading} rowKey="id" scroll={{ x: 1000 }}
          pagination={{ current, pageSize: 20, total: data?.total, onChange: setCurrent, showTotal: (t) => `共 ${t} 条` }} />
      </Card>
    </div>
  )
}
```

- [ ] **Step 2: Create stock-alerts.tsx**

```tsx
'use client'
import { Card, List, Tag, Progress, Button } from 'antd'
import { WarningOutlined, ReloadOutlined } from '@ant-design/icons'
import { useStockAlerts } from '../hooks'
import type { StockAlert } from '../types'

const statusConfig: Record<string, { color: string; label: string }> = {
  low: { color: 'warning', label: '偏低' },
  critical: { color: 'error', label: '紧急' },
  out: { color: 'default', label: '缺货' },
}

export default function StockAlerts() {
  const { data: alerts, isLoading, refetch } = useStockAlerts()

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}><WarningOutlined style={{ color: '#faad14', marginRight: 8 }} />库存预警</h2>
        <Button icon={<ReloadOutlined />} onClick={() => refetch()}>刷新</Button>
      </div>
      <Card>
        <List
          loading={isLoading}
          dataSource={alerts || []}
          renderItem={(item: StockAlert) => {
            const config = statusConfig[item.status]
            const percent = Math.round((item.currentStock / item.threshold) * 100)
            return (
              <List.Item>
                <List.Item.Meta
                  title={<span>{item.productName} <Tag color={config.color}>{config.label}</Tag></span>}
                  description={<span style={{ fontSize: 12, color: '#8c8c8c' }}>{item.skuCode} · {item.warehouseName} · 库存 {item.currentStock} / 阈值 {item.threshold}</span>}
                />
                <Progress percent={Math.min(percent, 100)} size="small" status={item.status === 'out' ? 'exception' : item.status === 'critical' ? 'exception' : 'active'} style={{ width: 100 }} />
              </List.Item>
            )
          }}
        />
      </Card>
    </div>
  )
}
```

- [ ] **Step 3: Create stock-movements.tsx**

```tsx
'use client'
import { useState } from 'react'
import { Table, Card, Tag, Select } from 'antd'
import { useStockMovements } from '../hooks'
import type { StockMovement } from '../types'

const typeConfig: Record<string, { color: string; label: string }> = {
  inbound: { color: 'success', label: '入库' },
  outbound: { color: 'error', label: '出库' },
  adjust: { color: 'warning', label: '调整' },
  transfer: { color: 'processing', label: '调拨' },
}

export default function StockMovements() {
  const [type, setType] = useState<string>()
  const { data: movements, isLoading } = useStockMovements()

  const filtered = type ? movements?.filter(m => m.type === type) : movements

  const columns = [
    { title: 'SKU编码', dataIndex: 'skuCode', key: 'skuCode', width: 140 },
    { title: '商品名称', dataIndex: 'productName', key: 'productName' },
    { title: '类型', dataIndex: 'type', key: 'type', width: 80, render: (s: string) => { const c = typeConfig[s]; return c ? <Tag color={c.color}>{c.label}</Tag> : s } },
    { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 80, align: 'center' as const, render: (v: number) => <span style={{ color: v > 0 ? '#52c41a' : '#ff4d4f' }}>{v > 0 ? '+' : ''}{v}</span> },
    { title: '调整前', dataIndex: 'beforeStock', key: 'beforeStock', width: 80, align: 'center' as const },
    { title: '调整后', dataIndex: 'afterStock', key: 'afterStock', width: 80, align: 'center' as const },
    { title: '原因', dataIndex: 'reason', key: 'reason' },
    { title: '操作人', dataIndex: 'operator', key: 'operator', width: 100 },
    { title: '时间', dataIndex: 'createdAt', key: 'createdAt', width: 160 },
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>库存流水</h2>
        <Select placeholder="流水类型" style={{ width: 120 }} allowClear onChange={setType}
          options={[{ label: '入库', value: 'inbound' }, { label: '出库', value: 'outbound' }, { label: '调整', value: 'adjust' }, { label: '调拨', value: 'transfer' }]} />
      </div>
      <Card>
        <Table columns={columns} dataSource={filtered || []} loading={isLoading} rowKey="id" scroll={{ x: 1000 }} pagination={{ pageSize: 20 }} />
      </Card>
    </div>
  )
}
```

- [ ] **Step 4: Create barrel exports and feature export**

Create `components/index.ts`:
```typescript
export { default as InventoryList } from './inventory-list'
export { default as StockAlerts } from './stock-alerts'
export { default as StockMovements } from './stock-movements'
```

Create `features/inventory/index.ts`:
```typescript
export * from './types'
export * from './hooks'
export * from './components'
```

- [ ] **Step 5: Create inventory pages**

Update `scm-web/src/app/[locale]/(app)/inventory/page.tsx`:
```tsx
'use client'
import { InventoryList } from '@/features/inventory/components'
export default function InventoryPage() { return <InventoryList /> }
```

Update `scm-web/src/app/[locale]/(app)/inventory/alerts/page.tsx`:
```tsx
'use client'
import { StockAlerts } from '@/features/inventory/components'
export default function InventoryAlertsPage() { return <StockAlerts /> }
```

- [ ] **Step 6: Verify build**

Run: `cd scm-web && npx tsc --noEmit`

- [ ] **Step 7: Commit**

```bash
git add scm-web/src/features/inventory/ scm-web/src/app/\[locale\]/\(app\)/inventory/
git commit -m "feat(frontend): add inventory module with list, alerts, and movements"
```

---

## Summary

| Task | Files Created | Files Modified |
|------|--------------|----------------|
| 1. Types, Service, Hooks | 6 | 0 |
| 2. Components & Pages | 5 | 2 |
| **Total** | **11** | **2** |

**Phase 5 Deliverables:**
- Inventory types (InventoryItem, StockMovement, StockAlert)
- Inventory service with mock data
- Inventory list with stock level progress bars
- Stock alerts page
- Stock movements (流水) page

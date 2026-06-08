# Frontend Phase 7: Purchase & Supplier Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build purchase order management and supplier management modules with CRUD pages.

**Architecture:** Feature module pattern. Mock data for development.

**Tech Stack:** Next.js 15, React 19, Ant Design 5, TanStack Query 5

---

## Tasks

### Task 1: Create Purchase Module

**Files:**
- Create: `scm-web/src/features/purchase/types/purchase.types.ts`
- Create: `scm-web/src/features/purchase/types/index.ts`
- Create: `scm-web/src/features/purchase/services/purchase.service.ts`
- Create: `scm-web/src/features/purchase/services/index.ts`
- Create: `scm-web/src/features/purchase/hooks/use-purchase.ts`
- Create: `scm-web/src/features/purchase/hooks/index.ts`
- Create: `scm-web/src/features/purchase/components/purchase-order-list.tsx`
- Create: `scm-web/src/features/purchase/components/purchase-order-detail.tsx`
- Create: `scm-web/src/features/purchase/components/index.ts`
- Create: `scm-web/src/features/purchase/index.ts`
- Modify: `scm-web/src/app/[locale]/(app)/purchase/page.tsx`
- Create: `scm-web/src/app/[locale]/(app)/purchase/[id]/page.tsx`

### Task 2: Create Supplier Module

**Files:**
- Create: `scm-web/src/features/supplier/types/supplier.types.ts`
- Create: `scm-web/src/features/supplier/types/index.ts`
- Create: `scm-web/src/features/supplier/services/supplier.service.ts`
- Create: `scm-web/src/features/supplier/services/index.ts`
- Create: `scm-web/src/features/supplier/hooks/use-supplier.ts`
- Create: `scm-web/src/features/supplier/hooks/index.ts`
- Create: `scm-web/src/features/supplier/components/supplier-list.tsx`
- Create: `scm-web/src/features/supplier/components/supplier-detail.tsx`
- Create: `scm-web/src/features/supplier/components/index.ts`
- Create: `scm-web/src/features/supplier/index.ts`
- Modify: `scm-web/src/app/[locale]/(app)/supplier/page.tsx`
- Create: `scm-web/src/app/[locale]/(app)/supplier/[id]/page.tsx`

---

## Task 1: Purchase Module

### purchase.types.ts

```typescript
export interface PurchaseOrder {
  id: string
  purchaseNo: string
  supplierId: string
  supplierName: string
  items: PurchaseOrderItem[]
  totalAmount: number
  status: 'draft' | 'pending' | 'approved' | 'ordered' | 'received' | 'completed' | 'cancelled'
  statusLabel: string
  remark?: string
  createdAt: string
  updatedAt: string
}

export interface PurchaseOrderItem {
  id: string
  purchaseOrderId: string
  skuId: string
  skuCode: string
  productName: string
  quantity: number
  unitPrice: number
  totalPrice: number
  receivedQuantity: number
}

export interface PurchaseSearchParams {
  current?: number
  pageSize?: number
  keyword?: string
  status?: string
  supplierId?: string
}
```

### purchase.service.ts

```typescript
import type { PurchaseOrder, PurchaseSearchParams } from '../types'

const statusMap: Record<string, { label: string; color: string }> = {
  draft: { label: '草稿', color: 'default' },
  pending: { label: '待审批', color: 'warning' },
  approved: { label: '已审批', color: 'processing' },
  ordered: { label: '已下单', color: 'blue' },
  received: { label: '已收货', color: 'purple' },
  completed: { label: '已完成', color: 'success' },
  cancelled: { label: '已取消', color: 'error' },
}

const mockPurchaseOrders: PurchaseOrder[] = [
  { id: '1', purchaseNo: 'PO-2026-001', supplierId: '1', supplierName: 'Apple供应链', items: [
    { id: '11', purchaseOrderId: '1', skuId: '11', skuCode: 'SKU-001-128', productName: 'iPhone 15 Pro 128G', quantity: 100, unitPrice: 5500, totalPrice: 550000, receivedQuantity: 100 },
  ], totalAmount: 550000, status: 'completed', statusLabel: '已完成', createdAt: '2026-05-15', updatedAt: '2026-06-01' },
  { id: '2', purchaseNo: 'PO-2026-002', supplierId: '1', supplierName: 'Apple供应链', items: [
    { id: '21', purchaseOrderId: '2', skuId: '21', skuCode: 'SKU-002-256', productName: 'MacBook Air M3 256G', quantity: 50, unitPrice: 7000, totalPrice: 350000, receivedQuantity: 30 },
  ], totalAmount: 350000, status: 'received', statusLabel: '已收货', createdAt: '2026-06-01', updatedAt: '2026-06-08' },
  { id: '3', purchaseNo: 'PO-2026-003', supplierId: '2', supplierName: 'Samsung代理', items: [
    { id: '31', purchaseOrderId: '3', skuId: '41', skuCode: 'SKU-004', productName: 'Galaxy S24 Ultra', quantity: 80, unitPrice: 6000, totalPrice: 480000, receivedQuantity: 0 },
  ], totalAmount: 480000, status: 'approved', statusLabel: '已审批', createdAt: '2026-06-05', updatedAt: '2026-06-07' },
]

export const purchaseService = {
  list: async (params?: PurchaseSearchParams) => {
    let filtered = [...mockPurchaseOrders]
    if (params?.keyword) filtered = filtered.filter(o => o.purchaseNo.includes(params.keyword!) || o.supplierName.includes(params.keyword!))
    if (params?.status) filtered = filtered.filter(o => o.status === params.status)
    const start = ((params?.current || 1) - 1) * (params?.pageSize || 20)
    return { records: filtered.slice(start, start + (params?.pageSize || 20)), total: filtered.length, current: params?.current || 1, pageSize: params?.pageSize || 20 }
  },
  getById: async (id: string): Promise<PurchaseOrder> => {
    const o = mockPurchaseOrders.find(x => x.id === id)
    if (!o) throw new Error('PurchaseOrder not found')
    return o
  },
  getStatusMap: () => statusMap,
}
```

### use-purchase.ts

```typescript
'use client'
import { useQuery } from '@tanstack/react-query'
import { purchaseService } from '../services/purchase.service'
import type { PurchaseSearchParams } from '../types'

export function usePurchaseOrderList(params?: PurchaseSearchParams) {
  return useQuery({ queryKey: ['purchase', 'list', params], queryFn: () => purchaseService.list(params) })
}
export function usePurchaseOrderDetail(id: string) {
  return useQuery({ queryKey: ['purchase', 'detail', id], queryFn: () => purchaseService.getById(id), enabled: !!id })
}
```

### purchase-order-list.tsx

```tsx
'use client'
import { useState } from 'react'
import { Button, Tag, Space, Table, Card, Input, Select } from 'antd'
import { EyeOutlined } from '@ant-design/icons'
import { useRouter } from 'next/navigation'
import { usePurchaseOrderList } from '../hooks'
import { purchaseService } from '../services/purchase.service'
import type { PurchaseOrder } from '../types'

export default function PurchaseOrderList() {
  const [current, setCurrent] = useState(1)
  const [keyword, setKeyword] = useState('')
  const [status, setStatus] = useState<string>()
  const router = useRouter()
  const { data, isLoading } = usePurchaseOrderList({ current, keyword, status })
  const statusMap = purchaseService.getStatusMap()

  const columns = [
    { title: '采购单号', dataIndex: 'purchaseNo', key: 'purchaseNo', render: (text: string, record: PurchaseOrder) => <a onClick={() => router.push(`/purchase/${record.id}`)}>{text}</a> },
    { title: '供应商', dataIndex: 'supplierName', key: 'supplierName' },
    { title: '商品数', key: 'itemCount', width: 80, align: 'center' as const, render: (_: unknown, r: PurchaseOrder) => r.items?.length || 0 },
    { title: '总金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 120, align: 'right' as const, render: (v: number) => `¥${v?.toLocaleString()}` },
    { title: '状态', dataIndex: 'status', key: 'status', width: 100, render: (s: string) => { const cfg = statusMap[s]; return cfg ? <Tag color={cfg.color}>{cfg.label}</Tag> : s } },
    { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 120 },
    { title: '操作', key: 'action', width: 80, render: (_: unknown, record: PurchaseOrder) => (
      <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => router.push(`/purchase/${record.id}`)}>查看</Button>
    )},
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>采购管理</h2>
      </div>
      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input.Search placeholder="搜索采购单号/供应商" style={{ width: 240 }} onSearch={(v) => { setKeyword(v); setCurrent(1) }} allowClear />
          <Select placeholder="状态" style={{ width: 120 }} allowClear onChange={(v) => { setStatus(v); setCurrent(1) }}
            options={Object.entries(statusMap).map(([key, val]) => ({ label: val.label, value: key }))} />
        </Space>
      </Card>
      <Card>
        <Table columns={columns} dataSource={data?.records || []} loading={isLoading} rowKey="id"
          pagination={{ current, pageSize: 20, total: data?.total, onChange: setCurrent }} />
      </Card>
    </div>
  )
}
```

### purchase-order-detail.tsx

```tsx
'use client'
import { Descriptions, Card, Tag, Table, Button } from 'antd'
import { useRouter } from 'next/navigation'
import { usePurchaseOrderDetail } from '../hooks'
import { purchaseService } from '../services/purchase.service'
import type { PurchaseOrderItem } from '../types'

interface Props { id: string }

export default function PurchaseOrderDetail({ id }: Props) {
  const { data: order, isLoading } = usePurchaseOrderDetail(id)
  const router = useRouter()
  const statusMap = purchaseService.getStatusMap()
  if (!order) return null

  const itemColumns = [
    { title: 'SKU编码', dataIndex: 'skuCode', key: 'skuCode', width: 140 },
    { title: '商品名称', dataIndex: 'productName', key: 'productName' },
    { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice', width: 100, align: 'right' as const, render: (v: number) => `¥${v?.toLocaleString()}` },
    { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 80, align: 'center' as const },
    { title: '已收货', dataIndex: 'receivedQuantity', key: 'receivedQuantity', width: 80, align: 'center' as const },
    { title: '小计', dataIndex: 'totalPrice', key: 'totalPrice', width: 100, align: 'right' as const, render: (v: number) => `¥${v?.toLocaleString()}` },
  ]

  const cfg = statusMap[order.status]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>采购单详情</h2>
        <Button onClick={() => router.push('/purchase')}>返回列表</Button>
      </div>
      <Card title="基本信息" loading={isLoading} style={{ marginBottom: 16 }}>
        <Descriptions column={2}>
          <Descriptions.Item label="采购单号">{order.purchaseNo}</Descriptions.Item>
          <Descriptions.Item label="状态"><Tag color={cfg?.color}>{cfg?.label}</Tag></Descriptions.Item>
          <Descriptions.Item label="供应商">{order.supplierName}</Descriptions.Item>
          <Descriptions.Item label="总金额">¥{order.totalAmount?.toLocaleString()}</Descriptions.Item>
          <Descriptions.Item label="创建时间">{order.createdAt}</Descriptions.Item>
        </Descriptions>
      </Card>
      <Card title="采购明细">
        <Table columns={itemColumns} dataSource={order.items || []} rowKey="id" pagination={false} size="small" />
      </Card>
    </div>
  )
}
```

Create barrel exports and pages similarly to other modules.

---

## Task 2: Supplier Module

### supplier.types.ts

```typescript
export interface Supplier {
  id: string
  name: string
  code: string
  contactPerson: string
  contactPhone: string
  email?: string
  address?: string
  status: 'active' | 'disabled'
  rating: number
  remark?: string
  createdAt: string
}

export interface SupplierSearchParams {
  current?: number
  pageSize?: number
  keyword?: string
  status?: string
}
```

### supplier.service.ts

```typescript
import type { Supplier, SupplierSearchParams } from '../types'

const mockSuppliers: Supplier[] = [
  { id: '1', name: 'Apple供应链', code: 'SUP-001', contactPerson: '张经理', contactPhone: '13800000001', email: 'zhang@apple-supply.com', status: 'active', rating: 5, createdAt: '2025-01-01' },
  { id: '2', name: 'Samsung代理', code: 'SUP-002', contactPerson: '李经理', contactPhone: '13800000002', email: 'li@samsung-agent.com', status: 'active', rating: 4, createdAt: '2025-02-01' },
  { id: '3', name: 'Huawei授权', code: 'SUP-003', contactPerson: '王经理', contactPhone: '13800000003', status: 'disabled', rating: 3, createdAt: '2025-03-01' },
]

export const supplierService = {
  list: async (params?: SupplierSearchParams) => {
    let filtered = [...mockSuppliers]
    if (params?.keyword) filtered = filtered.filter(s => s.name.includes(params.keyword!) || s.code.includes(params.keyword!))
    if (params?.status) filtered = filtered.filter(s => s.status === params.status)
    const start = ((params?.current || 1) - 1) * (params?.pageSize || 20)
    return { records: filtered.slice(start, start + (params?.pageSize || 20)), total: filtered.length, current: params?.current || 1, pageSize: params?.pageSize || 20 }
  },
  getById: async (id: string): Promise<Supplier> => {
    const s = mockSuppliers.find(x => x.id === id)
    if (!s) throw new Error('Supplier not found')
    return s
  },
}
```

### use-supplier.ts

```typescript
'use client'
import { useQuery } from '@tanstack/react-query'
import { supplierService } from '../services/supplier.service'
import type { SupplierSearchParams } from '../types'

export function useSupplierList(params?: SupplierSearchParams) {
  return useQuery({ queryKey: ['suppliers', 'list', params], queryFn: () => supplierService.list(params) })
}
export function useSupplierDetail(id: string) {
  return useQuery({ queryKey: ['suppliers', 'detail', id], queryFn: () => supplierService.getById(id), enabled: !!id })
}
```

### supplier-list.tsx

```tsx
'use client'
import { useState } from 'react'
import { Button, Tag, Space, Table, Card, Input, Select, Rate } from 'antd'
import { EyeOutlined } from '@ant-design/icons'
import { useRouter } from 'next/navigation'
import { useSupplierList } from '../hooks'
import type { Supplier } from '../types'

export default function SupplierList() {
  const [current, setCurrent] = useState(1)
  const [keyword, setKeyword] = useState('')
  const [status, setStatus] = useState<string>()
  const router = useRouter()
  const { data, isLoading } = useSupplierList({ current, keyword, status })

  const columns = [
    { title: '供应商编码', dataIndex: 'code', key: 'code', width: 120 },
    { title: '供应商名称', dataIndex: 'name', key: 'name', render: (text: string, record: Supplier) => <a onClick={() => router.push(`/supplier/${record.id}`)}>{text}</a> },
    { title: '联系人', dataIndex: 'contactPerson', key: 'contactPerson', width: 100 },
    { title: '联系电话', dataIndex: 'contactPhone', key: 'contactPhone', width: 140 },
    { title: '评级', dataIndex: 'rating', key: 'rating', width: 140, render: (v: number) => <Rate disabled defaultValue={v} /> },
    { title: '状态', dataIndex: 'status', key: 'status', width: 80, render: (s: string) => <Tag color={s === 'active' ? 'success' : 'error'}>{s === 'active' ? '启用' : '禁用'}</Tag> },
    { title: '操作', key: 'action', width: 80, render: (_: unknown, record: Supplier) => (
      <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => router.push(`/supplier/${record.id}`)}>查看</Button>
    )},
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>供应商管理</h2>
      </div>
      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input.Search placeholder="搜索供应商名称/编码" style={{ width: 240 }} onSearch={(v) => { setKeyword(v); setCurrent(1) }} allowClear />
          <Select placeholder="状态" style={{ width: 120 }} allowClear onChange={(v) => { setStatus(v); setCurrent(1) }}
            options={[{ label: '启用', value: 'active' }, { label: '禁用', value: 'disabled' }]} />
        </Space>
      </Card>
      <Card>
        <Table columns={columns} dataSource={data?.records || []} loading={isLoading} rowKey="id"
          pagination={{ current, pageSize: 20, total: data?.total, onChange: setCurrent }} />
      </Card>
    </div>
  )
}
```

### supplier-detail.tsx

```tsx
'use client'
import { Descriptions, Card, Tag, Button, Rate } from 'antd'
import { useRouter } from 'next/navigation'
import { useSupplierDetail } from '../hooks'

interface Props { id: string }

export default function SupplierDetail({ id }: Props) {
  const { data: supplier, isLoading } = useSupplierDetail(id)
  const router = useRouter()
  if (!supplier) return null

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>供应商详情</h2>
        <Button onClick={() => router.push('/supplier')}>返回列表</Button>
      </div>
      <Card loading={isLoading}>
        <Descriptions column={2}>
          <Descriptions.Item label="供应商编码">{supplier.code}</Descriptions.Item>
          <Descriptions.Item label="供应商名称">{supplier.name}</Descriptions.Item>
          <Descriptions.Item label="联系人">{supplier.contactPerson}</Descriptions.Item>
          <Descriptions.Item label="联系电话">{supplier.contactPhone}</Descriptions.Item>
          <Descriptions.Item label="邮箱">{supplier.email || '-'}</Descriptions.Item>
          <Descriptions.Item label="地址">{supplier.address || '-'}</Descriptions.Item>
          <Descriptions.Item label="评级"><Rate disabled defaultValue={supplier.rating} /></Descriptions.Item>
          <Descriptions.Item label="状态"><Tag color={supplier.status === 'active' ? 'success' : 'error'}>{supplier.status === 'active' ? '启用' : '禁用'}</Tag></Descriptions.Item>
          <Descriptions.Item label="创建时间">{supplier.createdAt}</Descriptions.Item>
        </Descriptions>
      </Card>
    </div>
  )
}
```

---

## Commit Strategy

Each module gets 2 commits:
1. `feat(frontend): add purchase module with list and detail`
2. `feat(frontend): add supplier module with list and detail`

## Summary

| Task | Files |
|------|-------|
| 1. Purchase Module | 12 files |
| 2. Supplier Module | 12 files |
| **Total** | **24 files** |

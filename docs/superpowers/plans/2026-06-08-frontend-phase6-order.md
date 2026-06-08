# Frontend Phase 6: Order Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build order management module with order list, detail view, order status flow visualization, and refund management.

**Architecture:** Feature module pattern with types, service, hooks, components, pages. Mock data for development.

**Tech Stack:** Next.js 15, React 19, Ant Design 5, TanStack Query 5

---

## Tasks

### Task 1: Create Order Types, Service, and Hooks

**Files:**
- Create: `scm-web/src/features/order/types/order.types.ts`
- Create: `scm-web/src/features/order/types/index.ts`
- Create: `scm-web/src/features/order/services/order.service.ts`
- Create: `scm-web/src/features/order/services/index.ts`
- Create: `scm-web/src/features/order/hooks/use-orders.ts`
- Create: `scm-web/src/features/order/hooks/index.ts`

- [ ] **Step 1: Create order.types.ts**

```typescript
export interface Order {
  id: string
  orderNo: string
  customerName: string
  customerPhone?: string
  items: OrderItem[]
  totalAmount: number
  discountAmount: number
  payAmount: number
  status: OrderStatus
  statusLabel: string
  payTime?: string
  shipTime?: string
  completeTime?: string
  cancelTime?: string
  remark?: string
  createdAt: string
  updatedAt: string
}

export interface OrderItem {
  id: string
  orderId: string
  skuId: string
  skuCode: string
  productName: string
  quantity: number
  unitPrice: number
  totalPrice: number
}

export type OrderStatus = 'PENDING_PAYMENT' | 'PAID' | 'PENDING_SHIP' | 'SHIPPED' | 'IN_TRANSIT' | 'DELIVERED' | 'COMPLETED' | 'CANCELLED' | 'REFUNDING'

export interface RefundRequest {
  id: string
  orderId: string
  orderNo: string
  reason: string
  amount: number
  status: 'pending' | 'approved' | 'rejected' | 'completed'
  createdAt: string
}

export interface OrderSearchParams {
  current?: number
  pageSize?: number
  keyword?: string
  status?: string
  dateRange?: [string, string]
}
```

- [ ] **Step 2: Create order.service.ts**

```typescript
import type { Order, RefundRequest, OrderSearchParams, OrderStatus } from '../types'

const statusFlow: Record<OrderStatus, { label: string; color: string }> = {
  PENDING_PAYMENT: { label: '待付款', color: 'warning' },
  PAID: { label: '已付款', color: 'processing' },
  PENDING_SHIP: { label: '待发货', color: 'processing' },
  SHIPPED: { label: '已发货', color: 'purple' },
  IN_TRANSIT: { label: '运输中', color: 'blue' },
  DELIVERED: { label: '已送达', color: 'cyan' },
  COMPLETED: { label: '已完成', color: 'success' },
  CANCELLED: { label: '已取消', color: 'default' },
  REFUNDING: { label: '退款中', color: 'error' },
}

const mockOrders: Order[] = [
  { id: '1', orderNo: 'ORD-2026-001', customerName: '张三', customerPhone: '13800138001', items: [
    { id: '11', orderId: '1', skuId: '11', skuCode: 'SKU-001-128', productName: 'iPhone 15 Pro 128G', quantity: 1, unitPrice: 7999, totalPrice: 7999 },
  ], totalAmount: 7999, discountAmount: 0, payAmount: 7999, status: 'COMPLETED', statusLabel: '已完成', payTime: '2026-06-01 10:30', shipTime: '2026-06-02 14:00', completeTime: '2026-06-05 16:30', createdAt: '2026-06-01 10:00', updatedAt: '2026-06-05 16:30' },
  { id: '2', orderNo: 'ORD-2026-002', customerName: '李四', items: [
    { id: '21', orderId: '2', skuId: '21', skuCode: 'SKU-002-256', productName: 'MacBook Air M3 256G', quantity: 1, unitPrice: 9999, totalPrice: 9999 },
  ], totalAmount: 9999, discountAmount: 500, payAmount: 9499, status: 'SHIPPED', statusLabel: '已发货', payTime: '2026-06-07 09:00', shipTime: '2026-06-08 11:00', createdAt: '2026-06-07 08:30', updatedAt: '2026-06-08 11:00' },
  { id: '3', orderNo: 'ORD-2026-003', customerName: '王五', items: [
    { id: '31', orderId: '3', skuId: '31', skuCode: 'SKU-003', productName: 'AirPods Pro 2', quantity: 2, unitPrice: 1899, totalPrice: 3798 },
  ], totalAmount: 3798, discountAmount: 0, payAmount: 3798, status: 'PENDING_PAYMENT', statusLabel: '待付款', createdAt: '2026-06-08 14:00', updatedAt: '2026-06-08 14:00' },
  { id: '4', orderNo: 'ORD-2026-004', customerName: '赵六', items: [
    { id: '41', orderId: '4', skuId: '11', skuCode: 'SKU-001-128', productName: 'iPhone 15 Pro 128G', quantity: 2, unitPrice: 7999, totalPrice: 15998 },
  ], totalAmount: 15998, discountAmount: 1000, payAmount: 14998, status: 'PAID', statusLabel: '已付款', payTime: '2026-06-08 10:30', createdAt: '2026-06-08 10:00', updatedAt: '2026-06-08 10:30' },
  { id: '5', orderNo: 'ORD-2026-005', customerName: '钱七', items: [
    { id: '51', orderId: '5', skuId: '12', skuCode: 'SKU-001-256', productName: 'iPhone 15 Pro 256G', quantity: 1, unitPrice: 8999, totalPrice: 8999 },
  ], totalAmount: 8999, discountAmount: 0, payAmount: 8999, status: 'CANCELLED', statusLabel: '已取消', cancelTime: '2026-06-07 15:00', createdAt: '2026-06-07 14:00', updatedAt: '2026-06-07 15:00' },
]

const mockRefunds: RefundRequest[] = [
  { id: '1', orderId: '5', orderNo: 'ORD-2026-005', reason: '不想要了', amount: 8999, status: 'completed', createdAt: '2026-06-07 15:30' },
]

export const orderService = {
  list: async (params?: OrderSearchParams) => {
    let filtered = [...mockOrders]
    if (params?.keyword) filtered = filtered.filter(o => o.orderNo.includes(params.keyword!) || o.customerName.includes(params.keyword!))
    if (params?.status) filtered = filtered.filter(o => o.status === params.status)
    const start = ((params?.current || 1) - 1) * (params?.pageSize || 20)
    return { records: filtered.slice(start, start + (params?.pageSize || 20)), total: filtered.length, current: params?.current || 1, pageSize: params?.pageSize || 20 }
  },
  getById: async (id: string): Promise<Order> => {
    const o = mockOrders.find(x => x.id === id)
    if (!o) throw new Error('Order not found')
    return o
  },
  updateStatus: async (id: string, status: OrderStatus): Promise<void> => {},
  listRefunds: async (): Promise<RefundRequest[]> => mockRefunds,
  getStatusFlow: () => statusFlow,
}
```

- [ ] **Step 3: Create use-orders.ts**

```typescript
'use client'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { orderService } from '../services/order.service'
import type { OrderSearchParams, OrderStatus } from '../types'

export function useOrderList(params?: OrderSearchParams) {
  return useQuery({ queryKey: ['orders', 'list', params], queryFn: () => orderService.list(params) })
}
export function useOrderDetail(id: string) {
  return useQuery({ queryKey: ['orders', 'detail', id], queryFn: () => orderService.getById(id), enabled: !!id })
}
export function useUpdateOrderStatus() {
  const qc = useQueryClient()
  return useMutation({ mutationFn: ({ id, status }: { id: string; status: OrderStatus }) => orderService.updateStatus(id, status), onSuccess: () => { qc.invalidateQueries({ queryKey: ['orders'] }); message.success('状态更新成功') } })
}
export function useRefundList() {
  return useQuery({ queryKey: ['orders', 'refunds'], queryFn: () => orderService.listRefunds() })
}
```

- [ ] **Step 4: Create barrel exports**

`types/index.ts`: `export * from './order.types'`
`services/index.ts`: `export * from './order.service'`
`hooks/index.ts`: `export * from './use-orders'`

- [ ] **Step 5: Commit**

```bash
git add scm-web/src/features/order/
git commit -m "feat(frontend): add order types, service, and hooks"
```

---

### Task 2: Create Order Components and Pages

**Files:**
- Create: `scm-web/src/features/order/components/order-list.tsx`
- Create: `scm-web/src/features/order/components/order-detail.tsx`
- Create: `scm-web/src/features/order/components/order-status-flow.tsx`
- Create: `scm-web/src/features/order/components/index.ts`
- Create: `scm-web/src/features/order/index.ts`
- Modify: `scm-web/src/app/[locale]/(app)/order/page.tsx`
- Create: `scm-web/src/app/[locale]/(app)/order/[id]/page.tsx`

- [ ] **Step 1: Create order-list.tsx**

```tsx
'use client'
import { useState } from 'react'
import { Button, Tag, Space, Table, Card, Input, Select } from 'antd'
import { EyeOutlined } from '@ant-design/icons'
import { useRouter } from 'next/navigation'
import { useOrderList } from '../hooks'
import { orderService } from '../services/order.service'
import type { Order } from '../types'

export default function OrderList() {
  const [current, setCurrent] = useState(1)
  const [keyword, setKeyword] = useState('')
  const [status, setStatus] = useState<string>()
  const router = useRouter()
  const { data, isLoading } = useOrderList({ current, keyword, status })
  const statusFlow = orderService.getStatusFlow()

  const columns = [
    { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', render: (text: string, record: Order) => <a onClick={() => router.push(`/order/${record.id}`)}>{text}</a> },
    { title: '客户', dataIndex: 'customerName', key: 'customerName', width: 100 },
    { title: '商品数', key: 'itemCount', width: 80, align: 'center' as const, render: (_: unknown, r: Order) => r.items?.length || 0 },
    { title: '订单金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 100, align: 'right' as const, render: (v: number) => `¥${v?.toLocaleString()}` },
    { title: '实付', dataIndex: 'payAmount', key: 'payAmount', width: 100, align: 'right' as const, render: (v: number) => `¥${v?.toLocaleString()}` },
    { title: '状态', dataIndex: 'status', key: 'status', width: 100, render: (s: keyof typeof statusFlow) => { const cfg = statusFlow[s]; return cfg ? <Tag color={cfg.color}>{cfg.label}</Tag> : s } },
    { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 160 },
    { title: '操作', key: 'action', width: 100, fixed: 'right' as const, render: (_: unknown, record: Order) => (
      <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => router.push(`/order/${record.id}`)}>查看</Button>
    )},
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>订单管理</h2>
      </div>
      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input.Search placeholder="搜索订单号/客户" style={{ width: 240 }} onSearch={(v) => { setKeyword(v); setCurrent(1) }} allowClear />
          <Select placeholder="订单状态" style={{ width: 120 }} allowClear onChange={(v) => { setStatus(v); setCurrent(1) }}
            options={Object.entries(statusFlow).map(([key, val]) => ({ label: val.label, value: key }))} />
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

- [ ] **Step 2: Create order-detail.tsx**

```tsx
'use client'
import { Descriptions, Card, Tag, Table, Button, Space, Timeline } from 'antd'
import { useRouter } from 'next/navigation'
import { useOrderDetail } from '../hooks'
import { orderService } from '../services/order.service'
import OrderStatusFlow from './order-status-flow'

interface OrderDetailProps { id: string }

export default function OrderDetail({ id }: OrderDetailProps) {
  const { data: order, isLoading } = useOrderDetail(id)
  const router = useRouter()
  const statusFlow = orderService.getStatusFlow()

  if (!order) return null

  const itemColumns = [
    { title: 'SKU编码', dataIndex: 'skuCode', key: 'skuCode', width: 140 },
    { title: '商品名称', dataIndex: 'productName', key: 'productName' },
    { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice', width: 100, align: 'right' as const, render: (v: number) => `¥${v?.toLocaleString()}` },
    { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 80, align: 'center' as const },
    { title: '小计', dataIndex: 'totalPrice', key: 'totalPrice', width: 100, align: 'right' as const, render: (v: number) => `¥${v?.toLocaleString()}` },
  ]

  const timelineItems = [
    order.createdAt && { children: `创建订单 ${order.createdAt}` },
    order.payTime && { children: `付款 ${order.payTime}`, color: 'green' },
    order.shipTime && { children: `发货 ${order.shipTime}`, color: 'green' },
    order.completeTime && { children: `完成 ${order.completeTime}`, color: 'green' },
    order.cancelTime && { children: `取消 ${order.cancelTime}`, color: 'red' },
  ].filter(Boolean)

  const cfg = statusFlow[order.status]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>订单详情</h2>
        <Button onClick={() => router.push('/order')}>返回列表</Button>
      </div>
      <OrderStatusFlow status={order.status} />
      <Card title="基本信息" loading={isLoading} style={{ marginTop: 16 }}>
        <Descriptions column={2}>
          <Descriptions.Item label="订单号">{order.orderNo}</Descriptions.Item>
          <Descriptions.Item label="状态"><Tag color={cfg?.color}>{cfg?.label}</Tag></Descriptions.Item>
          <Descriptions.Item label="客户">{order.customerName}</Descriptions.Item>
          <Descriptions.Item label="电话">{order.customerPhone || '-'}</Descriptions.Item>
          <Descriptions.Item label="订单金额">¥{order.totalAmount?.toLocaleString()}</Descriptions.Item>
          <Descriptions.Item label="优惠">¥{order.discountAmount?.toLocaleString()}</Descriptions.Item>
          <Descriptions.Item label="实付金额">¥{order.payAmount?.toLocaleString()}</Descriptions.Item>
          <Descriptions.Item label="创建时间">{order.createdAt}</Descriptions.Item>
          <Descriptions.Item label="备注" span={2}>{order.remark || '-'}</Descriptions.Item>
        </Descriptions>
      </Card>
      <Card title="商品明细" style={{ marginTop: 16 }}>
        <Table columns={itemColumns} dataSource={order.items || []} rowKey="id" pagination={false} size="small" />
      </Card>
      <Card title="订单日志" style={{ marginTop: 16 }}>
        <Timeline items={timelineItems} />
      </Card>
    </div>
  )
}
```

- [ ] **Step 3: Create order-status-flow.tsx**

```tsx
'use client'
import { Steps } from 'antd'
import type { OrderStatus } from '../types'

const steps = [
  { title: '待付款', key: 'PENDING_PAYMENT' },
  { title: '已付款', key: 'PAID' },
  { title: '待发货', key: 'PENDING_SHIP' },
  { title: '已发货', key: 'SHIPPED' },
  { title: '运输中', key: 'IN_TRANSIT' },
  { title: '已送达', key: 'DELIVERED' },
  { title: '已完成', key: 'COMPLETED' },
]

interface OrderStatusFlowProps { status: OrderStatus }

export default function OrderStatusFlow({ status }: OrderStatusFlowProps) {
  if (status === 'CANCELLED') {
    return <Steps current={-1} items={[...steps.slice(0, 3), { title: '已取消', status: 'error' }]} />
  }
  if (status === 'REFUNDING') {
    return <Steps current={-1} items={[...steps.slice(0, 2), { title: '退款中', status: 'process' }]} />
  }

  const currentStep = steps.findIndex(s => s.key === status)
  return <Steps current={currentStep >= 0 ? currentStep : 0} items={steps.map((s, i) => ({ ...s, status: i <= currentStep ? 'finish' : 'wait' }))} />
}
```

- [ ] **Step 4: Create barrel exports and feature export**

Create `components/index.ts`:
```typescript
export { default as OrderList } from './order-list'
export { default as OrderDetail } from './order-detail'
export { default as OrderStatusFlow } from './order-status-flow'
```

Create `features/order/index.ts`:
```typescript
export * from './types'
export * from './hooks'
export * from './components'
```

- [ ] **Step 5: Create order pages**

Update `scm-web/src/app/[locale]/(app)/order/page.tsx`:
```tsx
'use client'
import { OrderList } from '@/features/order/components'
export default function OrderPage() { return <OrderList /> }
```

Create `scm-web/src/app/[locale]/(app)/order/[id]/page.tsx`:
```tsx
'use client'
import { usePathname } from 'next/navigation'
import { OrderDetail } from '@/features/order/components'

export default function OrderDetailPage() {
  const pathname = usePathname()
  const id = pathname.split('/').pop() || ''
  return <OrderDetail id={id} />
}
```

- [ ] **Step 6: Verify build**

Run: `cd scm-web && npx tsc --noEmit`

- [ ] **Step 7: Commit**

```bash
git add scm-web/src/features/order/ scm-web/src/app/\[locale\]/\(app\)/order/
git commit -m "feat(frontend): add order module with list, detail, and status flow"
```

---

## Summary

| Task | Files Created | Files Modified |
|------|--------------|----------------|
| 1. Types, Service, Hooks | 6 | 0 |
| 2. Components & Pages | 6 | 1 |
| **Total** | **12** | **1** |

**Phase 6 Deliverables:**
- Order types (Order, OrderItem, RefundRequest, OrderStatus)
- Order service with mock data and status flow
- Order list with search, status filter
- Order detail with items table, timeline, status flow visualization
- Order status flow component (Steps)

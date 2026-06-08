# Frontend Phase 9: Notification Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build notification module with notification list, notification bell with badge, and notification detail view.

**Architecture:** Feature module pattern. Mock data for development.

**Tech Stack:** Next.js 15, React 19, Ant Design 5, TanStack Query 5

---

## Tasks

### Task 1: Create Notification Module

Create `scm-web/src/features/notification/` with all types, service, hooks, components, and pages.

**types/notification.types.ts:**
```typescript
export interface Notification {
  id: string
  title: string
  content: string
  type: 'system' | 'order' | 'inventory' | 'approval' | 'security'
  typeLabel: string
  isRead: boolean
  link?: string
  createdAt: string
}

export interface NotificationSearchParams {
  current?: number
  pageSize?: number
  type?: string
  isRead?: boolean
}
```

**services/notification.service.ts:**
```typescript
import type { Notification, NotificationSearchParams } from '../types'

const typeMap: Record<string, { label: string; color: string }> = {
  system: { label: '系统', color: 'blue' },
  order: { label: '订单', color: 'green' },
  inventory: { label: '库存', color: 'orange' },
  approval: { label: '审批', color: 'purple' },
  security: { label: '安全', color: 'red' },
}

const mockNotifications: Notification[] = [
  { id: '1', title: '订单已发货', content: '订单 ORD-2026-002 已发货，快递单号 JD0987654321', type: 'order', typeLabel: '订单', isRead: false, link: '/order/2', createdAt: '2026-06-08 14:30' },
  { id: '2', title: '库存预警', content: 'AirPods Pro 2 库存已不足，请及时补货', type: 'inventory', typeLabel: '库存', isRead: false, link: '/inventory/alerts', createdAt: '2026-06-08 13:00' },
  { id: '3', title: '系统维护通知', content: '系统将于今晚 22:00-23:00 进行维护升级', type: 'system', typeLabel: '系统', isRead: true, createdAt: '2026-06-08 10:00' },
  { id: '4', title: '采购单审批通过', content: '采购单 PO-2026-003 已通过审批', type: 'approval', typeLabel: '审批', isRead: true, link: '/purchase/3', createdAt: '2026-06-07 16:00' },
  { id: '5', title: '登录异常', content: '您的账号在异地登录，如非本人操作请及时修改密码', type: 'security', typeLabel: '安全', isRead: false, createdAt: '2026-06-07 09:00' },
]

export const notificationService = {
  list: async (params?: NotificationSearchParams) => {
    let filtered = [...mockNotifications]
    if (params?.type) filtered = filtered.filter(n => n.type === params.type)
    if (params?.isRead !== undefined) filtered = filtered.filter(n => n.isRead === params.isRead)
    const start = ((params?.current || 1) - 1) * (params?.pageSize || 20)
    return { records: filtered.slice(start, start + (params?.pageSize || 20)), total: filtered.length, current: params?.current || 1, pageSize: params?.pageSize || 20 }
  },
  getUnreadCount: async (): Promise<number> => mockNotifications.filter(n => !n.isRead).length,
  markAsRead: async (id: string): Promise<void> => {},
  markAllAsRead: async (): Promise<void> => {},
  getTypeMap: () => typeMap,
}
```

**hooks/use-notification.ts:**
```typescript
'use client'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { notificationService } from '../services/notification.service'
import type { NotificationSearchParams } from '../types'

export function useNotificationList(params?: NotificationSearchParams) {
  return useQuery({ queryKey: ['notifications', 'list', params], queryFn: () => notificationService.list(params) })
}
export function useUnreadCount() {
  return useQuery({ queryKey: ['notifications', 'unread'], queryFn: () => notificationService.getUnreadCount(), refetchInterval: 60000 })
}
export function useMarkAsRead() {
  const qc = useQueryClient()
  return useMutation({ mutationFn: (id: string) => notificationService.markAsRead(id), onSuccess: () => { qc.invalidateQueries({ queryKey: ['notifications'] }) } })
}
export function useMarkAllAsRead() {
  const qc = useQueryClient()
  return useMutation({ mutationFn: () => notificationService.markAllAsRead(), onSuccess: () => { qc.invalidateQueries({ queryKey: ['notifications'] }); message.success('已全部标为已读') } })
}
```

**components/notification-list.tsx:**
```tsx
'use client'
import { useState } from 'react'
import { List, Card, Tag, Button, Space, Select, Badge } from 'antd'
import { BellOutlined, CheckOutlined } from '@ant-design/icons'
import { useRouter } from 'next/navigation'
import { useNotificationList, useMarkAsRead, useMarkAllAsRead, useUnreadCount } from '../hooks'
import { notificationService } from '../services/notification.service'
import type { Notification } from '../types'

export default function NotificationList() {
  const [type, setType] = useState<string>()
  const [current, setCurrent] = useState(1)
  const router = useRouter()
  const { data, isLoading } = useNotificationList({ current, type })
  const { data: unreadCount } = useUnreadCount()
  const markAsRead = useMarkAsRead()
  const markAllAsRead = useMarkAllAsRead()
  const typeMap = notificationService.getTypeMap()

  const handleItemClick = (item: Notification) => {
    if (!item.isRead) markAsRead.mutate(item.id)
    if (item.link) router.push(item.link)
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Space>
          <h2 style={{ margin: 0 }}>通知管理</h2>
          <Badge count={unreadCount || 0} />
        </Space>
        <Space>
          <Select placeholder="通知类型" style={{ width: 120 }} allowClear onChange={setType}
            options={Object.entries(typeMap).map(([key, val]) => ({ label: val.label, value: key }))} />
          <Button icon={<CheckOutlined />} onClick={() => markAllAsRead.mutate()}>全部已读</Button>
        </Space>
      </div>
      <Card>
        <List
          loading={isLoading}
          dataSource={data?.records || []}
          pagination={{ current, pageSize: 20, total: data?.total, onChange: setCurrent }}
          renderItem={(item: Notification) => {
            const cfg = typeMap[item.type]
            return (
              <List.Item
                style={{ background: item.isRead ? 'transparent' : '#f6ffed', cursor: 'pointer', padding: '12px 16px' }}
                onClick={() => handleItemClick(item)}
                actions={item.isRead ? [] : [<Button key="read" type="link" size="small" onClick={(e) => { e.stopPropagation(); markAsRead.mutate(item.id) }}>标为已读</Button>]}
              >
                <List.Item.Meta
                  title={<Space><Tag color={cfg?.color}>{cfg?.label}</Tag><span style={{ fontWeight: item.isRead ? 'normal' : 'bold' }}>{item.title}</span></Space>}
                  description={<div><div>{item.content}</div><div style={{ fontSize: 12, color: '#8c8c8c', marginTop: 4 }}>{item.createdAt}</div></div>}
                />
              </List.Item>
            )
          }}
        />
      </Card>
    </div>
  )
}
```

**components/notification-bell.tsx:**
```tsx
'use client'
import { Badge, Dropdown, List, Button, Empty } from 'antd'
import { BellOutlined } from '@ant-design/icons'
import { useRouter } from 'next/navigation'
import { useUnreadCount, useNotificationList, useMarkAsRead } from '../hooks'
import type { Notification } from '../types'

export default function NotificationBell() {
  const router = useRouter()
  const { data: unreadCount } = useUnreadCount()
  const { data } = useNotificationList({ pageSize: 5 })
  const markAsRead = useMarkAsRead()

  const items = (data?.records || []).map((n: Notification) => ({
    key: n.id,
    label: (
      <div style={{ maxWidth: 280, padding: '4px 0' }} onClick={() => { if (!n.isRead) markAsRead.mutate(n.id); if (n.link) router.push(n.link) }}>
        <div style={{ fontWeight: n.isRead ? 'normal' : 'bold', fontSize: 13 }}>{n.title}</div>
        <div style={{ fontSize: 12, color: '#8c8c8c', marginTop: 2 }}>{n.createdAt}</div>
      </div>
    ),
  }))

  return (
    <Dropdown menu={{ items: items.length ? items : [{ key: 'empty', label: <Empty description="暂无通知" image={Empty.PRESENTED_IMAGE_SIMPLE} /> }] }} placement="bottomRight" trigger={['click']}>
      <Badge count={unreadCount || 0} size="small">
        <BellOutlined style={{ fontSize: 18, cursor: 'pointer' }} />
      </Badge>
    </Dropdown>
  )
}
```

**components/index.ts:**
```typescript
export { default as NotificationList } from './notification-list'
export { default as NotificationBell } from './notification-bell'
```

**index.ts:**
```typescript
export * from './types'
export * from './hooks'
export * from './components'
```

**Update pages:**
- `scm-web/src/app/[locale]/(app)/notification/page.tsx`:
```tsx
'use client'
import { NotificationList } from '@/features/notification/components'
export default function NotificationPage() { return <NotificationList /> }
```

**Update header to add NotificationBell:**

Modify `scm-web/src/components/Layout/header.tsx`:
- Add import: `import NotificationBell from '@/features/notification/components/notification-bell'`
- Replace the existing Badge/BellOutlined section with `<NotificationBell />`

---

## Commit

```bash
git add scm-web/src/features/notification/ scm-web/src/app/\[locale\]/\(app\)/notification/
git commit -m "feat(frontend): add notification module with list and bell"
```

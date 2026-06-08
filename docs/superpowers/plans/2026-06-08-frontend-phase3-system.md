# Frontend Phase 3: System Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build system management module with User CRUD, Role CRUD, Permission tree, Department tree, and Dictionary management pages.

**Architecture:** Each sub-module follows the feature module pattern: types → service → hooks → components → page. Uses ProTable for lists, ProForm for create/edit, Modal/Drawer for forms. Mock data for development.

**Tech Stack:** Next.js 15, React 19, Ant Design 5, Pro Components, TanStack Query 5

---

## Tasks

### Task 1: Create System Module Types

**Files:**
- Create: `scm-web/src/features/system/types/system.types.ts`
- Create: `scm-web/src/features/system/types/index.ts`

- [ ] **Step 1: Create system.types.ts**

```typescript
// scm-web/src/features/system/types/system.types.ts

export interface User {
  id: string
  username: string
  displayName: string
  email: string
  phone?: string
  avatar?: string
  status: 'active' | 'disabled'
  roles: string[]
  deptId?: string
  deptName?: string
  createdAt: string
  updatedAt: string
}

export interface Role {
  id: string
  name: string
  code: string
  description?: string
  status: 'active' | 'disabled'
  permissions: string[]
  userCount: number
  createdAt: string
}

export interface Permission {
  id: string
  name: string
  code: string
  type: 'menu' | 'button' | 'data'
  parentId: string | null
  icon?: string
  path?: string
  sort: number
  status: 'active' | 'disabled'
  children?: Permission[]
}

export interface Department {
  id: string
  name: string
  code: string
  parentId: string | null
  sort: number
  status: 'active' | 'disabled'
  leaderId?: string
  leaderName?: string
  children?: Department[]
}

export interface DictType {
  id: string
  name: string
  code: string
  description?: string
  status: 'active' | 'disabled'
  itemCount: number
}

export interface DictItem {
  id: string
  dictTypeId: string
  label: string
  value: string
  sort: number
  status: 'active' | 'disabled'
  remark?: string
}

export interface PageParams {
  current?: number
  pageSize?: number
  keyword?: string
  status?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  pageSize: number
}
```

- [ ] **Step 2: Create types barrel export**

```typescript
export * from './system.types'
```

- [ ] **Step 3: Commit**

```bash
git add scm-web/src/features/system/types/
git commit -m "feat(frontend): add system management types"
```

---

### Task 2: Create System Services

**Files:**
- Create: `scm-web/src/features/system/services/user.service.ts`
- Create: `scm-web/src/features/system/services/role.service.ts`
- Create: `scm-web/src/features/system/services/permission.service.ts`
- Create: `scm-web/src/features/system/services/dept.service.ts`
- Create: `scm-web/src/features/system/services/dict.service.ts`
- Create: `scm-web/src/features/system/services/index.ts`

- [ ] **Step 1: Create user.service.ts**

```typescript
import apiClient from '@/lib/api/client'
import type { User, PageParams, PageResult } from '../types'

// Mock data
const mockUsers: User[] = [
  { id: '1', username: 'admin', displayName: '管理员', email: 'admin@scm.com', status: 'active', roles: ['admin'], deptId: '1', deptName: '技术部', createdAt: '2026-01-01', updatedAt: '2026-06-01' },
  { id: '2', username: 'zhangsan', displayName: '张三', email: 'zhangsan@scm.com', phone: '13800138000', status: 'active', roles: ['user'], deptId: '2', deptName: '销售部', createdAt: '2026-02-15', updatedAt: '2026-05-20' },
  { id: '3', username: 'lisi', displayName: '李四', email: 'lisi@scm.com', status: 'disabled', roles: ['user'], deptId: '2', deptName: '销售部', createdAt: '2026-03-10', updatedAt: '2026-04-01' },
]

export const userService = {
  list: async (params?: PageParams): Promise<PageResult<User>> => {
    // TODO: Replace with real API
    // return apiClient.get('/api/users', { params })
    const filtered = mockUsers.filter(u =>
      !params?.keyword || u.username.includes(params.keyword) || u.displayName.includes(params.keyword)
    )
    return { records: filtered, total: filtered.length, current: params?.current || 1, pageSize: params?.pageSize || 20 }
  },
  getById: async (id: string): Promise<User> => {
    const user = mockUsers.find(u => u.id === id)
    if (!user) throw new Error('User not found')
    return user
  },
  create: async (data: Partial<User>): Promise<User> => {
    return { ...data, id: String(Date.now()), createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() } as User
  },
  update: async (id: string, data: Partial<User>): Promise<User> => {
    const user = mockUsers.find(u => u.id === id)
    if (!user) throw new Error('User not found')
    return { ...user, ...data, updatedAt: new Date().toISOString() }
  },
  delete: async (id: string): Promise<void> => {
    // apiClient.delete(`/api/users/${id}`)
  },
}
```

- [ ] **Step 2: Create role.service.ts**

```typescript
import type { Role } from '../types'

const mockRoles: Role[] = [
  { id: '1', name: '超级管理员', code: 'admin', description: '系统最高权限', status: 'active', permissions: ['*'], userCount: 1, createdAt: '2026-01-01' },
  { id: '2', name: '普通用户', code: 'user', description: '基本操作权限', status: 'active', permissions: ['dashboard:view', 'product:view', 'order:view'], userCount: 5, createdAt: '2026-01-01' },
  { id: '3', name: '仓库管理员', code: 'warehouse_admin', description: '仓库管理权限', status: 'active', permissions: ['inventory:view', 'inventory:adjust', 'warehouse:view'], userCount: 3, createdAt: '2026-02-01' },
]

export const roleService = {
  list: async (): Promise<Role[]> => mockRoles,
  getById: async (id: string): Promise<Role> => {
    const role = mockRoles.find(r => r.id === id)
    if (!role) throw new Error('Role not found')
    return role
  },
  create: async (data: Partial<Role>): Promise<Role> => {
    return { ...data, id: String(Date.now()), userCount: 0, createdAt: new Date().toISOString() } as Role
  },
  update: async (id: string, data: Partial<Role>): Promise<Role> => {
    const role = mockRoles.find(r => r.id === id)
    if (!role) throw new Error('Role not found')
    return { ...role, ...data }
  },
  delete: async (id: string): Promise<void> => {},
}
```

- [ ] **Step 3: Create permission.service.ts**

```typescript
import type { Permission } from '../types'

const mockPermissions: Permission[] = [
  { id: '1', name: '仪表盘', code: 'dashboard:view', type: 'menu', parentId: null, icon: 'DashboardOutlined', path: '/dashboard', sort: 1, status: 'active' },
  { id: '2', name: '商品管理', code: 'product', type: 'menu', parentId: null, icon: 'ShopOutlined', path: '/product', sort: 2, status: 'active', children: [
    { id: '21', name: '商品列表', code: 'product:view', type: 'menu', parentId: '2', path: '/product', sort: 1, status: 'active' },
    { id: '22', name: '创建商品', code: 'product:create', type: 'button', parentId: '2', sort: 2, status: 'active' },
    { id: '23', name: '编辑商品', code: 'product:edit', type: 'button', parentId: '2', sort: 3, status: 'active' },
    { id: '24', name: '删除商品', code: 'product:delete', type: 'button', parentId: '2', sort: 4, status: 'active' },
  ]},
  { id: '3', name: '订单管理', code: 'order', type: 'menu', parentId: null, icon: 'ShoppingCartOutlined', path: '/order', sort: 3, status: 'active', children: [
    { id: '31', name: '订单列表', code: 'order:view', type: 'menu', parentId: '3', path: '/order', sort: 1, status: 'active' },
    { id: '32', name: '创建订单', code: 'order:create', type: 'button', parentId: '3', sort: 2, status: 'active' },
  ]},
  { id: '4', name: '系统管理', code: 'system', type: 'menu', parentId: null, icon: 'SettingOutlined', path: '/system', sort: 10, status: 'active', children: [
    { id: '41', name: '用户管理', code: 'system:user:view', type: 'menu', parentId: '4', path: '/system/user', sort: 1, status: 'active' },
    { id: '42', name: '角色管理', code: 'system:role:view', type: 'menu', parentId: '4', path: '/system/role', sort: 2, status: 'active' },
  ]},
]

export const permissionService = {
  list: async (): Promise<Permission[]> => mockPermissions,
  create: async (data: Partial<Permission>): Promise<Permission> => {
    return { ...data, id: String(Date.now()), sort: 0, status: 'active' } as Permission
  },
  update: async (id: string, data: Partial<Permission>): Promise<Permission> => {
    return { ...data, id } as Permission
  },
  delete: async (id: string): Promise<void> => {},
}
```

- [ ] **Step 4: Create dept.service.ts**

```typescript
import type { Department } from '../types'

const mockDepts: Department[] = [
  { id: '1', name: '技术部', code: 'tech', parentId: null, sort: 1, status: 'active', leaderId: '1', leaderName: '管理员', children: [
    { id: '11', name: '前端组', code: 'tech-fe', parentId: '1', sort: 1, status: 'active' },
    { id: '12', name: '后端组', code: 'tech-be', parentId: '1', sort: 2, status: 'active' },
  ]},
  { id: '2', name: '销售部', code: 'sales', parentId: null, sort: 2, status: 'active', leaderId: '2', leaderName: '张三' },
  { id: '3', name: '仓储部', code: 'warehouse', parentId: null, sort: 3, status: 'active' },
]

export const deptService = {
  list: async (): Promise<Department[]> => mockDepts,
  create: async (data: Partial<Department>): Promise<Department> => {
    return { ...data, id: String(Date.now()), sort: 0, status: 'active' } as Department
  },
  update: async (id: string, data: Partial<Department>): Promise<Department> => {
    return { ...data, id } as Department
  },
  delete: async (id: string): Promise<void> => {},
}
```

- [ ] **Step 5: Create dict.service.ts**

```typescript
import type { DictType, DictItem } from '../types'

const mockDictTypes: DictType[] = [
  { id: '1', name: '订单状态', code: 'order_status', description: '订单状态字典', status: 'active', itemCount: 5 },
  { id: '2', name: '商品状态', code: 'product_status', description: '商品上下架状态', status: 'active', itemCount: 3 },
  { id: '3', name: '用户状态', code: 'user_status', description: '用户启用禁用状态', status: 'active', itemCount: 2 },
]

const mockDictItems: DictItem[] = [
  { id: '11', dictTypeId: '1', label: '待付款', value: 'PENDING_PAYMENT', sort: 1, status: 'active' },
  { id: '12', dictTypeId: '1', label: '已付款', value: 'PAID', sort: 2, status: 'active' },
  { id: '13', dictTypeId: '1', label: '已发货', value: 'SHIPPED', sort: 3, status: 'active' },
  { id: '14', dictTypeId: '1', label: '已完成', value: 'COMPLETED', sort: 4, status: 'active' },
  { id: '15', dictTypeId: '1', label: '已取消', value: 'CANCELLED', sort: 5, status: 'active' },
  { id: '21', dictTypeId: '2', label: '在售', value: 'ON_SALE', sort: 1, status: 'active' },
  { id: '22', dictTypeId: '2', label: '下架', value: 'OFF_SALE', sort: 2, status: 'active' },
  { id: '23', dictTypeId: '2', label: '草稿', value: 'DRAFT', sort: 3, status: 'active' },
]

export const dictService = {
  listTypes: async (): Promise<DictType[]> => mockDictTypes,
  getTypeById: async (id: string): Promise<DictType> => {
    const t = mockDictTypes.find(d => d.id === id)
    if (!t) throw new Error('DictType not found')
    return t
  },
  createType: async (data: Partial<DictType>): Promise<DictType> => {
    return { ...data, id: String(Date.now()), itemCount: 0, status: 'active' } as DictType
  },
  updateType: async (id: string, data: Partial<DictType>): Promise<DictType> => {
    return { ...data, id } as DictType
  },
  deleteType: async (id: string): Promise<void> => {},
  listItems: async (dictTypeId: string): Promise<DictItem[]> => {
    return mockDictItems.filter(i => i.dictTypeId === dictTypeId)
  },
  createItem: async (data: Partial<DictItem>): Promise<DictItem> => {
    return { ...data, id: String(Date.now()), status: 'active' } as DictItem
  },
  updateItem: async (id: string, data: Partial<DictItem>): Promise<DictItem> => {
    return { ...data, id } as DictItem
  },
  deleteItem: async (id: string): Promise<void> => {},
}
```

- [ ] **Step 6: Create services barrel export**

```typescript
export * from './user.service'
export * from './role.service'
export * from './permission.service'
export * from './dept.service'
export * from './dict.service'
```

- [ ] **Step 7: Commit**

```bash
git add scm-web/src/features/system/services/
git commit -m "feat(frontend): add system management services with mock data"
```

---

### Task 3: Create System Hooks

**Files:**
- Create: `scm-web/src/features/system/hooks/use-users.ts`
- Create: `scm-web/src/features/system/hooks/use-roles.ts`
- Create: `scm-web/src/features/system/hooks/use-permissions.ts`
- Create: `scm-web/src/features/system/hooks/use-depts.ts`
- Create: `scm-web/src/features/system/hooks/use-dict.ts`
- Create: `scm-web/src/features/system/hooks/index.ts`

- [ ] **Step 1: Create use-users.ts**

```typescript
'use client'

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { userService } from '../services/user.service'
import type { PageParams } from '../types'

export function useUserList(params?: PageParams) {
  return useQuery({
    queryKey: ['users', 'list', params],
    queryFn: () => userService.list(params),
  })
}

export function useUserDetail(id: string) {
  return useQuery({
    queryKey: ['users', 'detail', id],
    queryFn: () => userService.getById(id),
    enabled: !!id,
  })
}

export function useCreateUser() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: Parameters<typeof userService.create>[0]) => userService.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] })
      message.success('创建成功')
    },
  })
}

export function useUpdateUser() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: Parameters<typeof userService.update>[1] }) => userService.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] })
      message.success('更新成功')
    },
  })
}

export function useDeleteUser() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => userService.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] })
      message.success('删除成功')
    },
  })
}
```

- [ ] **Step 2: Create use-roles.ts**

```typescript
'use client'

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { roleService } from '../services/role.service'

export function useRoleList() {
  return useQuery({
    queryKey: ['roles'],
    queryFn: () => roleService.list(),
  })
}

export function useRoleDetail(id: string) {
  return useQuery({
    queryKey: ['roles', 'detail', id],
    queryFn: () => roleService.getById(id),
    enabled: !!id,
  })
}

export function useCreateRole() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: Parameters<typeof roleService.create>[0]) => roleService.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['roles'] })
      message.success('创建成功')
    },
  })
}

export function useUpdateRole() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: Parameters<typeof roleService.update>[1] }) => roleService.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['roles'] })
      message.success('更新成功')
    },
  })
}

export function useDeleteRole() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => roleService.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['roles'] })
      message.success('删除成功')
    },
  })
}
```

- [ ] **Step 3: Create use-permissions.ts**

```typescript
'use client'

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { permissionService } from '../services/permission.service'

export function usePermissionList() {
  return useQuery({
    queryKey: ['permissions'],
    queryFn: () => permissionService.list(),
  })
}

export function useCreatePermission() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: Parameters<typeof permissionService.create>[0]) => permissionService.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['permissions'] })
      message.success('创建成功')
    },
  })
}

export function useDeletePermission() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => permissionService.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['permissions'] })
      message.success('删除成功')
    },
  })
}
```

- [ ] **Step 4: Create use-depts.ts**

```typescript
'use client'

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { deptService } from '../services/dept.service'

export function useDeptList() {
  return useQuery({
    queryKey: ['depts'],
    queryFn: () => deptService.list(),
  })
}

export function useCreateDept() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: Parameters<typeof deptService.create>[0]) => deptService.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['depts'] })
      message.success('创建成功')
    },
  })
}

export function useDeleteDept() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => deptService.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['depts'] })
      message.success('删除成功')
    },
  })
}
```

- [ ] **Step 5: Create use-dict.ts**

```typescript
'use client'

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { dictService } from '../services/dict.service'

export function useDictTypeList() {
  return useQuery({
    queryKey: ['dict', 'types'],
    queryFn: () => dictService.listTypes(),
  })
}

export function useDictItems(dictTypeId: string) {
  return useQuery({
    queryKey: ['dict', 'items', dictTypeId],
    queryFn: () => dictService.listItems(dictTypeId),
    enabled: !!dictTypeId,
  })
}

export function useCreateDictType() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: Parameters<typeof dictService.createType>[0]) => dictService.createType(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['dict', 'types'] })
      message.success('创建成功')
    },
  })
}

export function useDeleteDictType() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => dictService.deleteType(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['dict'] })
      message.success('删除成功')
    },
  })
}

export function useCreateDictItem() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: Parameters<typeof dictService.createItem>[0]) => dictService.createItem(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['dict', 'items'] })
      message.success('创建成功')
    },
  })
}
```

- [ ] **Step 6: Create hooks barrel export**

```typescript
export * from './use-users'
export * from './use-roles'
export * from './use-permissions'
export * from './use-depts'
export * from './use-dict'
```

- [ ] **Step 7: Commit**

```bash
git add scm-web/src/features/system/hooks/
git commit -m "feat(frontend): add system management hooks"
```

---

### Task 4: Create User Management Page

**Files:**
- Create: `scm-web/src/features/system/components/user/user-list.tsx`
- Create: `scm-web/src/features/system/components/user/user-form.tsx`
- Create: `scm-web/src/features/system/components/user/index.ts`
- Modify: `scm-web/src/app/[locale]/(app)/system/user/page.tsx`

- [ ] **Step 1: Create user-list.tsx**

```tsx
'use client'

import { useState } from 'react'
import { Button, Tag, Space, Popconfirm, message } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, EyeOutlined } from '@ant-design/icons'
import { useRouter } from 'next/navigation'
import { useUserList, useDeleteUser } from '../../hooks'
import type { User } from '../../types'

export default function UserList() {
  const [current, setCurrent] = useState(1)
  const [keyword, setKeyword] = useState('')
  const router = useRouter()
  const { data, isLoading } = useUserList({ current, keyword })
  const deleteUser = useDeleteUser()

  const columns = [
    { title: '用户名', dataIndex: 'username', key: 'username' },
    { title: '姓名', dataIndex: 'displayName', key: 'displayName' },
    { title: '邮箱', dataIndex: 'email', key: 'email' },
    { title: '部门', dataIndex: 'deptName', key: 'deptName' },
    { title: '状态', dataIndex: 'status', key: 'status', render: (status: string) => <Tag color={status === 'active' ? 'success' : 'error'}>{status === 'active' ? '启用' : '禁用'}</Tag> },
    { title: '角色', dataIndex: 'roles', key: 'roles', render: (roles: string[]) => roles?.map(r => <Tag key={r}>{r}</Tag>) },
    {
      title: '操作', key: 'action', render: (_: unknown, record: User) => (
        <Space>
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => router.push(`/system/user/${record.id}`)}>查看</Button>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => router.push(`/system/user/${record.id}/edit`)}>编辑</Button>
          <Popconfirm title="确定删除?" onConfirm={() => deleteUser.mutate(record.id)}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>用户管理</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => router.push('/system/user/create')}>新建用户</Button>
      </div>
      <ProTable
        columns={columns}
        dataSource={data?.records || []}
        loading={isLoading}
        rowKey="id"
        search={{ onSearch: (value: string) => { setKeyword(value); setCurrent(1) } }}
        pagination={{ current, pageSize: 20, total: data?.total, onChange: setCurrent }}
      />
    </div>
  )
}
```

Note: Need to import ProTable. If not available, use regular Table from antd.

- [ ] **Step 2: Create user-form.tsx**

```tsx
'use client'

import { useEffect } from 'react'
import { Form, Input, Select, Modal, message } from 'antd'
import { useCreateUser, useUpdateUser } from '../../hooks'
import type { User } from '../../types'

interface UserFormProps {
  open: boolean
  onClose: () => void
  user?: User | null
}

export default function UserForm({ open, onClose, user }: UserFormProps) {
  const [form] = Form.useForm()
  const createUser = useCreateUser()
  const updateUser = useUpdateUser()

  useEffect(() => {
    if (open) {
      if (user) {
        form.setFieldsValue(user)
      } else {
        form.resetFields()
      }
    }
  }, [open, user, form])

  const handleOk = async () => {
    try {
      const values = await form.validateFields()
      if (user) {
        await updateUser.mutateAsync({ id: user.id, data: values })
      } else {
        await createUser.mutateAsync(values)
      }
      onClose()
    } catch {
      // validation error
    }
  }

  return (
    <Modal
      title={user ? '编辑用户' : '新建用户'}
      open={open}
      onOk={handleOk}
      onCancel={onClose}
      confirmLoading={createUser.isPending || updateUser.isPending}
    >
      <Form form={form} layout="vertical">
        <Form.Item name="username" label="用户名" rules={[{ required: true }]}>
          <Input placeholder="请输入用户名" />
        </Form.Item>
        <Form.Item name="displayName" label="姓名" rules={[{ required: true }]}>
          <Input placeholder="请输入姓名" />
        </Form.Item>
        <Form.Item name="email" label="邮箱" rules={[{ required: true }, { type: 'email' }]}>
          <Input placeholder="请输入邮箱" />
        </Form.Item>
        <Form.Item name="phone" label="手机号">
          <Input placeholder="请输入手机号" />
        </Form.Item>
        <Form.Item name="status" label="状态" rules={[{ required: true }]}>
          <Select options={[{ label: '启用', value: 'active' }, { label: '禁用', value: 'disabled' }]} />
        </Form.Item>
      </Form>
    </Modal>
  )
}
```

- [ ] **Step 3: Create user barrel export**

```typescript
export { default as UserList } from './user-list'
export { default as UserForm } from './user-form'
```

- [ ] **Step 4: Update user page**

Replace `scm-web/src/app/[locale]/(app)/system/user/page.tsx` with:

```tsx
'use client'

import { UserList } from '@/features/system/components/user'

export default function UserPage() {
  return <UserList />
}
```

- [ ] **Step 5: Commit**

```bash
git add scm-web/src/features/system/components/user/ scm-web/src/app/\[locale\]/\(app\)/system/user/page.tsx
git commit -m "feat(frontend): add user management page with list and form"
```

---

### Task 5: Create Role Management Page

**Files:**
- Create: `scm-web/src/features/system/components/role/role-list.tsx`
- Create: `scm-web/src/features/system/components/role/role-form.tsx`
- Create: `scm-web/src/features/system/components/role/index.ts`
- Modify: `scm-web/src/app/[locale]/(app)/system/role/page.tsx`

- [ ] **Step 1: Create role-list.tsx**

```tsx
'use client'

import { useState } from 'react'
import { Button, Tag, Space, Popconfirm, Table, Card } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import { useRoleList, useDeleteRole } from '../../hooks'
import RoleForm from './role-form'
import type { Role } from '../../types'

export default function RoleList() {
  const [formOpen, setFormOpen] = useState(false)
  const [editingRole, setEditingRole] = useState<Role | null>(null)
  const { data: roles, isLoading } = useRoleList()
  const deleteRole = useDeleteRole()

  const columns = [
    { title: '角色名称', dataIndex: 'name', key: 'name' },
    { title: '角色编码', dataIndex: 'code', key: 'code' },
    { title: '描述', dataIndex: 'description', key: 'description' },
    { title: '状态', dataIndex: 'status', key: 'status', render: (status: string) => <Tag color={status === 'active' ? 'success' : 'error'}>{status === 'active' ? '启用' : '禁用'}</Tag> },
    { title: '用户数', dataIndex: 'userCount', key: 'userCount' },
    {
      title: '操作', key: 'action', render: (_: unknown, record: Role) => (
        <Space>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => { setEditingRole(record); setFormOpen(true) }}>编辑</Button>
          <Popconfirm title="确定删除?" onConfirm={() => deleteRole.mutate(record.id)}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>角色管理</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => { setEditingRole(null); setFormOpen(true) }}>新建角色</Button>
      </div>
      <Card>
        <Table columns={columns} dataSource={roles || []} loading={isLoading} rowKey="id" pagination={false} />
      </Card>
      <RoleForm open={formOpen} onClose={() => setFormOpen(false)} role={editingRole} />
    </div>
  )
}
```

- [ ] **Step 2: Create role-form.tsx**

```tsx
'use client'

import { useEffect } from 'react'
import { Form, Input, Select, Modal, Tree } from 'antd'
import { useCreateRole, useUpdateRole, usePermissionList } from '../../hooks'
import type { Role, Permission } from '../../types'

interface RoleFormProps {
  open: boolean
  onClose: () => void
  role?: Role | null
}

function buildTreeData(permissions: Permission[]) {
  return permissions.map(p => ({
    key: p.code,
    title: p.name,
    children: p.children ? buildTreeData(p.children) : undefined,
  }))
}

export default function RoleForm({ open, onClose, role }: RoleFormProps) {
  const [form] = Form.useForm()
  const createRole = useCreateRole()
  const updateRole = useUpdateRole()
  const { data: permissions } = usePermissionList()

  useEffect(() => {
    if (open) {
      if (role) {
        form.setFieldsValue(role)
      } else {
        form.resetFields()
      }
    }
  }, [open, role, form])

  const handleOk = async () => {
    try {
      const values = await form.validateFields()
      if (role) {
        await updateRole.mutateAsync({ id: role.id, data: values })
      } else {
        await createRole.mutateAsync(values)
      }
      onClose()
    } catch {}
  }

  return (
    <Modal title={role ? '编辑角色' : '新建角色'} open={open} onOk={handleOk} onCancel={onClose} width={600}>
      <Form form={form} layout="vertical">
        <Form.Item name="name" label="角色名称" rules={[{ required: true }]}>
          <Input placeholder="请输入角色名称" />
        </Form.Item>
        <Form.Item name="code" label="角色编码" rules={[{ required: true }]}>
          <Input placeholder="请输入角色编码" disabled={!!role} />
        </Form.Item>
        <Form.Item name="description" label="描述">
          <Input.TextArea placeholder="请输入描述" />
        </Form.Item>
        <Form.Item name="status" label="状态" rules={[{ required: true }]}>
          <Select options={[{ label: '启用', value: 'active' }, { label: '禁用', value: 'disabled' }]} />
        </Form.Item>
        <Form.Item name="permissions" label="权限">
          <Tree checkable treeData={buildTreeData(permissions || [])} defaultExpandAll />
        </Form.Item>
      </Form>
    </Modal>
  )
}
```

- [ ] **Step 3: Create role barrel export and update page**

```typescript
export { default as RoleList } from './role-list'
export { default as RoleForm } from './role-form'
```

Update `scm-web/src/app/[locale]/(app)/system/role/page.tsx`:

```tsx
'use client'

import { RoleList } from '@/features/system/components/role'

export default function RolePage() {
  return <RoleList />
}
```

- [ ] **Step 4: Commit**

```bash
git add scm-web/src/features/system/components/role/ scm-web/src/app/\[locale\]/\(app\)/system/role/page.tsx
git commit -m "feat(frontend): add role management page with permission tree"
```

---

### Task 6: Create Permission and Department Pages

**Files:**
- Create: `scm-web/src/features/system/components/permission/permission-tree.tsx`
- Create: `scm-web/src/features/system/components/permission/index.ts`
- Create: `scm-web/src/features/system/components/dept/dept-tree.tsx`
- Create: `scm-web/src/features/system/components/dept/index.ts`
- Modify: `scm-web/src/app/[locale]/(app)/system/permission/page.tsx`
- Modify: `scm-web/src/app/[locale]/(app)/system/dept/page.tsx`

- [ ] **Step 1: Create permission-tree.tsx**

```tsx
'use client'

import { useState } from 'react'
import { Tree, Button, Card, Tag, Space, Modal, Form, Input, Select, message } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import { usePermissionList, useCreatePermission, useDeletePermission } from '../../hooks'
import type { Permission } from '../../types'

function buildTreeData(permissions: Permission[]): any[] {
  return permissions.map(p => ({
    key: p.id,
    title: (
      <Space>
        <span>{p.name}</span>
        <Tag color={p.type === 'menu' ? 'blue' : p.type === 'button' ? 'green' : 'orange'}>{p.type}</Tag>
        <span style={{ color: '#999', fontSize: 12 }}>{p.code}</span>
      </Space>
    ),
    children: p.children ? buildTreeData(p.children) : undefined,
  }))
}

export default function PermissionTree() {
  const { data: permissions, isLoading } = usePermissionList()
  const createPermission = useCreatePermission()
  const deletePermission = useDeletePermission()
  const [modalOpen, setModalOpen] = useState(false)
  const [form] = Form.useForm()

  const handleCreate = async () => {
    try {
      const values = await form.validateFields()
      await createPermission.mutateAsync(values)
      setModalOpen(false)
      form.resetFields()
    } catch {}
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>权限管理</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>新建权限</Button>
      </div>
      <Card loading={isLoading}>
        <Tree
          treeData={buildTreeData(permissions || [])}
          defaultExpandAll
          showLine
        />
      </Card>
      <Modal title="新建权限" open={modalOpen} onOk={handleCreate} onCancel={() => setModalOpen(false)}>
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="权限名称" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="code" label="权限编码" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="type" label="类型" rules={[{ required: true }]}>
            <Select options={[{ label: '菜单', value: 'menu' }, { label: '按钮', value: 'button' }, { label: '数据', value: 'data' }]} />
          </Form.Item>
          <Form.Item name="path" label="路由路径"><Input /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
```

- [ ] **Step 2: Create dept-tree.tsx**

```tsx
'use client'

import { useState } from 'react'
import { Tree, Button, Card, Space, Modal, Form, Input, message } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import { useDeptList, useCreateDept, useDeleteDept } from '../../hooks'
import type { Department } from '../../types'

function buildTreeData(depts: Department[]): any[] {
  return depts.map(d => ({
    key: d.id,
    title: (
      <Space>
        <span>{d.name}</span>
        {d.leaderName && <span style={{ color: '#999', fontSize: 12 }}>负责人: {d.leaderName}</span>}
      </Space>
    ),
    children: d.children ? buildTreeData(d.children) : undefined,
  }))
}

export default function DeptTree() {
  const { data: depts, isLoading } = useDeptList()
  const createDept = useCreateDept()
  const [modalOpen, setModalOpen] = useState(false)
  const [form] = Form.useForm()

  const handleCreate = async () => {
    try {
      const values = await form.validateFields()
      await createDept.mutateAsync(values)
      setModalOpen(false)
      form.resetFields()
    } catch {}
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>部门管理</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>新建部门</Button>
      </div>
      <Card loading={isLoading}>
        <Tree treeData={buildTreeData(depts || [])} defaultExpandAll showLine />
      </Card>
      <Modal title="新建部门" open={modalOpen} onOk={handleCreate} onCancel={() => setModalOpen(false)}>
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="部门名称" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="code" label="部门编码" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="parentId" label="上级部门"><Input /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
```

- [ ] **Step 3: Create barrel exports and update pages**

Create `scm-web/src/features/system/components/permission/index.ts`:
```typescript
export { default as PermissionTree } from './permission-tree'
```

Create `scm-web/src/features/system/components/dept/index.ts`:
```typescript
export { default as DeptTree } from './dept-tree'
```

Update `scm-web/src/app/[locale]/(app)/system/permission/page.tsx`:
```tsx
'use client'
import { PermissionTree } from '@/features/system/components/permission'
export default function PermissionPage() { return <PermissionTree /> }
```

Update `scm-web/src/app/[locale]/(app)/system/dept/page.tsx`:
```tsx
'use client'
import { DeptTree } from '@/features/system/components/dept'
export default function DeptPage() { return <DeptTree /> }
```

- [ ] **Step 4: Commit**

```bash
git add scm-web/src/features/system/components/permission/ scm-web/src/features/system/components/dept/ scm-web/src/app/\[locale\]/\(app\)/system/permission/ scm-web/src/app/\[locale\]/\(app\)/system/dept/
git commit -m "feat(frontend): add permission tree and department tree pages"
```

---

### Task 7: Create Dictionary Management Page

**Files:**
- Create: `scm-web/src/features/system/components/dict/dict-list.tsx`
- Create: `scm-web/src/features/system/components/dict/dict-item-list.tsx`
- Create: `scm-web/src/features/system/components/dict/index.ts`
- Modify: `scm-web/src/app/[locale]/(app)/system/dictionary/page.tsx`

- [ ] **Step 1: Create dict-list.tsx**

```tsx
'use client'

import { useState } from 'react'
import { Button, Tag, Space, Popconfirm, Table, Card, Modal, Form, Input } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import { useDictTypeList, useCreateDictType, useDeleteDictType } from '../../hooks'
import type { DictType } from '../../types'

export default function DictList() {
  const [modalOpen, setModalOpen] = useState(false)
  const [form] = Form.useForm()
  const { data: dictTypes, isLoading } = useDictTypeList()
  const createDictType = useCreateDictType()
  const deleteDictType = useDeleteDictType()

  const columns = [
    { title: '字典名称', dataIndex: 'name', key: 'name' },
    { title: '字典编码', dataIndex: 'code', key: 'code' },
    { title: '描述', dataIndex: 'description', key: 'description' },
    { title: '状态', dataIndex: 'status', key: 'status', render: (status: string) => <Tag color={status === 'active' ? 'success' : 'error'}>{status === 'active' ? '启用' : '禁用'}</Tag> },
    { title: '字典项', dataIndex: 'itemCount', key: 'itemCount' },
    {
      title: '操作', key: 'action', render: (_: unknown, record: DictType) => (
        <Space>
          <Button type="link" size="small" icon={<EditOutlined />}>编辑</Button>
          <Popconfirm title="确定删除?" onConfirm={() => deleteDictType.mutate(record.id)}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  const handleCreate = async () => {
    try {
      const values = await form.validateFields()
      await createDictType.mutateAsync(values)
      setModalOpen(false)
      form.resetFields()
    } catch {}
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>字典管理</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>新建字典</Button>
      </div>
      <Card>
        <Table columns={columns} dataSource={dictTypes || []} loading={isLoading} rowKey="id" pagination={false} />
      </Card>
      <Modal title="新建字典" open={modalOpen} onOk={handleCreate} onCancel={() => setModalOpen(false)}>
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="字典名称" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="code" label="字典编码" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="description" label="描述"><Input.TextArea /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
```

- [ ] **Step 2: Create barrel export and update page**

Create `scm-web/src/features/system/components/dict/index.ts`:
```typescript
export { default as DictList } from './dict-list'
```

Update `scm-web/src/app/[locale]/(app)/system/dictionary/page.tsx`:
```tsx
'use client'
import { DictList } from '@/features/system/components/dict'
export default function DictionaryPage() { return <DictList /> }
```

- [ ] **Step 3: Commit**

```bash
git add scm-web/src/features/system/components/dict/ scm-web/src/app/\[locale\]/\(app\)/system/dictionary/page.tsx
git commit -m "feat(frontend): add dictionary management page"
```

---

### Task 8: Create Feature Barrel Export and Verify Build

**Files:**
- Create: `scm-web/src/features/system/components/index.ts`
- Create: `scm-web/src/features/system/index.ts`

- [ ] **Step 1: Create barrel exports**

Create `scm-web/src/features/system/components/index.ts`:
```typescript
export * from './user'
export * from './role'
export * from './permission'
export * from './dept'
export * from './dict'
```

Create `scm-web/src/features/system/index.ts`:
```typescript
export * from './types'
export * from './hooks'
export * from './components'
```

- [ ] **Step 2: Verify build**

Run: `cd scm-web && npx tsc --noEmit`
Expected: No new errors

- [ ] **Step 3: Commit**

```bash
git add scm-web/src/features/system/
git commit -m "feat(frontend): add system module barrel exports"
```

---

## Summary

| Task | Files Created | Files Modified |
|------|--------------|----------------|
| 1. Types | 2 | 0 |
| 2. Services | 6 | 0 |
| 3. Hooks | 6 | 0 |
| 4. User Page | 3 | 1 |
| 5. Role Page | 3 | 1 |
| 6. Permission & Dept | 4 | 2 |
| 7. Dictionary | 2 | 1 |
| 8. Barrel Exports | 2 | 0 |
| **Total** | **28** | **5** |

**Phase 3 Deliverables:**
- System module types (User, Role, Permission, Department, DictType, DictItem)
- Services with mock data for all 5 sub-modules
- TanStack Query hooks for CRUD operations
- User management page with list and form
- Role management page with permission tree assignment
- Permission tree page
- Department tree page
- Dictionary management page

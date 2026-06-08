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

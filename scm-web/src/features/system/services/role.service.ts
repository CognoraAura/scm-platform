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

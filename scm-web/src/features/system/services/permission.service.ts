import type { Permission } from '../types'

const mockPermissions: Permission[] = [
  { id: '1', name: '仪表盘', code: 'dashboard:view', type: 'menu', parentId: null, icon: 'DashboardOutlined', path: '/dashboard', sort: 1, status: 'active' },
  { id: '2', name: '商品管理', code: 'product', type: 'menu', parentId: null, icon: 'ShopOutlined', path: '/product', sort: 2, status: 'active', children: [
    { id: '21', name: '商品列表', code: 'product:view', type: 'menu', parentId: '2', path: '/product', sort: 1, status: 'active' },
    { id: '22', name: '创建商品', code: 'product:create', type: 'button', parentId: '2', sort: 2, status: 'active' },
  ]},
  { id: '3', name: '系统管理', code: 'system', type: 'menu', parentId: null, icon: 'SettingOutlined', path: '/system', sort: 10, status: 'active', children: [
    { id: '31', name: '用户管理', code: 'system:user:view', type: 'menu', parentId: '3', path: '/system/user', sort: 1, status: 'active' },
    { id: '32', name: '角色管理', code: 'system:role:view', type: 'menu', parentId: '3', path: '/system/role', sort: 2, status: 'active' },
  ]},
]

export const permissionService = {
  list: async (): Promise<Permission[]> => mockPermissions,
  create: async (data: Partial<Permission>): Promise<Permission> => ({ ...data, id: String(Date.now()), sort: 0, status: 'active' } as Permission),
  update: async (id: string, data: Partial<Permission>): Promise<Permission> => ({ ...data, id } as Permission),
  delete: async (id: string): Promise<void> => {},
}

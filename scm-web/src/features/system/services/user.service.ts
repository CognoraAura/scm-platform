import type { User, PageParams, PageResult } from '../types'

const mockUsers: User[] = [
  { id: '1', username: 'admin', displayName: '管理员', email: 'admin@scm.com', status: 'active', roles: ['admin'], deptId: '1', deptName: '技术部', createdAt: '2026-01-01', updatedAt: '2026-06-01' },
  { id: '2', username: 'zhangsan', displayName: '张三', email: 'zhangsan@scm.com', phone: '13800138000', status: 'active', roles: ['user'], deptId: '2', deptName: '销售部', createdAt: '2026-02-15', updatedAt: '2026-05-20' },
  { id: '3', username: 'lisi', displayName: '李四', email: 'lisi@scm.com', status: 'disabled', roles: ['user'], deptId: '2', deptName: '销售部', createdAt: '2026-03-10', updatedAt: '2026-04-01' },
]

export const userService = {
  list: async (params?: PageParams): Promise<PageResult<User>> => {
    const filtered = mockUsers.filter(u => !params?.keyword || u.username.includes(params.keyword) || u.displayName.includes(params.keyword))
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
  delete: async (id: string): Promise<void> => {},
}

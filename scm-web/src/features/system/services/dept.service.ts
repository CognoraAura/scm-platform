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
  create: async (data: Partial<Department>): Promise<Department> => ({ ...data, id: String(Date.now()), sort: 0, status: 'active' } as Department),
  update: async (id: string, data: Partial<Department>): Promise<Department> => ({ ...data, id } as Department),
  delete: async (id: string): Promise<void> => {},
}

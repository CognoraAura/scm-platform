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

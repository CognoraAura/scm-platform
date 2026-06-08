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

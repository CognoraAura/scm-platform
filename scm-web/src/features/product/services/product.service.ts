import type { Product, Category, Brand, ProductSearchParams, PageResult } from '../types'

const mockCategories: Category[] = [
  { id: '1', name: '电子产品', code: 'electronics', parentId: null, sort: 1, status: 'active', children: [
    { id: '11', name: '手机', code: 'phone', parentId: '1', sort: 1, status: 'active' },
    { id: '12', name: '电脑', code: 'computer', parentId: '1', sort: 2, status: 'active' },
    { id: '13', name: '配件', code: 'accessories', parentId: '1', sort: 3, status: 'active' },
  ]},
  { id: '2', name: '办公用品', code: 'office', parentId: null, sort: 2, status: 'active' },
]

const mockBrands: Brand[] = [
  { id: '1', name: 'Apple', code: 'apple', status: 'active' },
  { id: '2', name: 'Samsung', code: 'samsung', status: 'active' },
  { id: '3', name: 'Huawei', code: 'huawei', status: 'active' },
]

const mockProducts: Product[] = [
  { id: '1', name: 'iPhone 15 Pro', code: 'PRD-001', categoryId: '11', categoryName: '手机', brandId: '1', brandName: 'Apple', status: 'on_sale', price: 7999, costPrice: 5500, unit: '台', skus: [
    { id: '11', productId: '1', skuCode: 'SKU-001-128', name: 'iPhone 15 Pro 128G', price: 7999, stock: 50, status: 'active' },
    { id: '12', productId: '1', skuCode: 'SKU-001-256', name: 'iPhone 15 Pro 256G', price: 8999, stock: 30, status: 'active' },
  ], createdAt: '2026-01-15', updatedAt: '2026-06-01' },
  { id: '2', name: 'MacBook Air M3', code: 'PRD-002', categoryId: '12', categoryName: '电脑', brandId: '1', brandName: 'Apple', status: 'on_sale', price: 9999, costPrice: 7000, unit: '台', skus: [
    { id: '21', productId: '2', skuCode: 'SKU-002-256', name: 'MacBook Air M3 256G', price: 9999, stock: 25, status: 'active' },
  ], createdAt: '2026-02-01', updatedAt: '2026-05-20' },
  { id: '3', name: 'AirPods Pro 2', code: 'PRD-003', categoryId: '13', categoryName: '配件', brandId: '1', brandName: 'Apple', status: 'on_sale', price: 1899, costPrice: 1200, unit: '副', skus: [
    { id: '31', productId: '3', skuCode: 'SKU-003', name: 'AirPods Pro 2', price: 1899, stock: 100, status: 'active' },
  ], createdAt: '2026-03-01', updatedAt: '2026-06-05' },
  { id: '4', name: 'Galaxy S24 Ultra', code: 'PRD-004', categoryId: '11', categoryName: '手机', brandId: '2', brandName: 'Samsung', status: 'draft', price: 9999, unit: '台', skus: [], createdAt: '2026-04-01', updatedAt: '2026-04-01' },
]

export const productService = {
  list: async (params?: ProductSearchParams): Promise<PageResult<Product>> => {
    let filtered = [...mockProducts]
    if (params?.keyword) filtered = filtered.filter(p => p.name.includes(params.keyword!) || p.code.includes(params.keyword!))
    if (params?.categoryId) filtered = filtered.filter(p => p.categoryId === params.categoryId)
    if (params?.status) filtered = filtered.filter(p => p.status === params.status)
    const start = ((params?.current || 1) - 1) * (params?.pageSize || 20)
    return { records: filtered.slice(start, start + (params?.pageSize || 20)), total: filtered.length, current: params?.current || 1, pageSize: params?.pageSize || 20 }
  },
  getById: async (id: string): Promise<Product> => {
    const p = mockProducts.find(x => x.id === id)
    if (!p) throw new Error('Product not found')
    return p
  },
  create: async (data: Partial<Product>): Promise<Product> => ({ ...data, id: String(Date.now()), skus: data.skus || [], createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() } as Product),
  update: async (id: string, data: Partial<Product>): Promise<Product> => {
    const p = mockProducts.find(x => x.id === id)
    if (!p) throw new Error('Product not found')
    return { ...p, ...data, updatedAt: new Date().toISOString() }
  },
  delete: async (id: string): Promise<void> => {},
  listCategories: async (): Promise<Category[]> => mockCategories,
  listBrands: async (): Promise<Brand[]> => mockBrands,
}

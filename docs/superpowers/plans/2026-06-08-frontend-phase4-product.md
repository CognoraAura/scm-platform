# Frontend Phase 4: Product Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build product management module with product list, create/edit form, detail view, category tree, and brand selector.

**Architecture:** Follows feature module pattern. Types → Service → Hooks → Components → Page. Uses Table for list, Modal/Drawer for forms, Descriptions for detail view.

**Tech Stack:** Next.js 15, React 19, Ant Design 5, TanStack Query 5

---

## Tasks

### Task 1: Create Product Types and Service

**Files:**
- Create: `scm-web/src/features/product/types/product.types.ts`
- Create: `scm-web/src/features/product/types/index.ts`
- Create: `scm-web/src/features/product/services/product.service.ts`
- Create: `scm-web/src/features/product/services/index.ts`

- [ ] **Step 1: Create product.types.ts**

```typescript
export interface Product {
  id: string
  name: string
  code: string
  categoryId: string
  categoryName: string
  brandId?: string
  brandName?: string
  description?: string
  status: 'on_sale' | 'off_sale' | 'draft'
  price: number
  costPrice?: number
  unit: string
  imageUrl?: string
  skus: SKU[]
  createdAt: string
  updatedAt: string
}

export interface SKU {
  id: string
  productId: string
  skuCode: string
  name: string
  price: number
  costPrice?: number
  stock: number
  attributes?: Record<string, string>
  status: 'active' | 'disabled'
}

export interface Category {
  id: string
  name: string
  code: string
  parentId: string | null
  sort: number
  status: 'active' | 'disabled'
  children?: Category[]
}

export interface Brand {
  id: string
  name: string
  code: string
  logo?: string
  status: 'active' | 'disabled'
}

export interface ProductSearchParams {
  current?: number
  pageSize?: number
  keyword?: string
  categoryId?: string
  brandId?: string
  status?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  pageSize: number
}
```

- [ ] **Step 2: Create product.service.ts**

```typescript
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
  { id: '4', name: 'Dell', code: 'dell', status: 'active' },
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
  create: async (data: Partial<Product>): Promise<Product> => {
    return { ...data, id: String(Date.now()), skus: data.skus || [], createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() } as Product
  },
  update: async (id: string, data: Partial<Product>): Promise<Product> => {
    const p = mockProducts.find(x => x.id === id)
    if (!p) throw new Error('Product not found')
    return { ...p, ...data, updatedAt: new Date().toISOString() }
  },
  delete: async (id: string): Promise<void> => {},
  listCategories: async (): Promise<Category[]> => mockCategories,
  listBrands: async (): Promise<Brand[]> => mockBrands,
}
```

- [ ] **Step 3: Create barrel exports**

Create `types/index.ts`: `export * from './product.types'`
Create `services/index.ts`: `export * from './product.service'`

- [ ] **Step 4: Commit**

```bash
git add scm-web/src/features/product/
git commit -m "feat(frontend): add product types and service with mock data"
```

---

### Task 2: Create Product Hooks

**Files:**
- Create: `scm-web/src/features/product/hooks/use-products.ts`
- Create: `scm-web/src/features/product/hooks/use-categories.ts`
- Create: `scm-web/src/features/product/hooks/use-brands.ts`
- Create: `scm-web/src/features/product/hooks/index.ts`

- [ ] **Step 1: Create use-products.ts**

```typescript
'use client'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { productService } from '../services/product.service'
import type { ProductSearchParams } from '../types'

export function useProductList(params?: ProductSearchParams) {
  return useQuery({ queryKey: ['products', 'list', params], queryFn: () => productService.list(params) })
}
export function useProductDetail(id: string) {
  return useQuery({ queryKey: ['products', 'detail', id], queryFn: () => productService.getById(id), enabled: !!id })
}
export function useCreateProduct() {
  const qc = useQueryClient()
  return useMutation({ mutationFn: (data: Parameters<typeof productService.create>[0]) => productService.create(data), onSuccess: () => { qc.invalidateQueries({ queryKey: ['products'] }); message.success('创建成功') } })
}
export function useUpdateProduct() {
  const qc = useQueryClient()
  return useMutation({ mutationFn: ({ id, data }: { id: string; data: Parameters<typeof productService.update>[1] }) => productService.update(id, data), onSuccess: () => { qc.invalidateQueries({ queryKey: ['products'] }); message.success('更新成功') } })
}
export function useDeleteProduct() {
  const qc = useQueryClient()
  return useMutation({ mutationFn: (id: string) => productService.delete(id), onSuccess: () => { qc.invalidateQueries({ queryKey: ['products'] }); message.success('删除成功') } })
}
```

- [ ] **Step 2: Create use-categories.ts**

```typescript
'use client'
import { useQuery } from '@tanstack/react-query'
import { productService } from '../services/product.service'

export function useCategoryList() {
  return useQuery({ queryKey: ['categories'], queryFn: () => productService.listCategories() })
}
```

- [ ] **Step 3: Create use-brands.ts**

```typescript
'use client'
import { useQuery } from '@tanstack/react-query'
import { productService } from '../services/product.service'

export function useBrandList() {
  return useQuery({ queryKey: ['brands'], queryFn: () => productService.listBrands() })
}
```

- [ ] **Step 4: Create hooks barrel export**

```typescript
export * from './use-products'
export * from './use-categories'
export * from './use-brands'
```

- [ ] **Step 5: Commit**

```bash
git add scm-web/src/features/product/hooks/
git commit -m "feat(frontend): add product hooks"
```

---

### Task 3: Create Product Components

**Files:**
- Create: `scm-web/src/features/product/components/product-list.tsx`
- Create: `scm-web/src/features/product/components/product-form.tsx`
- Create: `scm-web/src/features/product/components/product-detail.tsx`
- Create: `scm-web/src/features/product/components/category-tree.tsx`
- Create: `scm-web/src/features/product/components/index.ts`

- [ ] **Step 1: Create product-list.tsx**

```tsx
'use client'
import { useState } from 'react'
import { Button, Tag, Space, Popconfirm, Table, Card, Input, Select } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, EyeOutlined } from '@ant-design/icons'
import { useRouter } from 'next/navigation'
import { useProductList, useDeleteProduct, useCategoryList } from '../hooks'
import type { Product } from '../types'

const statusMap: Record<string, { color: string; label: string }> = {
  on_sale: { color: 'success', label: '在售' },
  off_sale: { color: 'error', label: '下架' },
  draft: { color: 'default', label: '草稿' },
}

export default function ProductList() {
  const [current, setCurrent] = useState(1)
  const [keyword, setKeyword] = useState('')
  const [categoryId, setCategoryId] = useState<string>()
  const [status, setStatus] = useState<string>()
  const router = useRouter()
  const { data, isLoading } = useProductList({ current, keyword, categoryId, status })
  const deleteProduct = useDeleteProduct()
  const { data: categories } = useCategoryList()

  const columns = [
    { title: '商品编码', dataIndex: 'code', key: 'code', width: 120 },
    { title: '商品名称', dataIndex: 'name', key: 'name', render: (text: string, record: Product) => <a onClick={() => router.push(`/product/${record.id}`)}>{text}</a> },
    { title: '分类', dataIndex: 'categoryName', key: 'categoryName', width: 100 },
    { title: '品牌', dataIndex: 'brandName', key: 'brandName', width: 100 },
    { title: '价格', dataIndex: 'price', key: 'price', width: 100, align: 'right', render: (v: number) => `¥${v?.toLocaleString()}` },
    { title: 'SKU数', key: 'skuCount', width: 80, align: 'center', render: (_: unknown, r: Product) => r.skus?.length || 0 },
    { title: '状态', dataIndex: 'status', key: 'status', width: 80, render: (s: string) => { const m = statusMap[s]; return m ? <Tag color={m.color}>{m.label}</Tag> : s } },
    { title: '操作', key: 'action', width: 160, fixed: 'right' as const, render: (_: unknown, record: Product) => (
      <Space>
        <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => router.push(`/product/${record.id}`)}>查看</Button>
        <Button type="link" size="small" icon={<EditOutlined />} onClick={() => router.push(`/product/${record.id}/edit`)}>编辑</Button>
        <Popconfirm title="确定删除?" onConfirm={() => deleteProduct.mutate(record.id)}>
          <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
        </Popconfirm>
      </Space>
    )},
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>商品管理</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => router.push('/product/create')}>新建商品</Button>
      </div>
      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input.Search placeholder="搜索商品名称/编码" style={{ width: 240 }} onSearch={(v) => { setKeyword(v); setCurrent(1) }} allowClear />
          <Select placeholder="选择分类" style={{ width: 160 }} allowClear onChange={(v) => { setCategoryId(v); setCurrent(1) }}
            options={categories?.flatMap(c => [{ label: c.name, value: c.id }, ...(c.children?.map(ch => ({ label: `  ${ch.name}`, value: ch.id })) || [])])} />
          <Select placeholder="选择状态" style={{ width: 120 }} allowClear onChange={(v) => { setStatus(v); setCurrent(1) }}
            options={[{ label: '在售', value: 'on_sale' }, { label: '下架', value: 'off_sale' }, { label: '草稿', value: 'draft' }]} />
        </Space>
      </Card>
      <Card>
        <Table columns={columns} dataSource={data?.records || []} loading={isLoading} rowKey="id" scroll={{ x: 1200 }}
          pagination={{ current, pageSize: 20, total: data?.total, onChange: setCurrent, showSizeChanger: true, showTotal: (t) => `共 ${t} 条` }} />
      </Card>
    </div>
  )
}
```

- [ ] **Step 2: Create product-form.tsx**

```tsx
'use client'
import { useEffect } from 'react'
import { Form, Input, InputNumber, Select, Card, Button, Space } from 'antd'
import { useRouter } from 'next/navigation'
import { useCreateProduct, useUpdateProduct, useProductDetail, useCategoryList, useBrandList } from '../hooks'

interface ProductFormProps {
  id?: string
}

export default function ProductForm({ id }: ProductFormProps) {
  const [form] = Form.useForm()
  const router = useRouter()
  const { data: product } = useProductDetail(id || '')
  const { data: categories } = useCategoryList()
  const { data: brands } = useBrandList()
  const createProduct = useCreateProduct()
  const updateProduct = useUpdateProduct()

  useEffect(() => {
    if (product) form.setFieldsValue(product)
  }, [product, form])

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      if (id) { await updateProduct.mutateAsync({ id, data: values }) }
      else { await createProduct.mutateAsync(values) }
      router.push('/product')
    } catch {}
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>{id ? '编辑商品' : '新建商品'}</h2>
        <Space>
          <Button onClick={() => router.push('/product')}>取消</Button>
          <Button type="primary" onClick={handleSubmit} loading={createProduct.isPending || updateProduct.isPending}>保存</Button>
        </Space>
      </div>
      <Card title="基本信息">
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="商品名称" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="code" label="商品编码" rules={[{ required: true }]}><Input disabled={!!id} /></Form.Item>
          <Form.Item name="categoryId" label="分类" rules={[{ required: true }]}>
            <Select options={categories?.flatMap(c => [{ label: c.name, value: c.id }, ...(c.children?.map(ch => ({ label: ch.name, value: ch.id })) || [])])} />
          </Form.Item>
          <Form.Item name="brandId" label="品牌">
            <Select options={brands?.map(b => ({ label: b.name, value: b.id }))} allowClear />
          </Form.Item>
          <Form.Item name="price" label="售价" rules={[{ required: true }]}><InputNumber min={0} prefix="¥" style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="costPrice" label="成本价"><InputNumber min={0} prefix="¥" style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="unit" label="单位" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="status" label="状态" rules={[{ required: true }]}>
            <Select options={[{ label: '在售', value: 'on_sale' }, { label: '下架', value: 'off_sale' }, { label: '草稿', value: 'draft' }]} />
          </Form.Item>
          <Form.Item name="description" label="描述"><Input.TextArea rows={4} /></Form.Item>
        </Form>
      </Card>
    </div>
  )
}
```

- [ ] **Step 3: Create product-detail.tsx**

```tsx
'use client'
import { Descriptions, Card, Tag, Table, Button, Space } from 'antd'
import { EditOutlined } from '@ant-design/icons'
import { useRouter } from 'next/navigation'
import { useProductDetail } from '../hooks'

const statusMap: Record<string, { color: string; label: string }> = {
  on_sale: { color: 'success', label: '在售' },
  off_sale: { color: 'error', label: '下架' },
  draft: { color: 'default', label: '草稿' },
}

interface ProductDetailProps {
  id: string
}

export default function ProductDetail({ id }: ProductDetailProps) {
  const { data: product, isLoading } = useProductDetail(id)
  const router = useRouter()

  if (!product) return null

  const skuColumns = [
    { title: 'SKU编码', dataIndex: 'skuCode', key: 'skuCode' },
    { title: '名称', dataIndex: 'name', key: 'name' },
    { title: '价格', dataIndex: 'price', key: 'price', render: (v: number) => `¥${v?.toLocaleString()}` },
    { title: '库存', dataIndex: 'stock', key: 'stock' },
    { title: '状态', dataIndex: 'status', key: 'status', render: (s: string) => <Tag color={s === 'active' ? 'success' : 'error'}>{s === 'active' ? '启用' : '禁用'}</Tag> },
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>商品详情</h2>
        <Space>
          <Button onClick={() => router.push('/product')}>返回</Button>
          <Button type="primary" icon={<EditOutlined />} onClick={() => router.push(`/product/${id}/edit`)}>编辑</Button>
        </Space>
      </div>
      <Card title="基本信息" loading={isLoading} style={{ marginBottom: 16 }}>
        <Descriptions column={2}>
          <Descriptions.Item label="商品名称">{product.name}</Descriptions.Item>
          <Descriptions.Item label="商品编码">{product.code}</Descriptions.Item>
          <Descriptions.Item label="分类">{product.categoryName}</Descriptions.Item>
          <Descriptions.Item label="品牌">{product.brandName || '-'}</Descriptions.Item>
          <Descriptions.Item label="售价">¥{product.price?.toLocaleString()}</Descriptions.Item>
          <Descriptions.Item label="成本价">{product.costPrice ? `¥${product.costPrice.toLocaleString()}` : '-'}</Descriptions.Item>
          <Descriptions.Item label="单位">{product.unit}</Descriptions.Item>
          <Descriptions.Item label="状态"><Tag color={statusMap[product.status]?.color}>{statusMap[product.status]?.label}</Tag></Descriptions.Item>
          <Descriptions.Item label="描述" span={2}>{product.description || '-'}</Descriptions.Item>
        </Descriptions>
      </Card>
      <Card title="SKU列表">
        <Table columns={skuColumns} dataSource={product.skus || []} rowKey="id" pagination={false} size="small" />
      </Card>
    </div>
  )
}
```

- [ ] **Step 4: Create category-tree.tsx**

```tsx
'use client'
import { Tree, Card, Button } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { useCategoryList } from '../hooks'
import type { Category } from '../types'

function buildTreeData(categories: Category[]): any[] {
  return categories.map(c => ({
    key: c.id,
    title: c.name,
    children: c.children ? buildTreeData(c.children) : undefined,
  }))
}

export default function CategoryTree() {
  const { data: categories, isLoading } = useCategoryList()

  return (
    <Card title="商品分类" extra={<Button type="primary" size="small" icon={<PlusOutlined />}>新建分类</Button>} loading={isLoading}>
      <Tree treeData={buildTreeData(categories || [])} defaultExpandAll showLine />
    </Card>
  )
}
```

- [ ] **Step 5: Create barrel export**

```typescript
export { default as ProductList } from './product-list'
export { default as ProductForm } from './product-form'
export { default as ProductDetail } from './product-detail'
export { default as CategoryTree } from './category-tree'
```

- [ ] **Step 6: Commit**

```bash
git add scm-web/src/features/product/components/
git commit -m "feat(frontend): add product components (list, form, detail, category tree)"
```

---

### Task 4: Create Product Pages

**Files:**
- Modify: `scm-web/src/app/[locale]/(app)/product/page.tsx`
- Create: `scm-web/src/app/[locale]/(app)/product/create/page.tsx`
- Create: `scm-web/src/app/[locale]/(app)/product/[id]/page.tsx`
- Create: `scm-web/src/app/[locale]/(app)/product/[id]/edit/page.tsx`

- [ ] **Step 1: Update product list page**

```tsx
// scm-web/src/app/[locale]/(app)/product/page.tsx
'use client'
import { ProductList } from '@/features/product/components'
export default function ProductPage() { return <ProductList /> }
```

- [ ] **Step 2: Create product create page**

```tsx
// scm-web/src/app/[locale]/(app)/product/create/page.tsx
'use client'
import { ProductForm } from '@/features/product/components'
export default function CreateProductPage() { return <ProductForm /> }
```

- [ ] **Step 3: Create product detail page**

```tsx
// scm-web/src/app/[locale]/(app)/product/[id]/page.tsx
'use client'
import { ProductDetail } from '@/features/product/components'
export default function ProductDetailPage({ params }: { params: Promise<{ id: string }> }) {
  // In Next.js 15, params is a Promise
  return <ProductDetailWrapper />
}

function ProductDetailWrapper() {
  // Use usePathname to get the id
  const pathname = usePathname()
  const id = pathname.split('/').pop() || ''
  return <ProductDetail id={id} />
}
```

- [ ] **Step 4: Create product edit page**

```tsx
// scm-web/src/app/[locale]/(app)/product/[id]/edit/page.tsx
'use client'
import { ProductForm } from '@/features/product/components'
import { usePathname } from 'next/navigation'

export default function EditProductPage() {
  const pathname = usePathname()
  const id = pathname.split('/')[2] || ''
  return <ProductForm id={id} />
}
```

- [ ] **Step 5: Create feature barrel export**

Create `scm-web/src/features/product/index.ts`:
```typescript
export * from './types'
export * from './hooks'
export * from './components'
```

- [ ] **Step 6: Verify build**

Run: `cd scm-web && npx tsc --noEmit`
Expected: No new errors

- [ ] **Step 7: Commit**

```bash
git add scm-web/src/features/product/ scm-web/src/app/\[locale\]/\(app\)/product/
git commit -m "feat(frontend): add product pages (list, create, detail, edit)"
```

---

## Summary

| Task | Files Created | Files Modified |
|------|--------------|----------------|
| 1. Types & Service | 4 | 0 |
| 2. Hooks | 4 | 0 |
| 3. Components | 5 | 0 |
| 4. Pages | 4 | 1 |
| **Total** | **17** | **1** |

**Phase 4 Deliverables:**
- Product types (Product, SKU, Category, Brand)
- Product service with mock data
- Product CRUD hooks
- Product list with search, category filter, status filter
- Product create/edit form
- Product detail view with SKU table
- Category tree component

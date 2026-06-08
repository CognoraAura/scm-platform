'use client'
import { useEffect } from 'react'
import { Form, Input, InputNumber, Select, Card, Button, Space } from 'antd'
import { useRouter } from 'next/navigation'
import { useCreateProduct, useUpdateProduct, useProductDetail, useCategoryList, useBrandList } from '../hooks'

interface ProductFormProps { id?: string }

export default function ProductForm({ id }: ProductFormProps) {
  const [form] = Form.useForm()
  const router = useRouter()
  const { data: product } = useProductDetail(id || '')
  const { data: categories } = useCategoryList()
  const { data: brands } = useBrandList()
  const createProduct = useCreateProduct()
  const updateProduct = useUpdateProduct()

  useEffect(() => { if (product) form.setFieldsValue(product) }, [product, form])

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

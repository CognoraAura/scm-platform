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
    { title: '价格', dataIndex: 'price', key: 'price', width: 100, align: 'right' as const, render: (v: number) => `¥${v?.toLocaleString()}` },
    { title: 'SKU数', key: 'skuCount', width: 80, align: 'center' as const, render: (_: unknown, r: Product) => r.skus?.length || 0 },
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

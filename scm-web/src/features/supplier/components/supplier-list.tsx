'use client'
import { useState } from 'react'
import { Button, Tag, Space, Table, Card, Input, Select, Rate } from 'antd'
import { EyeOutlined } from '@ant-design/icons'
import { useRouter } from 'next/navigation'
import { useSupplierList } from '../hooks'
import type { Supplier } from '../types'

export default function SupplierList() {
  const [current, setCurrent] = useState(1)
  const [keyword, setKeyword] = useState('')
  const [status, setStatus] = useState<string>()
  const router = useRouter()
  const { data, isLoading } = useSupplierList({ current, keyword, status })

  const columns = [
    { title: '供应商编码', dataIndex: 'code', key: 'code', width: 120 },
    { title: '供应商名称', dataIndex: 'name', key: 'name', render: (text: string, record: Supplier) => <a onClick={() => router.push(`/supplier/${record.id}`)}>{text}</a> },
    { title: '联系人', dataIndex: 'contactPerson', key: 'contactPerson', width: 100 },
    { title: '联系电话', dataIndex: 'contactPhone', key: 'contactPhone', width: 140 },
    { title: '评级', dataIndex: 'rating', key: 'rating', width: 140, render: (v: number) => <Rate disabled defaultValue={v} /> },
    { title: '状态', dataIndex: 'status', key: 'status', width: 80, render: (s: string) => <Tag color={s === 'active' ? 'success' : 'error'}>{s === 'active' ? '启用' : '禁用'}</Tag> },
    { title: '操作', key: 'action', width: 80, render: (_: unknown, record: Supplier) => (
      <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => router.push(`/supplier/${record.id}`)}>查看</Button>
    )},
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>供应商管理</h2>
      </div>
      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input.Search placeholder="搜索供应商名称/编码" style={{ width: 240 }} onSearch={(v) => { setKeyword(v); setCurrent(1) }} allowClear />
          <Select placeholder="状态" style={{ width: 120 }} allowClear onChange={(v) => { setStatus(v); setCurrent(1) }}
            options={[{ label: '启用', value: 'active' }, { label: '禁用', value: 'disabled' }]} />
        </Space>
      </Card>
      <Card>
        <Table columns={columns} dataSource={data?.records || []} loading={isLoading} rowKey="id"
          pagination={{ current, pageSize: 20, total: data?.total, onChange: setCurrent }} />
      </Card>
    </div>
  )
}

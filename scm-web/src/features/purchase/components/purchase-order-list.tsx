'use client'
import { useState } from 'react'
import { Button, Tag, Space, Table, Card, Input, Select } from 'antd'
import { EyeOutlined } from '@ant-design/icons'
import { useRouter } from 'next/navigation'
import { usePurchaseOrderList } from '../hooks'
import { purchaseService } from '../services/purchase.service'
import type { PurchaseOrder } from '../types'

export default function PurchaseOrderList() {
  const [current, setCurrent] = useState(1)
  const [keyword, setKeyword] = useState('')
  const [status, setStatus] = useState<string>()
  const router = useRouter()
  const { data, isLoading } = usePurchaseOrderList({ current, keyword, status })
  const statusMap = purchaseService.getStatusMap()

  const columns = [
    { title: '采购单号', dataIndex: 'purchaseNo', key: 'purchaseNo', render: (text: string, record: PurchaseOrder) => <a onClick={() => router.push(`/purchase/${record.id}`)}>{text}</a> },
    { title: '供应商', dataIndex: 'supplierName', key: 'supplierName' },
    { title: '商品数', key: 'itemCount', width: 80, align: 'center' as const, render: (_: unknown, r: PurchaseOrder) => r.items?.length || 0 },
    { title: '总金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 120, align: 'right' as const, render: (v: number) => `¥${v?.toLocaleString()}` },
    { title: '状态', dataIndex: 'status', key: 'status', width: 100, render: (s: string) => { const cfg = statusMap[s]; return cfg ? <Tag color={cfg.color}>{cfg.label}</Tag> : s } },
    { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 120 },
    { title: '操作', key: 'action', width: 80, render: (_: unknown, record: PurchaseOrder) => (
      <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => router.push(`/purchase/${record.id}`)}>查看</Button>
    )},
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>采购管理</h2>
      </div>
      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input.Search placeholder="搜索采购单号/供应商" style={{ width: 240 }} onSearch={(v) => { setKeyword(v); setCurrent(1) }} allowClear />
          <Select placeholder="状态" style={{ width: 120 }} allowClear onChange={(v) => { setStatus(v); setCurrent(1) }}
            options={Object.entries(statusMap).map(([key, val]) => ({ label: val.label, value: key }))} />
        </Space>
      </Card>
      <Card>
        <Table columns={columns} dataSource={data?.records || []} loading={isLoading} rowKey="id"
          pagination={{ current, pageSize: 20, total: data?.total, onChange: setCurrent }} />
      </Card>
    </div>
  )
}

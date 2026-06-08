'use client'
import { useState } from 'react'
import { Button, Tag, Space, Table, Card, Input, Select } from 'antd'
import { EyeOutlined } from '@ant-design/icons'
import { useRouter } from 'next/navigation'
import { useOrderList } from '../hooks'
import { orderService } from '../services/order.service'
import type { Order } from '../types'

export default function OrderList() {
  const [current, setCurrent] = useState(1)
  const [keyword, setKeyword] = useState('')
  const [status, setStatus] = useState<string>()
  const router = useRouter()
  const { data, isLoading } = useOrderList({ current, keyword, status })
  const statusFlow = orderService.getStatusFlow()

  const columns = [
    { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', render: (text: string, record: Order) => <a onClick={() => router.push(`/order/${record.id}`)}>{text}</a> },
    { title: '客户', dataIndex: 'customerName', key: 'customerName', width: 100 },
    { title: '商品数', key: 'itemCount', width: 80, align: 'center' as const, render: (_: unknown, r: Order) => r.items?.length || 0 },
    { title: '订单金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 100, align: 'right' as const, render: (v: number) => `¥${v?.toLocaleString()}` },
    { title: '实付', dataIndex: 'payAmount', key: 'payAmount', width: 100, align: 'right' as const, render: (v: number) => `¥${v?.toLocaleString()}` },
    { title: '状态', dataIndex: 'status', key: 'status', width: 100, render: (s: keyof typeof statusFlow) => { const cfg = statusFlow[s]; return cfg ? <Tag color={cfg.color}>{cfg.label}</Tag> : s } },
    { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 160 },
    { title: '操作', key: 'action', width: 100, fixed: 'right' as const, render: (_: unknown, record: Order) => (
      <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => router.push(`/order/${record.id}`)}>查看</Button>
    )},
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>订单管理</h2>
      </div>
      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input.Search placeholder="搜索订单号/客户" style={{ width: 240 }} onSearch={(v) => { setKeyword(v); setCurrent(1) }} allowClear />
          <Select placeholder="订单状态" style={{ width: 120 }} allowClear onChange={(v) => { setStatus(v); setCurrent(1) }}
            options={Object.entries(statusFlow).map(([key, val]) => ({ label: val.label, value: key }))} />
        </Space>
      </Card>
      <Card>
        <Table columns={columns} dataSource={data?.records || []} loading={isLoading} rowKey="id" scroll={{ x: 1000 }}
          pagination={{ current, pageSize: 20, total: data?.total, onChange: setCurrent, showTotal: (t) => `共 ${t} 条` }} />
      </Card>
    </div>
  )
}

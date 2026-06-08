'use client'
import { useState } from 'react'
import { Button, Tag, Space, Table, Card, Input, Select, Progress } from 'antd'
import { useInventoryList } from '../hooks'
import type { InventoryItem } from '../types'

const statusConfig: Record<string, { color: string; label: string }> = {
  normal: { color: 'success', label: '正常' },
  low: { color: 'warning', label: '偏低' },
  critical: { color: 'error', label: '紧急' },
  out: { color: 'default', label: '缺货' },
}

export default function InventoryList() {
  const [current, setCurrent] = useState(1)
  const [keyword, setKeyword] = useState('')
  const [status, setStatus] = useState<string>()
  const { data, isLoading } = useInventoryList({ current, keyword, status })

  const columns = [
    { title: 'SKU编码', dataIndex: 'skuCode', key: 'skuCode', width: 140 },
    { title: '商品名称', dataIndex: 'productName', key: 'productName' },
    { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 100 },
    { title: '总库存', dataIndex: 'totalStock', key: 'totalStock', width: 80, align: 'center' as const },
    { title: '可用', dataIndex: 'availableStock', key: 'availableStock', width: 80, align: 'center' as const },
    { title: '预留', dataIndex: 'reservedStock', key: 'reservedStock', width: 80, align: 'center' as const },
    { title: '库存状态', key: 'stockLevel', width: 120, render: (_: unknown, r: InventoryItem) => {
      const percent = Math.round((r.totalStock / r.threshold) * 100)
      return <Progress percent={Math.min(percent, 100)} size="small" status={r.status === 'out' || r.status === 'critical' ? 'exception' : r.status === 'low' ? 'active' : 'success'} />
    }},
    { title: '状态', dataIndex: 'status', key: 'status', width: 80, render: (s: string) => { const c = statusConfig[s]; return c ? <Tag color={c.color}>{c.label}</Tag> : s } },
    { title: '更新时间', dataIndex: 'lastUpdatedAt', key: 'lastUpdatedAt', width: 160 },
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>库存管理</h2>
      </div>
      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input.Search placeholder="搜索SKU/商品名称" style={{ width: 240 }} onSearch={(v) => { setKeyword(v); setCurrent(1) }} allowClear />
          <Select placeholder="库存状态" style={{ width: 120 }} allowClear onChange={(v) => { setStatus(v); setCurrent(1) }}
            options={[{ label: '正常', value: 'normal' }, { label: '偏低', value: 'low' }, { label: '紧急', value: 'critical' }, { label: '缺货', value: 'out' }]} />
        </Space>
      </Card>
      <Card>
        <Table columns={columns} dataSource={data?.records || []} loading={isLoading} rowKey="id" scroll={{ x: 1000 }}
          pagination={{ current, pageSize: 20, total: data?.total, onChange: setCurrent, showTotal: (t) => `共 ${t} 条` }} />
      </Card>
    </div>
  )
}

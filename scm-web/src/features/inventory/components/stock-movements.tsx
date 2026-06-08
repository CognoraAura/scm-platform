'use client'
import { useState } from 'react'
import { Table, Card, Tag, Select } from 'antd'
import { useStockMovements } from '../hooks'
import type { StockMovement } from '../types'

const typeConfig: Record<string, { color: string; label: string }> = {
  inbound: { color: 'success', label: '入库' },
  outbound: { color: 'error', label: '出库' },
  adjust: { color: 'warning', label: '调整' },
  transfer: { color: 'processing', label: '调拨' },
}

export default function StockMovements() {
  const [type, setType] = useState<string>()
  const { data: movements, isLoading } = useStockMovements()
  const filtered = type ? movements?.filter(m => m.type === type) : movements

  const columns = [
    { title: 'SKU编码', dataIndex: 'skuCode', key: 'skuCode', width: 140 },
    { title: '商品名称', dataIndex: 'productName', key: 'productName' },
    { title: '类型', dataIndex: 'type', key: 'type', width: 80, render: (s: string) => { const c = typeConfig[s]; return c ? <Tag color={c.color}>{c.label}</Tag> : s } },
    { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 80, align: 'center' as const, render: (v: number) => <span style={{ color: v > 0 ? '#52c41a' : '#ff4d4f' }}>{v > 0 ? '+' : ''}{v}</span> },
    { title: '调整前', dataIndex: 'beforeStock', key: 'beforeStock', width: 80, align: 'center' as const },
    { title: '调整后', dataIndex: 'afterStock', key: 'afterStock', width: 80, align: 'center' as const },
    { title: '原因', dataIndex: 'reason', key: 'reason' },
    { title: '操作人', dataIndex: 'operator', key: 'operator', width: 100 },
    { title: '时间', dataIndex: 'createdAt', key: 'createdAt', width: 160 },
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>库存流水</h2>
        <Select placeholder="流水类型" style={{ width: 120 }} allowClear onChange={setType}
          options={[{ label: '入库', value: 'inbound' }, { label: '出库', value: 'outbound' }, { label: '调整', value: 'adjust' }, { label: '调拨', value: 'transfer' }]} />
      </div>
      <Card>
        <Table columns={columns} dataSource={filtered || []} loading={isLoading} rowKey="id" scroll={{ x: 1000 }} pagination={{ pageSize: 20 }} />
      </Card>
    </div>
  )
}

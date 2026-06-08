'use client'
import { Table, Card, Tag } from 'antd'
import { useInboundList } from '../hooks'

const statusColors: Record<string, string> = { pending: 'warning', receiving: 'processing', completed: 'success', cancelled: 'error' }

export default function InboundList() {
  const { data: inboundOrders, isLoading } = useInboundList()

  const columns = [
    { title: '入库单号', dataIndex: 'inboundNo', key: 'inboundNo' },
    { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName' },
    { title: '供应商', dataIndex: 'supplierName', key: 'supplierName' },
    { title: '商品数', key: 'itemCount', width: 80, align: 'center' as const, render: (_: unknown, r: { items?: unknown[] }) => r.items?.length || 0 },
    { title: '状态', dataIndex: 'status', key: 'status', width: 100, render: (s: string, r: { statusLabel: string }) => <Tag color={statusColors[s]}>{r.statusLabel}</Tag> },
    { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 120 },
  ]

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>入库管理</h2>
      <Card>
        <Table columns={columns} dataSource={inboundOrders || []} loading={isLoading} rowKey="id" pagination={false} />
      </Card>
    </div>
  )
}

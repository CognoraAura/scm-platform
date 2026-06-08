'use client'

import { Card, Table, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { RecentOrder } from '../types'

interface RecentOrdersProps {
  orders: RecentOrder[]
  loading?: boolean
}

const statusColors: Record<string, string> = {
  PENDING: 'warning', PAID: 'processing', SHIPPED: 'purple', COMPLETED: 'success', CANCELLED: 'error',
}

const columns: ColumnsType<RecentOrder> = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', render: (text: string) => <a>{text}</a> },
  { title: '客户', dataIndex: 'customerName', key: 'customerName' },
  { title: '金额', dataIndex: 'amount', key: 'amount', align: 'right', render: (val: number) => `¥${val.toLocaleString()}` },
  { title: '状态', dataIndex: 'status', key: 'status', render: (status: string, record) => <Tag color={statusColors[status] || 'default'}>{record.statusLabel}</Tag> },
  { title: '时间', dataIndex: 'createdAt', key: 'createdAt' },
]

export default function RecentOrders({ orders, loading }: RecentOrdersProps) {
  return (
    <Card title="最近订单">
      <Table columns={columns} dataSource={orders} loading={loading} rowKey="id" pagination={false} size="small" />
    </Card>
  )
}

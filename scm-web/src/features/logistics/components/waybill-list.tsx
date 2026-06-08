'use client'
import { Table, Card, Tag, Button } from 'antd'
import { EnvironmentOutlined } from '@ant-design/icons'
import { useWaybillList } from '../hooks'

const statusColors: Record<string, string> = { pending: 'default', picked: 'processing', in_transit: 'blue', delivered: 'success', exception: 'error' }

export default function WaybillList() {
  const { data: waybills, isLoading } = useWaybillList()

  const columns = [
    { title: '运单号', dataIndex: 'waybillNo', key: 'waybillNo' },
    { title: '订单号', dataIndex: 'orderNo', key: 'orderNo' },
    { title: '承运商', dataIndex: 'carrierName', key: 'carrierName' },
    { title: '快递单号', dataIndex: 'trackingNo', key: 'trackingNo' },
    { title: '收件人', dataIndex: 'receiverName', key: 'receiverName', width: 100 },
    { title: '状态', dataIndex: 'status', key: 'status', width: 100, render: (s: string, r: { statusLabel: string }) => <Tag color={statusColors[s]}>{r.statusLabel}</Tag> },
    { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 120 },
  ]

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>运单管理</h2>
      <Card>
        <Table columns={columns} dataSource={waybills || []} loading={isLoading} rowKey="id" pagination={false} />
      </Card>
    </div>
  )
}

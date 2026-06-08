'use client'
import { Descriptions, Card, Tag, Table, Button, Timeline } from 'antd'
import { useRouter } from 'next/navigation'
import { useOrderDetail } from '../hooks'
import { orderService } from '../services/order.service'
import OrderStatusFlow from './order-status-flow'

interface OrderDetailProps { id: string }

export default function OrderDetail({ id }: OrderDetailProps) {
  const { data: order, isLoading } = useOrderDetail(id)
  const router = useRouter()
  const statusFlow = orderService.getStatusFlow()

  if (!order) return null

  const itemColumns = [
    { title: 'SKU编码', dataIndex: 'skuCode', key: 'skuCode', width: 140 },
    { title: '商品名称', dataIndex: 'productName', key: 'productName' },
    { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice', width: 100, align: 'right' as const, render: (v: number) => `¥${v?.toLocaleString()}` },
    { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 80, align: 'center' as const },
    { title: '小计', dataIndex: 'totalPrice', key: 'totalPrice', width: 100, align: 'right' as const, render: (v: number) => `¥${v?.toLocaleString()}` },
  ]

  const timelineItems: { children: string; color?: string }[] = []
  if (order.createdAt) timelineItems.push({ children: `创建订单 ${order.createdAt}` })
  if (order.payTime) timelineItems.push({ children: `付款 ${order.payTime}`, color: 'green' })
  if (order.shipTime) timelineItems.push({ children: `发货 ${order.shipTime}`, color: 'green' })
  if (order.completeTime) timelineItems.push({ children: `完成 ${order.completeTime}`, color: 'green' })
  if (order.cancelTime) timelineItems.push({ children: `取消 ${order.cancelTime}`, color: 'red' })

  const cfg = statusFlow[order.status]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>订单详情</h2>
        <Button onClick={() => router.push('/order')}>返回列表</Button>
      </div>
      <OrderStatusFlow status={order.status} />
      <Card title="基本信息" loading={isLoading} style={{ marginTop: 16 }}>
        <Descriptions column={2}>
          <Descriptions.Item label="订单号">{order.orderNo}</Descriptions.Item>
          <Descriptions.Item label="状态"><Tag color={cfg?.color}>{cfg?.label}</Tag></Descriptions.Item>
          <Descriptions.Item label="客户">{order.customerName}</Descriptions.Item>
          <Descriptions.Item label="电话">{order.customerPhone || '-'}</Descriptions.Item>
          <Descriptions.Item label="订单金额">¥{order.totalAmount?.toLocaleString()}</Descriptions.Item>
          <Descriptions.Item label="优惠">¥{order.discountAmount?.toLocaleString()}</Descriptions.Item>
          <Descriptions.Item label="实付金额">¥{order.payAmount?.toLocaleString()}</Descriptions.Item>
          <Descriptions.Item label="创建时间">{order.createdAt}</Descriptions.Item>
        </Descriptions>
      </Card>
      <Card title="商品明细" style={{ marginTop: 16 }}>
        <Table columns={itemColumns} dataSource={order.items || []} rowKey="id" pagination={false} size="small" />
      </Card>
      <Card title="订单日志" style={{ marginTop: 16 }}>
        <Timeline items={timelineItems} />
      </Card>
    </div>
  )
}

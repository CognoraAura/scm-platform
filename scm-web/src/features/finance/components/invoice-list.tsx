'use client'
import { Table, Card, Tag } from 'antd'
import { useInvoiceList } from '../hooks'

const statusColors: Record<string, string> = { pending: 'warning', issued: 'processing', sent: 'blue', received: 'success' }

export default function InvoiceList() {
  const { data: invoices, isLoading } = useInvoiceList()

  const columns = [
    { title: '发票号', dataIndex: 'invoiceNo', key: 'invoiceNo' },
    { title: '结算单号', dataIndex: 'settlementNo', key: 'settlementNo' },
    { title: '金额', dataIndex: 'amount', key: 'amount', width: 120, align: 'right' as const, render: (v: number) => `¥${v?.toLocaleString()}` },
    { title: '税额', dataIndex: 'taxAmount', key: 'taxAmount', width: 100, align: 'right' as const, render: (v: number) => `¥${v?.toLocaleString()}` },
    { title: '状态', dataIndex: 'status', key: 'status', width: 100, render: (s: string, r: { statusLabel: string }) => <Tag color={statusColors[s]}>{r.statusLabel}</Tag> },
    { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 120 },
  ]

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>发票管理</h2>
      <Card>
        <Table columns={columns} dataSource={invoices || []} loading={isLoading} rowKey="id" pagination={false} />
      </Card>
    </div>
  )
}

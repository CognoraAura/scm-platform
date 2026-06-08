'use client'
import { Table, Card, Tag } from 'antd'
import { useSettlementList } from '../hooks'

const statusColors: Record<string, string> = { pending: 'warning', processing: 'processing', completed: 'success', rejected: 'error' }

export default function SettlementList() {
  const { data: settlements, isLoading } = useSettlementList()

  const columns = [
    { title: '结算单号', dataIndex: 'settlementNo', key: 'settlementNo' },
    { title: '供应商', dataIndex: 'supplierName', key: 'supplierName' },
    { title: '金额', dataIndex: 'amount', key: 'amount', width: 120, align: 'right' as const, render: (v: number) => `¥${v?.toLocaleString()}` },
    { title: '账期', dataIndex: 'period', key: 'period', width: 100 },
    { title: '状态', dataIndex: 'status', key: 'status', width: 100, render: (s: string, r: { statusLabel: string }) => <Tag color={statusColors[s]}>{r.statusLabel}</Tag> },
    { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 120 },
  ]

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>结算管理</h2>
      <Card>
        <Table columns={columns} dataSource={settlements || []} loading={isLoading} rowKey="id" pagination={false} />
      </Card>
    </div>
  )
}

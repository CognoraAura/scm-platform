'use client'
import { Table, Card, Tag } from 'antd'
import { useWarehouseList } from '../hooks'

export default function WarehouseList() {
  const { data: warehouses, isLoading } = useWarehouseList()

  const columns = [
    { title: '仓库编码', dataIndex: 'code', key: 'code', width: 120 },
    { title: '仓库名称', dataIndex: 'name', key: 'name' },
    { title: '地址', dataIndex: 'address', key: 'address' },
    { title: '联系人', dataIndex: 'contactPerson', key: 'contactPerson', width: 100 },
    { title: '联系电话', dataIndex: 'contactPhone', key: 'contactPhone', width: 140 },
    { title: '状态', dataIndex: 'status', key: 'status', width: 80, render: (s: string) => <Tag color={s === 'active' ? 'success' : 'error'}>{s === 'active' ? '启用' : '禁用'}</Tag> },
  ]

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>仓库管理</h2>
      <Card>
        <Table columns={columns} dataSource={warehouses || []} loading={isLoading} rowKey="id" pagination={false} />
      </Card>
    </div>
  )
}

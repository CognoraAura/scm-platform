'use client'
import { Descriptions, Card, Tag, Button, Rate } from 'antd'
import { useRouter } from 'next/navigation'
import { useSupplierDetail } from '../hooks'

interface Props { id: string }

export default function SupplierDetail({ id }: Props) {
  const { data: supplier, isLoading } = useSupplierDetail(id)
  const router = useRouter()
  if (!supplier) return null

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>供应商详情</h2>
        <Button onClick={() => router.push('/supplier')}>返回列表</Button>
      </div>
      <Card loading={isLoading}>
        <Descriptions column={2}>
          <Descriptions.Item label="供应商编码">{supplier.code}</Descriptions.Item>
          <Descriptions.Item label="供应商名称">{supplier.name}</Descriptions.Item>
          <Descriptions.Item label="联系人">{supplier.contactPerson}</Descriptions.Item>
          <Descriptions.Item label="联系电话">{supplier.contactPhone}</Descriptions.Item>
          <Descriptions.Item label="邮箱">{supplier.email || '-'}</Descriptions.Item>
          <Descriptions.Item label="地址">{supplier.address || '-'}</Descriptions.Item>
          <Descriptions.Item label="评级"><Rate disabled defaultValue={supplier.rating} /></Descriptions.Item>
          <Descriptions.Item label="状态"><Tag color={supplier.status === 'active' ? 'success' : 'error'}>{supplier.status === 'active' ? '启用' : '禁用'}</Tag></Descriptions.Item>
          <Descriptions.Item label="创建时间">{supplier.createdAt}</Descriptions.Item>
        </Descriptions>
      </Card>
    </div>
  )
}

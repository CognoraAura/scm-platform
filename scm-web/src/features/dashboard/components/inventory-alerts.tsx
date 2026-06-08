'use client'

import { Card, List, Tag, Progress } from 'antd'
import { WarningOutlined } from '@ant-design/icons'
import type { InventoryAlert } from '../types'

interface InventoryAlertsProps {
  alerts: InventoryAlert[]
  loading?: boolean
}

const statusConfig: Record<string, { color: string; label: string }> = {
  critical: { color: '#ff4d4f', label: '紧急' },
  low: { color: '#faad14', label: '偏低' },
  out: { color: '#8c8c8c', label: '缺货' },
}

export default function InventoryAlerts({ alerts, loading }: InventoryAlertsProps) {
  return (
    <Card title="库存预警" extra={<WarningOutlined style={{ color: '#faad14' }} />}>
      <List
        loading={loading}
        dataSource={alerts}
        renderItem={(item) => {
          const config = statusConfig[item.status]
          const percent = Math.round((item.currentStock / item.threshold) * 100)
          return (
            <List.Item>
              <List.Item.Meta
                title={<span>{item.productName} <Tag color={config.color}>{config.label}</Tag></span>}
                description={<span style={{ fontSize: 12, color: '#8c8c8c' }}>{item.skuCode} · 库存 {item.currentStock} / 阈值 {item.threshold}</span>}
              />
              <Progress percent={Math.min(percent, 100)} size="small" status={item.status === 'out' || item.status === 'critical' ? 'exception' : 'active'} style={{ width: 100 }} />
            </List.Item>
          )
        }}
      />
    </Card>
  )
}

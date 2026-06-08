'use client'
import { Card, List, Tag, Progress, Button } from 'antd'
import { WarningOutlined, ReloadOutlined } from '@ant-design/icons'
import { useStockAlerts } from '../hooks'
import type { StockAlert } from '../types'

const statusConfig: Record<string, { color: string; label: string }> = {
  low: { color: 'warning', label: '偏低' },
  critical: { color: 'error', label: '紧急' },
  out: { color: 'default', label: '缺货' },
}

export default function StockAlerts() {
  const { data: alerts, isLoading, refetch } = useStockAlerts()

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}><WarningOutlined style={{ color: '#faad14', marginRight: 8 }} />库存预警</h2>
        <Button icon={<ReloadOutlined />} onClick={() => refetch()}>刷新</Button>
      </div>
      <Card>
        <List
          loading={isLoading}
          dataSource={alerts || []}
          renderItem={(item: StockAlert) => {
            const config = statusConfig[item.status]
            const percent = Math.round((item.currentStock / item.threshold) * 100)
            return (
              <List.Item>
                <List.Item.Meta
                  title={<span>{item.productName} <Tag color={config.color}>{config.label}</Tag></span>}
                  description={<span style={{ fontSize: 12, color: '#8c8c8c' }}>{item.skuCode} · {item.warehouseName} · 库存 {item.currentStock} / 阈值 {item.threshold}</span>}
                />
                <Progress percent={Math.min(percent, 100)} size="small" status={item.status === 'out' ? 'exception' : item.status === 'critical' ? 'exception' : 'active'} style={{ width: 100 }} />
              </List.Item>
            )
          }}
        />
      </Card>
    </div>
  )
}

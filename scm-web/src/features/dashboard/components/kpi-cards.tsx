'use client'

import { Row, Col, Card, Statistic } from 'antd'
import { ArrowUpOutlined, ArrowDownOutlined, DollarOutlined, ShoppingCartOutlined, InboxOutlined, TeamOutlined } from '@ant-design/icons'
import type { KPIData } from '../types'

const iconMap: Record<string, React.ReactNode> = {
  DollarOutlined: <DollarOutlined />,
  ShoppingCartOutlined: <ShoppingCartOutlined />,
  InboxOutlined: <InboxOutlined />,
  TeamOutlined: <TeamOutlined />,
}

interface KPICardsProps {
  kpis: KPIData[]
  loading?: boolean
}

export default function KPICards({ kpis, loading }: KPICardsProps) {
  return (
    <Row gutter={[16, 16]}>
      {kpis.map((kpi, index) => (
        <Col key={index} xs={24} sm={12} lg={6}>
          <Card loading={loading}>
            <Statistic
              title={kpi.title}
              value={kpi.value}
              prefix={iconMap[kpi.icon]}
              suffix={
                kpi.trend && (
                  <span style={{ fontSize: 12, color: kpi.trend.direction === 'up' ? '#52c41a' : kpi.trend.direction === 'down' ? '#ff4d4f' : '#8c8c8c' }}>
                    {kpi.trend.direction === 'up' ? <ArrowUpOutlined /> : kpi.trend.direction === 'down' ? <ArrowDownOutlined /> : null}{' '}
                    {kpi.trend.value}% {kpi.trend.period}
                  </span>
                )
              }
            />
          </Card>
        </Col>
      ))}
    </Row>
  )
}

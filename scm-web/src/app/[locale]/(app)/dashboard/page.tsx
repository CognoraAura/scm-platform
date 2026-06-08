'use client'

import { Row, Col } from 'antd'
import { useDashboardStats } from '@/features/dashboard/hooks'
import KPICards from '@/features/dashboard/components/kpi-cards'
import SalesChart from '@/features/dashboard/components/sales-chart'
import OrderStatusChart from '@/features/dashboard/components/order-status-chart'
import RecentOrders from '@/features/dashboard/components/recent-orders'
import InventoryAlerts from '@/features/dashboard/components/inventory-alerts'

export default function DashboardPage() {
  const { data, isLoading } = useDashboardStats()

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <KPICards kpis={data?.kpis || []} loading={isLoading} />
      <Row gutter={[16, 16]}>
        <Col xs={24} lg={16}>
          <SalesChart data={data?.salesTrend || { dates: [], series: [] }} loading={isLoading} />
        </Col>
        <Col xs={24} lg={8}>
          <OrderStatusChart data={data?.orderStatus || []} loading={isLoading} />
        </Col>
      </Row>
      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <RecentOrders orders={data?.recentOrders || []} loading={isLoading} />
        </Col>
        <Col xs={24} lg={12}>
          <InventoryAlerts alerts={data?.inventoryAlerts || []} loading={isLoading} />
        </Col>
      </Row>
    </div>
  )
}

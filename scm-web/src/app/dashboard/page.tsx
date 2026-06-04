'use client'

import { Typography, Row, Col, Card, Statistic } from 'antd'
import {
  UserOutlined,
  ShoppingCartOutlined,
  InboxOutlined,
  TeamOutlined,
} from '@ant-design/icons'
import MainLayout from '@/components/Layout/MainLayout'
import ProtectedRoute from '@/components/Auth/ProtectedRoute'

const { Title } = Typography

export default function DashboardPage() {
  return (
    <ProtectedRoute>
      <MainLayout>
        <Title level={2}>仪表盘</Title>
        <Row gutter={[16, 16]}>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic title="用户数" value={1128} prefix={<UserOutlined />} />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="订单数"
                value={9280}
                prefix={<ShoppingCartOutlined />}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="库存量"
                value={25600}
                prefix={<InboxOutlined />}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="租户数"
                value={28}
                prefix={<TeamOutlined />}
              />
            </Card>
          </Col>
        </Row>
      </MainLayout>
    </ProtectedRoute>
  )
}

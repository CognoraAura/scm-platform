'use client'

import { Skeleton, Card, Row, Col } from 'antd'

export function TableSkeleton() {
  return (
    <div>
      <Card style={{ marginBottom: 16 }}>
        <Row gutter={16}>
          {[1, 2, 3, 4].map((i) => (
            <Col key={i} span={6}>
              <Skeleton.Input active block style={{ height: 32 }} />
            </Col>
          ))}
        </Row>
      </Card>
      <Card>
        {[1, 2, 3, 4, 5].map((i) => (
          <Skeleton key={i} active paragraph={{ rows: 1 }} />
        ))}
      </Card>
    </div>
  )
}

export function DashboardSkeleton() {
  return (
    <div>
      <Row gutter={[16, 16]}>
        {[1, 2, 3, 4].map((i) => (
          <Col key={i} xs={24} sm={12} lg={6}>
            <Card>
              <Skeleton active paragraph={{ rows: 2 }} />
            </Card>
          </Col>
        ))}
      </Row>
      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={16}>
          <Card>
            <Skeleton active paragraph={{ rows: 8 }} />
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card>
            <Skeleton active paragraph={{ rows: 8 }} />
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export function DetailSkeleton() {
  return (
    <Card>
      <Skeleton active paragraph={{ rows: 10 }} />
    </Card>
  )
}

'use client'
import { useState } from 'react'
import { List, Card, Tag, Button, Space, Select, Badge } from 'antd'
import { CheckOutlined } from '@ant-design/icons'
import { useRouter } from 'next/navigation'
import { useNotificationList, useMarkAsRead, useMarkAllAsRead, useUnreadCount } from '../hooks'
import { notificationService } from '../services/notification.service'
import type { Notification } from '../types'

export default function NotificationList() {
  const [type, setType] = useState<string>()
  const [current, setCurrent] = useState(1)
  const router = useRouter()
  const { data, isLoading } = useNotificationList({ current, type })
  const { data: unreadCount } = useUnreadCount()
  const markAsRead = useMarkAsRead()
  const markAllAsRead = useMarkAllAsRead()
  const typeMap = notificationService.getTypeMap()

  const handleItemClick = (item: Notification) => {
    if (!item.isRead) markAsRead.mutate(item.id)
    if (item.link) router.push(item.link)
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Space>
          <h2 style={{ margin: 0 }}>通知管理</h2>
          <Badge count={unreadCount || 0} />
        </Space>
        <Space>
          <Select placeholder="通知类型" style={{ width: 120 }} allowClear onChange={setType}
            options={Object.entries(typeMap).map(([key, val]) => ({ label: val.label, value: key }))} />
          <Button icon={<CheckOutlined />} onClick={() => markAllAsRead.mutate()}>全部已读</Button>
        </Space>
      </div>
      <Card>
        <List
          loading={isLoading}
          dataSource={data?.records || []}
          pagination={{ current, pageSize: 20, total: data?.total, onChange: setCurrent }}
          renderItem={(item: Notification) => {
            const cfg = typeMap[item.type]
            return (
              <List.Item
                style={{ background: item.isRead ? 'transparent' : '#f6ffed', cursor: 'pointer', padding: '12px 16px' }}
                onClick={() => handleItemClick(item)}
                actions={item.isRead ? [] : [<Button key="read" type="link" size="small" onClick={(e) => { e.stopPropagation(); markAsRead.mutate(item.id) }}>标为已读</Button>]}
              >
                <List.Item.Meta
                  title={<Space><Tag color={cfg?.color}>{cfg?.label}</Tag><span style={{ fontWeight: item.isRead ? 'normal' : 'bold' }}>{item.title}</span></Space>}
                  description={<div><div>{item.content}</div><div style={{ fontSize: 12, color: '#8c8c8c', marginTop: 4 }}>{item.createdAt}</div></div>}
                />
              </List.Item>
            )
          }}
        />
      </Card>
    </div>
  )
}

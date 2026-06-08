'use client'

import { useState } from 'react'
import { Button, Tag, Space, Popconfirm, Table, Card } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import { useUserList, useDeleteUser } from '../../hooks'
import UserForm from './user-form'
import type { User } from '../../types'

export default function UserList() {
  const [current, setCurrent] = useState(1)
  const [keyword, setKeyword] = useState('')
  const [formOpen, setFormOpen] = useState(false)
  const [editingUser, setEditingUser] = useState<User | null>(null)
  const { data, isLoading } = useUserList({ current, keyword })
  const deleteUser = useDeleteUser()

  const columns = [
    { title: '用户名', dataIndex: 'username', key: 'username' },
    { title: '姓名', dataIndex: 'displayName', key: 'displayName' },
    { title: '邮箱', dataIndex: 'email', key: 'email' },
    { title: '部门', dataIndex: 'deptName', key: 'deptName' },
    { title: '状态', dataIndex: 'status', key: 'status', render: (status: string) => <Tag color={status === 'active' ? 'success' : 'error'}>{status === 'active' ? '启用' : '禁用'}</Tag> },
    { title: '角色', dataIndex: 'roles', key: 'roles', render: (roles: string[]) => roles?.map(r => <Tag key={r}>{r}</Tag>) },
    {
      title: '操作', key: 'action', render: (_: unknown, record: User) => (
        <Space>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => { setEditingUser(record); setFormOpen(true) }}>编辑</Button>
          <Popconfirm title="确定删除?" onConfirm={() => deleteUser.mutate(record.id)}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>用户管理</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => { setEditingUser(null); setFormOpen(true) }}>新建用户</Button>
      </div>
      <Card>
        <Table columns={columns} dataSource={data?.records || []} loading={isLoading} rowKey="id"
          pagination={{ current, pageSize: 20, total: data?.total, onChange: setCurrent }} />
      </Card>
      <UserForm open={formOpen} onClose={() => setFormOpen(false)} user={editingUser} />
    </div>
  )
}

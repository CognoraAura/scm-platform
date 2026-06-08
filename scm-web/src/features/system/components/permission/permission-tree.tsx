'use client'

import { useState } from 'react'
import { Tree, Button, Card, Tag, Space, Modal, Form, Input, Select } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { usePermissionList, useCreatePermission } from '../../hooks'
import type { Permission } from '../../types'

function buildTreeData(permissions: Permission[]): any[] {
  return permissions.map(p => ({
    key: p.id,
    title: (
      <Space>
        <span>{p.name}</span>
        <Tag color={p.type === 'menu' ? 'blue' : p.type === 'button' ? 'green' : 'orange'}>{p.type}</Tag>
        <span style={{ color: '#999', fontSize: 12 }}>{p.code}</span>
      </Space>
    ),
    children: p.children ? buildTreeData(p.children) : undefined,
  }))
}

export default function PermissionTree() {
  const { data: permissions, isLoading } = usePermissionList()
  const createPermission = useCreatePermission()
  const [modalOpen, setModalOpen] = useState(false)
  const [form] = Form.useForm()

  const handleCreate = async () => {
    try {
      const values = await form.validateFields()
      await createPermission.mutateAsync(values)
      setModalOpen(false)
      form.resetFields()
    } catch {}
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>权限管理</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>新建权限</Button>
      </div>
      <Card loading={isLoading}><Tree treeData={buildTreeData(permissions || [])} defaultExpandAll showLine /></Card>
      <Modal title="新建权限" open={modalOpen} onOk={handleCreate} onCancel={() => setModalOpen(false)}>
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="权限名称" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="code" label="权限编码" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="type" label="类型" rules={[{ required: true }]}><Select options={[{ label: '菜单', value: 'menu' }, { label: '按钮', value: 'button' }, { label: '数据', value: 'data' }]} /></Form.Item>
          <Form.Item name="path" label="路由路径"><Input /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

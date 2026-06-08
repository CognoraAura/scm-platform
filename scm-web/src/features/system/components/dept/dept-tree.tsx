'use client'

import { useState } from 'react'
import { Tree, Button, Card, Space, Modal, Form, Input } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { useDeptList, useCreateDept } from '../../hooks'
import type { Department } from '../../types'

function buildTreeData(depts: Department[]): any[] {
  return depts.map(d => ({
    key: d.id,
    title: <Space><span>{d.name}</span>{d.leaderName && <span style={{ color: '#999', fontSize: 12 }}>负责人: {d.leaderName}</span>}</Space>,
    children: d.children ? buildTreeData(d.children) : undefined,
  }))
}

export default function DeptTree() {
  const { data: depts, isLoading } = useDeptList()
  const createDept = useCreateDept()
  const [modalOpen, setModalOpen] = useState(false)
  const [form] = Form.useForm()

  const handleCreate = async () => {
    try {
      const values = await form.validateFields()
      await createDept.mutateAsync(values)
      setModalOpen(false)
      form.resetFields()
    } catch {}
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>部门管理</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>新建部门</Button>
      </div>
      <Card loading={isLoading}><Tree treeData={buildTreeData(depts || [])} defaultExpandAll showLine /></Card>
      <Modal title="新建部门" open={modalOpen} onOk={handleCreate} onCancel={() => setModalOpen(false)}>
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="部门名称" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="code" label="部门编码" rules={[{ required: true }]}><Input /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

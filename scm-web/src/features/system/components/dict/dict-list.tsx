'use client'

import { useState } from 'react'
import { Button, Tag, Space, Popconfirm, Table, Card, Modal, Form, Input } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import { useDictTypeList, useCreateDictType, useDeleteDictType } from '../../hooks'
import type { DictType } from '../../types'

export default function DictList() {
  const [modalOpen, setModalOpen] = useState(false)
  const [form] = Form.useForm()
  const { data: dictTypes, isLoading } = useDictTypeList()
  const createDictType = useCreateDictType()
  const deleteDictType = useDeleteDictType()

  const columns = [
    { title: '字典名称', dataIndex: 'name', key: 'name' },
    { title: '字典编码', dataIndex: 'code', key: 'code' },
    { title: '描述', dataIndex: 'description', key: 'description' },
    { title: '状态', dataIndex: 'status', key: 'status', render: (status: string) => <Tag color={status === 'active' ? 'success' : 'error'}>{status === 'active' ? '启用' : '禁用'}</Tag> },
    { title: '字典项', dataIndex: 'itemCount', key: 'itemCount' },
    {
      title: '操作', key: 'action', render: (_: unknown, record: DictType) => (
        <Space>
          <Button type="link" size="small" icon={<EditOutlined />}>编辑</Button>
          <Popconfirm title="确定删除?" onConfirm={() => deleteDictType.mutate(record.id)}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  const handleCreate = async () => {
    try {
      const values = await form.validateFields()
      await createDictType.mutateAsync(values)
      setModalOpen(false)
      form.resetFields()
    } catch {}
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>字典管理</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>新建字典</Button>
      </div>
      <Card><Table columns={columns} dataSource={dictTypes || []} loading={isLoading} rowKey="id" pagination={false} /></Card>
      <Modal title="新建字典" open={modalOpen} onOk={handleCreate} onCancel={() => setModalOpen(false)}>
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="字典名称" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="code" label="字典编码" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="description" label="描述"><Input.TextArea /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

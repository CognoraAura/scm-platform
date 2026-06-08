'use client'

import { useEffect } from 'react'
import { Form, Input, Select, Modal, Tree } from 'antd'
import { useCreateRole, useUpdateRole, usePermissionList } from '../../hooks'
import type { Role, Permission } from '../../types'

interface RoleFormProps {
  open: boolean
  onClose: () => void
  role?: Role | null
}

function buildTreeData(permissions: Permission[]): any[] {
  return permissions.map(p => ({
    key: p.code,
    title: p.name,
    children: p.children ? buildTreeData(p.children) : undefined,
  }))
}

export default function RoleForm({ open, onClose, role }: RoleFormProps) {
  const [form] = Form.useForm()
  const createRole = useCreateRole()
  const updateRole = useUpdateRole()
  const { data: permissions } = usePermissionList()

  useEffect(() => {
    if (open) {
      if (role) { form.setFieldsValue(role) } else { form.resetFields() }
    }
  }, [open, role, form])

  const handleOk = async () => {
    try {
      const values = await form.validateFields()
      if (role) { await updateRole.mutateAsync({ id: role.id, data: values }) }
      else { await createRole.mutateAsync(values) }
      onClose()
    } catch {}
  }

  return (
    <Modal title={role ? '编辑角色' : '新建角色'} open={open} onOk={handleOk} onCancel={onClose} width={600}>
      <Form form={form} layout="vertical">
        <Form.Item name="name" label="角色名称" rules={[{ required: true }]}><Input /></Form.Item>
        <Form.Item name="code" label="角色编码" rules={[{ required: true }]}><Input disabled={!!role} /></Form.Item>
        <Form.Item name="description" label="描述"><Input.TextArea /></Form.Item>
        <Form.Item name="status" label="状态" rules={[{ required: true }]}><Select options={[{ label: '启用', value: 'active' }, { label: '禁用', value: 'disabled' }]} /></Form.Item>
        <Form.Item name="permissions" label="权限"><Tree checkable treeData={buildTreeData(permissions || [])} defaultExpandAll /></Form.Item>
      </Form>
    </Modal>
  )
}

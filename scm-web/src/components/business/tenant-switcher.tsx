'use client'

import { Select, Space } from 'antd'
import { TeamOutlined } from '@ant-design/icons'
import { useTenantStore } from '@/stores/tenant-store'

export default function TenantSwitcher() {
  const { currentTenant, tenantList, setTenant } = useTenantStore()

  if (tenantList.length <= 1) {
    return null
  }

  return (
    <Space>
      <TeamOutlined />
      <Select
        value={currentTenant?.id}
        onChange={(tenantId) => {
          const tenant = tenantList.find((t) => t.id === tenantId)
          if (tenant) {
            setTenant(tenant)
          }
        }}
        style={{ width: 160 }}
        bordered={false}
        options={tenantList.map((t) => ({
          label: t.name,
          value: t.id,
        }))}
      />
    </Space>
  )
}

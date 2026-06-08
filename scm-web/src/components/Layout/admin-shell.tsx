'use client'

import { Layout } from 'antd'
import { useUIStore } from '@/stores/ui-store'
import AppHeader from './header'
import AppSidebar from './sidebar'

const { Content } = Layout

interface AdminShellProps {
  children: React.ReactNode
}

export default function AdminShell({ children }: AdminShellProps) {
  const { sidebarCollapsed } = useUIStore()

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <AppSidebar />
      <Layout
        style={{
          marginLeft: sidebarCollapsed ? 80 : 240,
          transition: 'margin-left 200ms ease',
        }}
      >
        <AppHeader />
        <Content
          style={{
            padding: 24,
            minHeight: 'calc(100vh - 64px)',
            background: 'var(--color-bg-layout, #f5f5f5)',
          }}
        >
          {children}
        </Content>
      </Layout>
    </Layout>
  )
}

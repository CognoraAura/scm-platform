'use client'

import { useRouter } from 'next/navigation'
import { Layout, Avatar, Dropdown, Space, Badge, Tooltip, Switch } from 'antd'
import {
  UserOutlined,
  LogoutOutlined,
  SettingOutlined,
  BellOutlined,
  ExpandOutlined,
  SunOutlined,
  MoonOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
} from '@ant-design/icons'
import { useAuthStore } from '@/stores/useAuthStore'
import { useUIStore } from '@/stores/ui-store'

const { Header: AntHeader } = Layout

export default function AppHeader() {
  const router = useRouter()
  const { user, logout } = useAuthStore()
  const { sidebarCollapsed, toggleSidebar, themeMode, setThemeMode } =
    useUIStore()

  const handleLogout = () => {
    logout()
    router.push('/login')
  }

  const toggleFullscreen = () => {
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen()
    } else {
      document.exitFullscreen()
    }
  }

  const userMenuItems = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: '个人中心',
      onClick: () => router.push('/settings/profile'),
    },
    {
      key: 'settings',
      icon: <SettingOutlined />,
      label: '系统设置',
      onClick: () => router.push('/settings/preferences'),
    },
    { type: 'divider' as const },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: handleLogout,
      danger: true,
    },
  ]

  return (
    <AntHeader
      style={{
        padding: '0 24px',
        background: 'var(--color-bg-container, #fff)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        borderBottom: '1px solid var(--color-border-secondary, #f0f0f0)',
        height: 64,
        position: 'sticky',
        top: 0,
        zIndex: 100,
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
        <div
          onClick={toggleSidebar}
          style={{ cursor: 'pointer', fontSize: 18, padding: '0 8px' }}
        >
          {sidebarCollapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
        <Tooltip title="全屏">
          <ExpandOutlined
            onClick={toggleFullscreen}
            style={{ cursor: 'pointer', fontSize: 16 }}
          />
        </Tooltip>

        <Tooltip title={themeMode === 'dark' ? '浅色模式' : '深色模式'}>
          <Switch
            checkedChildren={<MoonOutlined />}
            unCheckedChildren={<SunOutlined />}
            checked={themeMode === 'dark'}
            onChange={(checked) => setThemeMode(checked ? 'dark' : 'light')}
            size="small"
          />
        </Tooltip>

        <Badge count={0} size="small">
          <BellOutlined style={{ cursor: 'pointer', fontSize: 16 }} />
        </Badge>

        <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
          <Space style={{ cursor: 'pointer' }}>
            <Avatar icon={<UserOutlined />} size="small" />
            <span>{user?.displayName || user?.username || 'User'}</span>
          </Space>
        </Dropdown>
      </div>
    </AntHeader>
  )
}

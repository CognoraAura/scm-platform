'use client'

import { useRouter, usePathname } from 'next/navigation'
import { Layout, Menu } from 'antd'
import type { MenuProps } from 'antd'
import {
  DashboardOutlined,
  ShoppingCartOutlined,
  InboxOutlined,
  ShopOutlined,
  CarOutlined,
  TeamOutlined,
  SettingOutlined,
  UserOutlined,
  SafetyOutlined,
  BookOutlined,
  FileTextOutlined,
  NotificationOutlined,
  ApartmentOutlined,
} from '@ant-design/icons'
import { useUIStore } from '@/stores/ui-store'

const { Sider } = Layout

type MenuItem = Required<MenuProps>['items'][number]

function getItem(
  label: React.ReactNode,
  key: string,
  icon?: React.ReactNode,
  children?: MenuItem[]
): MenuItem {
  return { key, icon, children, label } as MenuItem
}

const menuItems: MenuItem[] = [
  getItem('仪表盘', '/dashboard', <DashboardOutlined />),
  getItem('商品管理', '/product', <ShopOutlined />, [
    getItem('商品列表', '/product'),
    getItem('商品分类', '/product/category'),
    getItem('品牌管理', '/product/brand'),
  ]),
  getItem('订单管理', '/order', <ShoppingCartOutlined />, [
    getItem('订单列表', '/order'),
    getItem('退款管理', '/order/refund'),
  ]),
  getItem('库存管理', '/inventory', <InboxOutlined />, [
    getItem('库存列表', '/inventory'),
    getItem('库存预警', '/inventory/alerts'),
  ]),
  getItem('仓库管理', '/warehouse', <ApartmentOutlined />, [
    getItem('仓库列表', '/warehouse'),
    getItem('入库管理', '/warehouse/inbound'),
    getItem('出库管理', '/warehouse/outbound'),
    getItem('拣货波次', '/warehouse/wave-picking'),
  ]),
  getItem('采购管理', '/purchase', <FileTextOutlined />, [
    getItem('采购订单', '/purchase'),
    getItem('询价管理', '/purchase/rfq'),
    getItem('报价管理', '/purchase/quotation'),
  ]),
  getItem('供应商管理', '/supplier', <TeamOutlined />),
  getItem('物流管理', '/logistics', <CarOutlined />, [
    getItem('运单管理', '/logistics'),
    getItem('物流跟踪', '/logistics/tracking'),
    getItem('承运商管理', '/logistics/carrier'),
  ]),
  getItem('财务管理', '/finance', <BookOutlined />, [
    getItem('结算管理', '/finance/settlement'),
    getItem('发票管理', '/finance/invoice'),
    getItem('对账管理', '/finance/reconciliation'),
  ]),
  getItem('租户管理', '/tenant', <TeamOutlined />),
  getItem('系统管理', '/system', <SettingOutlined />, [
    getItem('用户管理', '/system/user', <UserOutlined />),
    getItem('角色管理', '/system/role', <SafetyOutlined />),
    getItem('权限管理', '/system/permission'),
    getItem('部门管理', '/system/dept'),
    getItem('字典管理', '/system/dictionary'),
  ]),
  getItem('通知管理', '/notification', <NotificationOutlined />),
]

export default function AppSidebar() {
  const router = useRouter()
  const pathname = usePathname()
  const { sidebarCollapsed } = useUIStore()

  const getSelectedKey = () => {
    const path = pathname.replace(/^\/[a-z]{2}-[A-Z]{2}/, '') || pathname
    return path
  }

  const getOpenKeys = () => {
    const path = pathname.replace(/^\/[a-z]{2}-[A-Z]{2}/, '') || pathname
    const parts = path.split('/').filter(Boolean)
    if (parts.length > 1) {
      return [`/${parts[0]}`]
    }
    return []
  }

  return (
    <Sider
      trigger={null}
      collapsible
      collapsed={sidebarCollapsed}
      width={240}
      collapsedWidth={80}
      style={{
        overflow: 'auto',
        height: '100vh',
        position: 'fixed',
        left: 0,
        top: 0,
        bottom: 0,
        borderRight: '1px solid var(--color-border-secondary, #f0f0f0)',
        transition: 'width 200ms ease',
      }}
    >
      <div
        style={{
          height: 64,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          borderBottom: '1px solid var(--color-border-secondary, #f0f0f0)',
        }}
      >
        <span
          style={{
            color: 'var(--color-primary, #1677ff)',
            fontSize: sidebarCollapsed ? 18 : 20,
            fontWeight: 'bold',
          }}
        >
          {sidebarCollapsed ? 'SCM' : 'SCM Platform'}
        </span>
      </div>
      <Menu
        mode="inline"
        selectedKeys={[getSelectedKey()]}
        defaultOpenKeys={getOpenKeys()}
        items={menuItems}
        onClick={({ key }) => router.push(key)}
        style={{ borderRight: 0 }}
      />
    </Sider>
  )
}

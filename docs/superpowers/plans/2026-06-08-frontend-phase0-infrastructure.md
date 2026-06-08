# Frontend Phase 0: Infrastructure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restructure the existing `scm-web/` project to match the frontend architecture document's folder structure, design tokens, admin shell layout, state management, and core infrastructure.

**Architecture:** Phase 0 establishes the foundation: design tokens, admin shell (ProLayout with header/sidebar), Zustand stores (UI, tenant, preferences), TanStack Query provider, Axios interceptors with proper error handling, theme provider with dark mode support, auth middleware with route guards, error boundaries, and loading skeletons. All business modules build on this.

**Tech Stack:** Next.js 15 (App Router), React 19, Ant Design 5, Pro Components, Zustand 5, TanStack Query 5, Axios, next-intl, Zod

**Reference:** `docs/design/frontend-architecture.md` (sections 1-43)

---

## Current State Analysis

**Existing files in `scm-web/`:**
- `src/app/layout.tsx` — Empty root layout (just returns children)
- `src/app/login/page.tsx` — Basic login page with TOTP support
- `src/app/dashboard/page.tsx` — Basic dashboard with KPI cards
- `src/app/users/page.tsx`, `src/app/users/[id]/page.tsx` — User pages
- `src/app/roles/page.tsx` — Roles page
- `src/components/Layout/MainLayout.tsx` — Basic sidebar layout (hardcoded menu, no ProLayout)
- `src/components/Auth/ProtectedRoute.tsx` — Client-side auth check
- `src/components/providers/QueryProvider.tsx` — TanStack Query provider
- `src/lib/api/client.ts` — Axios instance with basic interceptors
- `src/lib/api/endpoints.ts` — API endpoint functions
- `src/stores/useAuthStore.ts` — Auth store with persist
- `src/i18n/routing.ts`, `src/i18n/request.ts` — i18n config
- `src/middleware.ts` — i18n middleware only (no auth)

**Key Gaps:**
1. No feature-based folder structure
2. No design tokens
3. Layout is basic (not ProLayout, hardcoded menu, no header features)
4. No UIStore, TenantStore, PreferenceStore
5. Axios interceptor uses localStorage (should use memory + Zustand)
6. No auth middleware (only i18n)
7. No error boundaries
8. No loading skeletons
9. No theme provider (light/dark)
10. Dashboard uses ProtectedRoute wrapper (should be in layout)

---

## File Structure

### Files to Create

| File | Responsibility |
|------|---------------|
| `src/lib/design-tokens.ts` | TypeScript design token definitions |
| `src/lib/design-tokens.css` | CSS custom properties |
| `src/lib/antd-theme.ts` | Ant Design theme config (light/dark) |
| `src/stores/ui-store.ts` | Sidebar, theme, density state |
| `src/stores/tenant-store.ts` | Current tenant, tenant list |
| `src/stores/preference-store.ts` | User preferences (locale, page size, etc.) |
| `src/stores/index.ts` | Barrel export |
| `src/providers/theme-provider.tsx` | Ant Design ConfigProvider with theme |
| `src/providers/auth-provider.tsx` | Auth context provider |
| `src/providers/tenant-provider.tsx` | Tenant context provider |
| `src/providers/app-provider.tsx` | Composed providers |
| `src/layouts/admin-layout.tsx` | Admin shell with ProLayout |
| `src/layouts/auth-layout.tsx` | Login/register layout |
| `src/components/layout/header.tsx` | Header with search, notifications, avatar |
| `src/components/layout/sidebar.tsx` | Sidebar with multi-level menu |
| `src/components/layout/breadcrumb.tsx` | Auto-generated breadcrumbs |
| `src/components/layout/admin-shell.tsx` | Admin shell combining header + sidebar + content |
| `src/components/ui/loading-skeleton.tsx` | Skeleton loading component |
| `src/components/ui/error-boundary.tsx` | Error boundary with retry |
| `src/components/ui/offline-banner.tsx` | Network offline banner |
| `src/app/[locale]/(auth)/layout.tsx` | Auth layout (no sidebar) |
| `src/app/[locale]/(app)/layout.tsx` | App layout (admin shell) |
| `src/app/[locale]/(app)/loading.tsx` | App loading skeleton |
| `src/app/[locale]/(app)/error.tsx` | App error boundary |
| `src/app/[locale]/(app)/not-found.tsx` | 404 page |
| `src/constants/api-paths.ts` | API endpoint paths |
| `src/constants/route-paths.ts` | Frontend route paths |
| `src/constants/permissions.ts` | Permission codes |
| `src/constants/storage-keys.ts` | Local storage keys |
| `src/constants/query-keys.ts` | TanStack Query keys |
| `src/constants/index.ts` | Barrel export |

### Files to Modify

| File | Changes |
|------|---------|
| `src/app/layout.tsx` | Add html/body, providers, global styles |
| `src/app/[locale]/layout.tsx` | Add i18n provider, theme provider |
| `src/lib/api/client.ts` | Use Zustand for tokens, add tenant header, improve error handling |
| `src/middleware.ts` | Add auth guard, tenant header |
| `src/stores/useAuthStore.ts` | Remove persist (memory only), add permissions |
| `src/components/providers/QueryProvider.tsx` | Update config per architecture |
| `src/app/login/page.tsx` | Use auth layout, remove ProtectedRoute wrapper |
| `src/app/dashboard/page.tsx` | Remove MainLayout/ProtectedRoute wrappers |

### Files to Delete

| File | Reason |
|------|--------|
| `src/components/Layout/MainLayout.tsx` | Replaced by admin-shell |
| `src/components/Auth/ProtectedRoute.tsx` | Auth handled by middleware + layout |

---

## Tasks

### Task 1: Create Design Tokens

**Files:**
- Create: `scm-web/src/lib/design-tokens.ts`
- Create: `scm-web/src/lib/design-tokens.css`
- Create: `scm-web/src/lib/antd-theme.ts`

- [ ] **Step 1: Create design-tokens.ts**

```typescript
// scm-web/src/lib/design-tokens.ts
export const tokens = {
  // Layout
  headerHeight: 64,
  sidebarWidth: 240,
  sidebarCollapsedWidth: 80,
  contentPadding: 24,

  // Spacing (8px grid)
  spacing: {
    xs: 4,
    sm: 8,
    md: 16,
    lg: 24,
    xl: 32,
    xxl: 48,
    xxxl: 64,
  },

  // Typography
  fontSize: {
    xs: 12,
    sm: 13,
    base: 14,
    lg: 16,
    xl: 20,
    xxl: 24,
    xxxl: 32,
  },

  // Border radius
  radius: {
    xs: 4,
    sm: 6,
    base: 8,
    lg: 12,
    xl: 16,
  },

  // Colors (light theme)
  colors: {
    primary: '#1677ff',
    success: '#52c41a',
    warning: '#faad14',
    error: '#ff4d4f',
    info: '#1677ff',
  },

  // Shadows
  shadow: {
    xs: '0 1px 2px 0 rgba(0,0,0,0.03), 0 1px 6px -1px rgba(0,0,0,0.02)',
    sm: '0 1px 2px 0 rgba(0,0,0,0.03), 0 2px 4px -1px rgba(0,0,0,0.02)',
    base: '0 6px 16px 0 rgba(0,0,0,0.08), 0 3px 6px -4px rgba(0,0,0,0.12)',
    lg: '0 12px 40px 0 rgba(0,0,0,0.12), 0 8px 20px -6px rgba(0,0,0,0.16)',
  },

  // Breakpoints
  breakpoint: {
    xs: 480,
    sm: 576,
    md: 768,
    lg: 992,
    xl: 1200,
    xxl: 1600,
  },

  // Animation
  duration: {
    fast: 100,
    normal: 200,
    slow: 300,
  },

  // Z-index layers
  zIndex: {
    dropdown: 1050,
    sticky: 1100,
    fixed: 1200,
    modalBackdrop: 1300,
    modal: 1400,
    popover: 1500,
    tooltip: 1600,
  },
} as const

export type Tokens = typeof tokens
```

- [ ] **Step 2: Create design-tokens.css**

```css
/* scm-web/src/lib/design-tokens.css */
:root {
  /* Layout */
  --header-height: 64px;
  --sidebar-width: 240px;
  --sidebar-collapsed-width: 80px;
  --content-padding: 24px;

  /* Spacing */
  --spacing-xs: 4px;
  --spacing-sm: 8px;
  --spacing-md: 16px;
  --spacing-lg: 24px;
  --spacing-xl: 32px;
  --spacing-xxl: 48px;

  /* Colors */
  --color-primary: #1677ff;
  --color-success: #52c41a;
  --color-warning: #faad14;
  --color-error: #ff4d4f;

  /* Typography */
  --font-size-xs: 12px;
  --font-size-sm: 13px;
  --font-size-base: 14px;
  --font-size-lg: 16px;

  /* Radius */
  --radius-xs: 4px;
  --radius-sm: 6px;
  --radius-base: 8px;
  --radius-lg: 12px;
  --radius-xl: 16px;

  /* Duration */
  --duration-fast: 100ms;
  --duration-normal: 200ms;
  --duration-slow: 300ms;
}

[data-theme='dark'] {
  --color-primary: #1668dc;
  --color-success: #49aa19;
  --color-warning: #d89614;
  --color-error: #dc4446;
}
```

- [ ] **Step 3: Create antd-theme.ts**

```typescript
// scm-web/src/lib/antd-theme.ts
import { theme } from 'antd'
import type { ThemeConfig } from 'antd'
import { tokens } from './design-tokens'

export const lightTheme: ThemeConfig = {
  token: {
    colorPrimary: tokens.colors.primary,
    colorSuccess: tokens.colors.success,
    colorWarning: tokens.colors.warning,
    colorError: tokens.colors.error,
    borderRadius: tokens.radius.base,
    fontSize: tokens.fontSize.base,
  },
  algorithm: theme.defaultAlgorithm,
}

export const darkTheme: ThemeConfig = {
  token: {
    colorPrimary: '#1668dc',
    colorSuccess: '#49aa19',
    colorWarning: '#d89614',
    colorError: '#dc4446',
    borderRadius: tokens.radius.base,
    fontSize: tokens.fontSize.base,
    colorBgContainer: '#141414',
    colorBgLayout: '#000000',
    colorBgElevated: '#1f1f1f',
    colorText: 'rgba(255,255,255,0.88)',
    colorTextSecondary: 'rgba(255,255,255,0.65)',
    colorBorder: '#424242',
    colorBorderSecondary: '#303030',
  },
  algorithm: theme.darkAlgorithm,
}
```

- [ ] **Step 4: Verify files compile**

Run: `cd scm-web && npx tsc --noEmit src/lib/design-tokens.ts src/lib/antd-theme.ts`
Expected: No errors

- [ ] **Step 5: Commit**

```bash
git add scm-web/src/lib/design-tokens.ts scm-web/src/lib/design-tokens.css scm-web/src/lib/antd-theme.ts
git commit -m "feat(frontend): add design tokens and theme configuration"
```

---

### Task 2: Create Zustand Stores

**Files:**
- Create: `scm-web/src/stores/ui-store.ts`
- Create: `scm-web/src/stores/tenant-store.ts`
- Create: `scm-web/src/stores/preference-store.ts`
- Create: `scm-web/src/stores/index.ts`
- Modify: `scm-web/src/stores/useAuthStore.ts`

- [ ] **Step 1: Create ui-store.ts**

```typescript
// scm-web/src/stores/ui-store.ts
import { create } from 'zustand'
import { persist } from 'zustand/middleware'

type ThemeMode = 'light' | 'dark' | 'system'
type Density = 'default' | 'compact' | 'loose'

interface UIState {
  sidebarCollapsed: boolean
  themeMode: ThemeMode
  density: Density
  toggleSidebar: () => void
  setSidebarCollapsed: (collapsed: boolean) => void
  setThemeMode: (mode: ThemeMode) => void
  setDensity: (density: Density) => void
}

export const useUIStore = create<UIState>()(
  persist(
    (set) => ({
      sidebarCollapsed: false,
      themeMode: 'light',
      density: 'default',
      toggleSidebar: () =>
        set((state) => ({ sidebarCollapsed: !state.sidebarCollapsed })),
      setSidebarCollapsed: (collapsed) => set({ sidebarCollapsed: collapsed }),
      setThemeMode: (mode) => set({ themeMode: mode }),
      setDensity: (density) => set({ density }),
    }),
    { name: 'scm-ui' }
  )
)
```

- [ ] **Step 2: Create tenant-store.ts**

```typescript
// scm-web/src/stores/tenant-store.ts
import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface Tenant {
  id: string
  name: string
  code: string
}

interface TenantState {
  currentTenant: Tenant | null
  tenantList: Tenant[]
  tenantLoading: boolean
  setTenant: (tenant: Tenant) => void
  setTenantList: (list: Tenant[]) => void
  setTenantLoading: (loading: boolean) => void
  clearTenant: () => void
}

export const useTenantStore = create<TenantState>()(
  persist(
    (set) => ({
      currentTenant: null,
      tenantList: [],
      tenantLoading: false,
      setTenant: (tenant) => set({ currentTenant: tenant }),
      setTenantList: (list) => set({ tenantList: list }),
      setTenantLoading: (loading) => set({ tenantLoading: loading }),
      clearTenant: () =>
        set({ currentTenant: null, tenantList: [], tenantLoading: false }),
    }),
    {
      name: 'scm-tenant',
      partialize: (state) => ({ currentTenant: state.currentTenant }),
    }
  )
)
```

- [ ] **Step 3: Create preference-store.ts**

```typescript
// scm-web/src/stores/preference-store.ts
import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface PreferenceState {
  locale: string
  tablePageSize: number
  dateFormat: string
  setLocale: (locale: string) => void
  setTablePageSize: (size: number) => void
  setDateFormat: (format: string) => void
}

export const usePreferenceStore = create<PreferenceState>()(
  persist(
    (set) => ({
      locale: 'zh-CN',
      tablePageSize: 20,
      dateFormat: 'YYYY-MM-DD HH:mm:ss',
      setLocale: (locale) => set({ locale }),
      setTablePageSize: (size) => set({ tablePageSize: size }),
      setDateFormat: (format) => set({ dateFormat: format }),
    }),
    { name: 'scm-preference' }
  )
)
```

- [ ] **Step 4: Create stores barrel export**

```typescript
// scm-web/src/stores/index.ts
export { useAuthStore } from './useAuthStore'
export { useUIStore } from './ui-store'
export { useTenantStore } from './tenant-store'
export { usePreferenceStore } from './preference-store'
```

- [ ] **Step 5: Update useAuthStore.ts — remove persist, add permissions**

```typescript
// scm-web/src/stores/useAuthStore.ts
import { create } from 'zustand'

interface User {
  id: string
  username: string
  displayName: string
  email?: string
  avatar?: string
  roles?: string[]
}

interface AuthState {
  user: User | null
  accessToken: string | null
  refreshToken: string | null
  permissions: string[]
  isAuthenticated: boolean
  login: (user: User, accessToken: string, refreshToken: string) => void
  logout: () => void
  setUser: (user: User) => void
  setPermissions: (permissions: string[]) => void
  hasPermission: (code: string) => boolean
}

export const useAuthStore = create<AuthState>()((set, get) => ({
  user: null,
  accessToken: null,
  refreshToken: null,
  permissions: [],
  isAuthenticated: false,
  login: (user, accessToken, refreshToken) => {
    set({ user, accessToken, refreshToken, isAuthenticated: true })
  },
  logout: () => {
    set({
      user: null,
      accessToken: null,
      refreshToken: null,
      permissions: [],
      isAuthenticated: false,
    })
  },
  setUser: (user) => set({ user }),
  setPermissions: (permissions) => set({ permissions }),
  hasPermission: (code) => {
    const { permissions } = get()
    return permissions.includes(code) || permissions.includes('*')
  },
}))
```

- [ ] **Step 6: Verify stores compile**

Run: `cd scm-web && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 7: Commit**

```bash
git add scm-web/src/stores/
git commit -m "feat(frontend): add UI, tenant, preference stores; update auth store"
```

---

### Task 3: Update Axios Client with Proper Interceptors

**Files:**
- Modify: `scm-web/src/lib/api/client.ts`

- [ ] **Step 1: Rewrite client.ts with Zustand-based tokens and tenant header**

```typescript
// scm-web/src/lib/api/client.ts
import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios'
import { useAuthStore } from '@/stores/useAuthStore'
import { useTenantStore } from '@/stores/tenant-store'

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8761'

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const { accessToken } = useAuthStore.getState()
    const { currentTenant } = useTenantStore.getState()

    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`
    }

    if (currentTenant) {
      config.headers['X-Tenant-Id'] = currentTenant.id
      config.headers['X-Tenant-Code'] = currentTenant.code
    }

    // Request ID for tracing
    config.headers['X-Request-Id'] = crypto.randomUUID()

    return config
  },
  (error) => Promise.reject(error)
)

// Response interceptor
let isRefreshing = false
let pendingRequests: Array<{
  resolve: (token: string) => void
  reject: (error: Error) => void
}> = []

apiClient.interceptors.response.use(
  (response) => response.data,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & {
      _retry?: boolean
    }

    // 401 — attempt token refresh
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true

      if (isRefreshing) {
        // Queue this request
        return new Promise((resolve, reject) => {
          pendingRequests.push({
            resolve: (token: string) => {
              originalRequest.headers.Authorization = `Bearer ${token}`
              resolve(apiClient(originalRequest))
            },
            reject,
          })
        })
      }

      isRefreshing = true

      try {
        const { refreshToken } = useAuthStore.getState()
        if (!refreshToken) {
          throw new Error('No refresh token')
        }

        const response = await axios.post(`${API_BASE_URL}/api/auth/refresh`, {
          refreshToken,
        })

        const {
          accessToken: newToken,
          refreshToken: newRefreshToken,
        } = (response.data as { data: { accessToken: string; refreshToken: string } }).data

        const { login, user } = useAuthStore.getState()
        if (user) {
          login(user, newToken, newRefreshToken)
        }

        // Retry pending requests
        pendingRequests.forEach(({ resolve }) => resolve(newToken))
        pendingRequests = []

        originalRequest.headers.Authorization = `Bearer ${newToken}`
        return apiClient(originalRequest)
      } catch (refreshError) {
        // Reject all pending requests
        pendingRequests.forEach(({ reject }) =>
          reject(new Error('Token refresh failed'))
        )
        pendingRequests = []

        // Clear auth and redirect
        useAuthStore.getState().logout()
        if (typeof window !== 'undefined') {
          window.location.href = '/login'
        }
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    // 403 — permission denied
    if (error.response?.status === 403) {
      // Will be handled by the UI layer
    }

    // 429 — rate limit
    if (error.response?.status === 429) {
      // Will be handled by the UI layer
    }

    return Promise.reject(error)
  }
)

export default apiClient
```

- [ ] **Step 2: Verify compilation**

Run: `cd scm-web && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 3: Commit**

```bash
git add scm-web/src/lib/api/client.ts
git commit -m "feat(frontend): update Axios client with Zustand tokens and tenant header"
```

---

### Task 4: Create Admin Shell Layout

**Files:**
- Create: `scm-web/src/components/layout/admin-shell.tsx`
- Create: `scm-web/src/components/layout/header.tsx`
- Create: `scm-web/src/components/layout/sidebar.tsx`
- Create: `scm-web/src/components/layout/breadcrumb.tsx`
- Create: `scm-web/src/layouts/admin-layout.tsx`
- Create: `scm-web/src/layouts/auth-layout.tsx`

- [ ] **Step 1: Create header.tsx**

```tsx
// scm-web/src/components/layout/header.tsx
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
            onChange={(checked) =>
              setThemeMode(checked ? 'dark' : 'light')
            }
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
```

- [ ] **Step 2: Create sidebar.tsx**

```tsx
// scm-web/src/components/layout/sidebar.tsx
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

  // Find the active key from pathname
  const getSelectedKey = () => {
    // Remove locale prefix if present
    const path = pathname.replace(/^\/[a-z]{2}-[A-Z]{2}/, '') || pathname
    return path
  }

  // Find open keys from pathname
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
```

- [ ] **Step 3: Create admin-shell.tsx**

```tsx
// scm-web/src/components/layout/admin-shell.tsx
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
```

- [ ] **Step 4: Create admin-layout.tsx and auth-layout.tsx**

```tsx
// scm-web/src/layouts/admin-layout.tsx
'use client'

import AdminShell from '@/components/layout/admin-shell'

interface AdminLayoutProps {
  children: React.ReactNode
}

export default function AdminLayout({ children }: AdminLayoutProps) {
  return <AdminShell>{children}</AdminShell>
}
```

```tsx
// scm-web/src/layouts/auth-layout.tsx
'use client'

import { Layout } from 'antd'

const { Content } = Layout

interface AuthLayoutProps {
  children: React.ReactNode
}

export default function AuthLayout({ children }: AuthLayoutProps) {
  return (
    <Layout style={{ minHeight: '100vh', background: '#f0f2f5' }}>
      <Content
        style={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
        }}
      >
        {children}
      </Content>
    </Layout>
  )
}
```

- [ ] **Step 5: Verify compilation**

Run: `cd scm-web && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 6: Commit**

```bash
git add scm-web/src/components/layout/ scm-web/src/layouts/
git commit -m "feat(frontend): add admin shell layout with header and sidebar"
```

---

### Task 5: Create Providers

**Files:**
- Create: `scm-web/src/providers/theme-provider.tsx`
- Create: `scm-web/src/providers/app-provider.tsx`
- Modify: `scm-web/src/components/providers/QueryProvider.tsx`

- [ ] **Step 1: Create theme-provider.tsx**

```tsx
// scm-web/src/providers/theme-provider.tsx
'use client'

import { ReactNode, useEffect, useMemo } from 'react'
import { ConfigProvider, App as AntApp } from 'antd'
import { lightTheme, darkTheme } from '@/lib/antd-theme'
import { useUIStore } from '@/stores/ui-store'

interface ThemeProviderProps {
  children: ReactNode
}

export default function ThemeProvider({ children }: ThemeProviderProps) {
  const { themeMode } = useUIStore()

  const isDark = useMemo(() => {
    if (themeMode === 'system') {
      return window.matchMedia('(prefers-color-scheme: dark)').matches
    }
    return themeMode === 'dark'
  }, [themeMode])

  useEffect(() => {
    document.documentElement.setAttribute(
      'data-theme',
      isDark ? 'dark' : 'light'
    )
  }, [isDark])

  return (
    <ConfigProvider theme={isDark ? darkTheme : lightTheme}>
      <AntApp>{children}</AntApp>
    </ConfigProvider>
  )
}
```

- [ ] **Step 2: Update QueryProvider.tsx**

```tsx
// scm-web/src/components/providers/QueryProvider.tsx
'use client'

import { ReactNode, useState } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools'

export default function QueryProvider({ children }: { children: ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 5 * 60 * 1000, // 5 minutes
            gcTime: 30 * 60 * 1000, // 30 minutes
            retry: 2,
            refetchOnWindowFocus: false,
            refetchOnReconnect: true,
          },
          mutations: {
            retry: 0,
          },
        },
      })
  )

  return (
    <QueryClientProvider client={queryClient}>
      {children}
      {process.env.NODE_ENV === 'development' && (
        <ReactQueryDevtools initialIsOpen={false} />
      )}
    </QueryClientProvider>
  )
}
```

- [ ] **Step 3: Create app-provider.tsx**

```tsx
// scm-web/src/providers/app-provider.tsx
'use client'

import { ReactNode } from 'react'
import QueryProvider from '@/components/providers/QueryProvider'
import ThemeProvider from './theme-provider'

interface AppProviderProps {
  children: ReactNode
}

export default function AppProvider({ children }: AppProviderProps) {
  return (
    <QueryProvider>
      <ThemeProvider>{children}</ThemeProvider>
    </QueryProvider>
  )
}
```

- [ ] **Step 4: Verify compilation**

Run: `cd scm-web && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 5: Commit**

```bash
git add scm-web/src/providers/ scm-web/src/components/providers/QueryProvider.tsx
git commit -m "feat(frontend): add theme provider and update query provider config"
```

---

### Task 6: Create UI Components (Error Boundary, Loading Skeleton, Offline Banner)

**Files:**
- Create: `scm-web/src/components/ui/error-boundary.tsx`
- Create: `scm-web/src/components/ui/loading-skeleton.tsx`
- Create: `scm-web/src/components/ui/offline-banner.tsx`

- [ ] **Step 1: Create error-boundary.tsx**

```tsx
// scm-web/src/components/ui/error-boundary.tsx
'use client'

import { Component, ReactNode } from 'react'
import { Button, Result } from 'antd'

interface ErrorBoundaryProps {
  children: ReactNode
  fallback?: ReactNode
}

interface ErrorBoundaryState {
  hasError: boolean
  error: Error | null
}

export class ErrorBoundary extends Component<
  ErrorBoundaryProps,
  ErrorBoundaryState
> {
  constructor(props: ErrorBoundaryProps) {
    super(props)
    this.state = { hasError: false, error: null }
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error }
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('ErrorBoundary caught:', error, errorInfo)
  }

  handleRetry = () => {
    this.setState({ hasError: false, error: null })
  }

  handleGoHome = () => {
    window.location.href = '/dashboard'
  }

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback
      }

      return (
        <Result
          status="error"
          title="页面加载出错"
          subTitle="发生了一个意外错误，请尝试以下操作"
          extra={[
            <Button key="retry" type="primary" onClick={this.handleRetry}>
              重试
            </Button>,
            <Button key="home" onClick={this.handleGoHome}>
              返回首页
            </Button>,
          ]}
        />
      )
    }

    return this.props.children
  }
}
```

- [ ] **Step 2: Create loading-skeleton.tsx**

```tsx
// scm-web/src/components/ui/loading-skeleton.tsx
'use client'

import { Skeleton, Card, Row, Col } from 'antd'

export function TableSkeleton() {
  return (
    <div>
      <Card style={{ marginBottom: 16 }}>
        <Row gutter={16}>
          {[1, 2, 3, 4].map((i) => (
            <Col key={i} span={6}>
              <Skeleton.Input active block style={{ height: 32 }} />
            </Col>
          ))}
        </Row>
      </Card>
      <Card>
        {[1, 2, 3, 4, 5].map((i) => (
          <Skeleton key={i} active paragraph={{ rows: 1 }} />
        ))}
      </Card>
    </div>
  )
}

export function DashboardSkeleton() {
  return (
    <div>
      <Row gutter={[16, 16]}>
        {[1, 2, 3, 4].map((i) => (
          <Col key={i} xs={24} sm={12} lg={6}>
            <Card>
              <Skeleton active paragraph={{ rows: 2 }} />
            </Card>
          </Col>
        ))}
      </Row>
      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={16}>
          <Card>
            <Skeleton active paragraph={{ rows: 8 }} />
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card>
            <Skeleton active paragraph={{ rows: 8 }} />
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export function DetailSkeleton() {
  return (
    <Card>
      <Skeleton active paragraph={{ rows: 10 }} />
    </Card>
  )
}
```

- [ ] **Step 3: Create offline-banner.tsx**

```tsx
// scm-web/src/components/ui/offline-banner.tsx
'use client'

import { useEffect, useState } from 'react'
import { Alert } from 'antd'

export default function OfflineBanner() {
  const [isOnline, setIsOnline] = useState(true)

  useEffect(() => {
    setIsOnline(navigator.onLine)

    const handleOnline = () => setIsOnline(true)
    const handleOffline = () => setIsOnline(false)

    window.addEventListener('online', handleOnline)
    window.addEventListener('offline', handleOffline)

    return () => {
      window.removeEventListener('online', handleOnline)
      window.removeEventListener('offline', handleOffline)
    }
  }, [])

  if (isOnline) return null

  return (
    <Alert
      message="网络连接已断开，请检查您的网络设置"
      type="warning"
      showIcon
      banner
      closable={false}
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        zIndex: 9999,
      }}
    />
  )
}
```

- [ ] **Step 4: Verify compilation**

Run: `cd scm-web && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 5: Commit**

```bash
git add scm-web/src/components/ui/
git commit -m "feat(frontend): add error boundary, loading skeleton, offline banner"
```

---

### Task 7: Create Route Structure with Layouts

**Files:**
- Modify: `scm-web/src/app/layout.tsx`
- Modify: `scm-web/src/app/[locale]/layout.tsx`
- Create: `scm-web/src/app/[locale]/(auth)/layout.tsx`
- Create: `scm-web/src/app/[locale]/(app)/layout.tsx`
- Create: `scm-web/src/app/[locale]/(app)/loading.tsx`
- Create: `scm-web/src/app/[locale]/(app)/error.tsx`
- Create: `scm-web/src/app/[locale]/(app)/not-found.tsx`
- Move: existing pages to new route structure

- [ ] **Step 1: Update root layout.tsx**

```tsx
// scm-web/src/app/layout.tsx
import { ReactNode } from 'react'
import '@/lib/design-tokens.css'

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html suppressHydrationWarning>
      <body>{children}</body>
    </html>
  )
}
```

- [ ] **Step 2: Update [locale]/layout.tsx**

```tsx
// scm-web/src/app/[locale]/layout.tsx
import { ReactNode } from 'react'
import { NextIntlClientProvider } from 'next-intl'
import { getMessages } from 'next-intl/server'
import AppProvider from '@/providers/app-provider'

interface LocaleLayoutProps {
  children: ReactNode
  params: Promise<{ locale: string }>
}

export default async function LocaleLayout({
  children,
  params,
}: LocaleLayoutProps) {
  const { locale } = await params
  const messages = await getMessages({ locale })

  return (
    <NextIntlClientProvider locale={locale} messages={messages}>
      <AppProvider>{children}</AppProvider>
    </NextIntlClientProvider>
  )
}
```

- [ ] **Step 3: Create (auth)/layout.tsx**

```tsx
// scm-web/src/app/[locale]/(auth)/layout.tsx
import { ReactNode } from 'react'
import AuthLayout from '@/layouts/auth-layout'

export default function AuthRouteLayout({ children }: { children: ReactNode }) {
  return <AuthLayout>{children}</AuthLayout>
}
```

- [ ] **Step 4: Create (app)/layout.tsx**

```tsx
// scm-web/src/app/[locale]/(app)/layout.tsx
import { ReactNode } from 'react'
import AdminLayout from '@/layouts/admin-layout'
import OfflineBanner from '@/components/ui/offline-banner'

export default function AppRouteLayout({ children }: { children: ReactNode }) {
  return (
    <>
      <OfflineBanner />
      <AdminLayout>{children}</AdminLayout>
    </>
  )
}
```

- [ ] **Step 5: Create (app)/loading.tsx**

```tsx
// scm-web/src/app/[locale]/(app)/loading.tsx
import { DashboardSkeleton } from '@/components/ui/loading-skeleton'

export default function Loading() {
  return <DashboardSkeleton />
}
```

- [ ] **Step 6: Create (app)/error.tsx**

```tsx
// scm-web/src/app/[locale]/(app)/error.tsx
'use client'

import { Button, Result } from 'antd'

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string }
  reset: () => void
}) {
  return (
    <Result
      status="error"
      title="页面加载出错"
      subTitle={error.message || '发生了一个意外错误'}
      extra={
        <Button type="primary" onClick={reset}>
          重试
        </Button>
      }
    />
  )
}
```

- [ ] **Step 7: Create (app)/not-found.tsx**

```tsx
// scm-web/src/app/[locale]/(app)/not-found.tsx
'use client'

import { Button, Result } from 'antd'
import { useRouter } from 'next/navigation'

export default function NotFound() {
  const router = useRouter()

  return (
    <Result
      status="404"
      title="页面不存在"
      subTitle="您访问的页面不存在或已被移除"
      extra={
        <Button type="primary" onClick={() => router.push('/dashboard')}>
          返回首页
        </Button>
      }
    />
  )
}
```

- [ ] **Step 8: Move existing pages to new route structure**

Move:
- `src/app/login/page.tsx` → `src/app/[locale]/(auth)/login/page.tsx`
- `src/app/dashboard/page.tsx` → `src/app/[locale]/(app)/dashboard/page.tsx`
- `src/app/users/page.tsx` → `src/app/[locale]/(app)/system/user/page.tsx`
- `src/app/users/[id]/page.tsx` → `src/app/[locale]/(app)/system/user/[id]/page.tsx`
- `src/app/roles/page.tsx` → `src/app/[locale]/(app)/system/role/page.tsx`

- [ ] **Step 9: Update moved pages to remove layout wrappers**

Update `dashboard/page.tsx` to remove `MainLayout` and `ProtectedRoute` wrappers:

```tsx
// scm-web/src/app/[locale]/(app)/dashboard/page.tsx
'use client'

import { Typography, Row, Col, Card, Statistic } from 'antd'
import {
  UserOutlined,
  ShoppingCartOutlined,
  InboxOutlined,
  TeamOutlined,
} from '@ant-design/icons'

const { Title } = Typography

export default function DashboardPage() {
  return (
    <>
      <Title level={2}>仪表盘</Title>
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic title="用户数" value={1128} prefix={<UserOutlined />} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="订单数"
              value={9280}
              prefix={<ShoppingCartOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="库存量"
              value={25600}
              prefix={<InboxOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="租户数"
              value={28}
              prefix={<TeamOutlined />}
            />
          </Card>
        </Col>
      </Row>
    </>
  )
}
```

- [ ] **Step 10: Delete old files**

Delete:
- `src/components/Layout/MainLayout.tsx`
- `src/components/Auth/ProtectedRoute.tsx`
- `src/app/login/page.tsx` (moved)
- `src/app/dashboard/page.tsx` (moved)
- `src/app/users/page.tsx` (moved)
- `src/app/users/[id]/page.tsx` (moved)
- `src/app/roles/page.tsx` (moved)

- [ ] **Step 11: Verify compilation**

Run: `cd scm-web && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 12: Commit**

```bash
git add scm-web/src/app/ scm-web/src/layouts/ scm-web/src/components/
git rm scm-web/src/components/Layout/MainLayout.tsx
git rm scm-web/src/components/Auth/ProtectedRoute.tsx
git commit -m "feat(frontend): restructure routes with auth/app layout groups"
```

---

### Task 8: Create Constants

**Files:**
- Create: `scm-web/src/constants/api-paths.ts`
- Create: `scm-web/src/constants/route-paths.ts`
- Create: `scm-web/src/constants/permissions.ts`
- Create: `scm-web/src/constants/storage-keys.ts`
- Create: `scm-web/src/constants/query-keys.ts`
- Create: `scm-web/src/constants/index.ts`

- [ ] **Step 1: Create constants files**

```typescript
// scm-web/src/constants/api-paths.ts
export const API_PATHS = {
  AUTH: {
    LOGIN: '/api/auth/login',
    LOGOUT: '/api/auth/logout',
    REFRESH: '/api/auth/refresh',
    ME: '/api/auth/me',
    MFA_VERIFY: '/api/auth/mfa/verify',
  },
  USER: {
    BASE: '/api/users',
    BY_ID: (id: string) => `/api/users/${id}`,
  },
  ORDER: {
    BASE: '/api/orders',
    BY_ID: (orderNo: string) => `/api/orders/${orderNo}`,
    STATUS: (orderNo: string) => `/api/orders/${orderNo}/status`,
  },
  PRODUCT: {
    BASE: '/api/products',
    BY_ID: (id: string) => `/api/products/${id}`,
    SEARCH: '/api/products/search',
  },
  INVENTORY: {
    BASE: '/api/inventory',
    BY_SKU: (skuId: string) => `/api/inventory/${skuId}`,
  },
} as const
```

```typescript
// scm-web/src/constants/route-paths.ts
export const ROUTE_PATHS = {
  LOGIN: '/login',
  DASHBOARD: '/dashboard',
  PRODUCT: {
    BASE: '/product',
    CREATE: '/product/create',
    BY_ID: (id: string) => `/product/${id}`,
    EDIT: (id: string) => `/product/${id}/edit`,
  },
  ORDER: {
    BASE: '/order',
    CREATE: '/order/create',
    BY_ID: (id: string) => `/order/${id}`,
  },
  INVENTORY: {
    BASE: '/inventory',
    ALERTS: '/inventory/alerts',
  },
  WAREHOUSE: {
    BASE: '/warehouse',
    INBOUND: '/warehouse/inbound',
    OUTBOUND: '/warehouse/outbound',
  },
  PURCHASE: {
    BASE: '/purchase',
    RFQ: '/purchase/rfq',
    QUOTATION: '/purchase/quotation',
  },
  SUPPLIER: {
    BASE: '/supplier',
  },
  LOGISTICS: {
    BASE: '/logistics',
    TRACKING: '/logistics/tracking',
    CARRIER: '/logistics/carrier',
  },
  FINANCE: {
    BASE: '/finance',
    SETTLEMENT: '/finance/settlement',
    INVOICE: '/finance/invoice',
  },
  SYSTEM: {
    USER: '/system/user',
    ROLE: '/system/role',
    PERMISSION: '/system/permission',
    DEPT: '/system/dept',
    DICTIONARY: '/system/dictionary',
  },
  SETTINGS: {
    PROFILE: '/settings/profile',
    SECURITY: '/settings/security',
    PREFERENCES: '/settings/preferences',
  },
} as const
```

```typescript
// scm-web/src/constants/permissions.ts
export const PERMISSIONS = {
  DASHBOARD: {
    VIEW: 'dashboard:view',
  },
  PRODUCT: {
    VIEW: 'product:view',
    CREATE: 'product:create',
    EDIT: 'product:edit',
    DELETE: 'product:delete',
    EXPORT: 'product:export',
    IMPORT: 'product:import',
  },
  ORDER: {
    VIEW: 'order:view',
    CREATE: 'order:create',
    EDIT: 'order:edit',
    DELETE: 'order:delete',
    CANCEL: 'order:cancel',
    REFUND: 'order:refund',
  },
  INVENTORY: {
    VIEW: 'inventory:view',
    ADJUST: 'inventory:adjust',
    EXPORT: 'inventory:export',
  },
  SYSTEM: {
    USER_VIEW: 'system:user:view',
    USER_CREATE: 'system:user:create',
    USER_EDIT: 'system:user:edit',
    USER_DELETE: 'system:user:delete',
    ROLE_VIEW: 'system:role:view',
    ROLE_CREATE: 'system:role:create',
    ROLE_EDIT: 'system:role:edit',
    ROLE_DELETE: 'system:role:delete',
  },
} as const
```

```typescript
// scm-web/src/constants/storage-keys.ts
export const STORAGE_KEYS = {
  AUTH: 'scm-auth',
  UI: 'scm-ui',
  TENANT: 'scm-tenant',
  PREFERENCE: 'scm-preference',
  TABLE_COLUMNS: (key: string) => `scm-table-columns-${key}`,
} as const
```

```typescript
// scm-web/src/constants/query-keys.ts
export const QUERY_KEYS = {
  AUTH: {
    ME: ['auth', 'me'] as const,
    PERMISSIONS: ['auth', 'permissions'] as const,
  },
  USER: {
    ALL: ['users'] as const,
    LIST: (params?: Record<string, unknown>) => ['users', 'list', params] as const,
    DETAIL: (id: string) => ['users', 'detail', id] as const,
  },
  PRODUCT: {
    ALL: ['products'] as const,
    LIST: (params?: Record<string, unknown>) => ['products', 'list', params] as const,
    DETAIL: (id: string) => ['products', 'detail', id] as const,
  },
  ORDER: {
    ALL: ['orders'] as const,
    LIST: (params?: Record<string, unknown>) => ['orders', 'list', params] as const,
    DETAIL: (orderNo: string) => ['orders', 'detail', orderNo] as const,
  },
  INVENTORY: {
    ALL: ['inventory'] as const,
    LIST: (params?: Record<string, unknown>) => ['inventory', 'list', params] as const,
    DETAIL: (skuId: string) => ['inventory', 'detail', skuId] as const,
  },
  DASHBOARD: {
    STATS: ['dashboard', 'stats'] as const,
    KPI: ['dashboard', 'kpi'] as const,
  },
} as const
```

```typescript
// scm-web/src/constants/index.ts
export * from './api-paths'
export * from './route-paths'
export * from './permissions'
export * from './storage-keys'
export * from './query-keys'
```

- [ ] **Step 2: Verify compilation**

Run: `cd scm-web && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 3: Commit**

```bash
git add scm-web/src/constants/
git commit -m "feat(frontend): add constants for API paths, routes, permissions, query keys"
```

---

### Task 9: Update Middleware with Auth Guard

**Files:**
- Modify: `scm-web/src/middleware.ts`

- [ ] **Step 1: Rewrite middleware.ts with auth guard**

```typescript
// scm-web/src/middleware.ts
import createMiddleware from 'next-intl/middleware'
import { routing } from './i18n/routing'
import { NextRequest, NextResponse } from 'next/server'

const intlMiddleware = createMiddleware(routing)

const publicPaths = ['/login', '/register', '/forgot-password']

export function middleware(request: NextRequest) {
  const { pathname } = request

  // Extract locale from pathname
  const localeMatch = pathname.match(/^\/([a-z]{2}-[A-Z]{2})(\/.*)?$/)
  const locale = localeMatch ? localeMatch[1] : null
  const pathWithoutLocale = localeMatch
    ? localeMatch[2] || '/'
    : pathname

  // Check if it's a public path
  const isPublicPath = publicPaths.some(
    (p) => pathWithoutLocale === p || pathWithoutLocale.startsWith(p + '/')
  )

  // Check for auth token (simplified - in production, verify JWT)
  const token = request.cookies.get('access_token')?.value

  // If not authenticated and not on public path, redirect to login
  if (!token && !isPublicPath && !pathWithoutLocale.startsWith('/api')) {
    const loginUrl = locale ? `/${locale}/login` : '/login'
    return NextResponse.redirect(new URL(loginUrl, request.url))
  }

  // If authenticated and on public path, redirect to dashboard
  if (token && isPublicPath) {
    const dashboardUrl = locale ? `/${locale}/dashboard` : '/dashboard'
    return NextResponse.redirect(new URL(dashboardUrl, request.url))
  }

  // Apply i18n middleware
  return intlMiddleware(request)
}

export const config = {
  matcher: ['/((?!api|_next|_vercel|.*\\..*).*)'],
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd scm-web && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 3: Commit**

```bash
git add scm-web/src/middleware.ts
git commit -m "feat(frontend): add auth guard to middleware"
```

---

### Task 10: Verify Full Build

**Files:**
- None (verification only)

- [ ] **Step 1: Run TypeScript type check**

Run: `cd scm-web && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 2: Run ESLint**

Run: `cd scm-web && npx next lint`
Expected: No errors

- [ ] **Step 3: Run build**

Run: `cd scm-web && npm run build`
Expected: Build succeeds

- [ ] **Step 4: Run dev server**

Run: `cd scm-web && npm run dev`
Expected: Server starts on http://localhost:3000

- [ ] **Step 5: Commit**

```bash
git add scm-web/
git commit -m "chore(frontend): verify Phase 0 infrastructure build"
```

---

## Summary

| Task | Files Created | Files Modified | Files Deleted |
|------|--------------|----------------|---------------|
| 1. Design Tokens | 3 | 0 | 0 |
| 2. Zustand Stores | 4 | 1 | 0 |
| 3. Axios Client | 0 | 1 | 0 |
| 4. Admin Shell | 6 | 0 | 0 |
| 5. Providers | 3 | 1 | 0 |
| 6. UI Components | 3 | 0 | 0 |
| 7. Route Structure | 6 | 2 | 5 |
| 8. Constants | 6 | 0 | 0 |
| 9. Middleware | 0 | 1 | 0 |
| 10. Verify Build | 0 | 0 | 0 |
| **Total** | **31** | **6** | **5** |

**Phase 0 Deliverables:**
- Design tokens with light/dark theme support
- Admin shell with ProLayout-style header and sidebar
- Zustand stores for auth, UI, tenant, preferences
- Axios client with proper interceptors and token refresh
- Route structure with auth/app layout groups
- Error boundaries, loading skeletons, offline banner
- Auth middleware with route guards
- Constants for API paths, routes, permissions, query keys

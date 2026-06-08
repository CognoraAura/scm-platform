# Frontend Phase 1: Authentication Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the authentication system with login/logout flow, auth provider for session initialization, permission loading, tenant switching, and password reset.

**Architecture:** Auth state lives in Zustand (memory only). Login stores tokens in Zustand + sets HttpOnly cookie for middleware. AuthProvider initializes session on app load by fetching current user. Permissions are loaded after login and cached in store.

**Tech Stack:** Next.js 15, React 19, Zustand 5, TanStack Query 5, Axios, Ant Design 5

---

## Current State

**Already in place (Phase 0):**
- `useAuthStore` with memory-based tokens, permissions, hasPermission
- Axios client with token refresh queue, tenant headers
- Auth middleware with route guards (checks cookie)
- Login page with basic form + MFA/TOTP support
- `useAuth` hooks (useLogin, useLogout, useCurrentUser)
- `authApi` endpoints (login, logout, refresh, me, verifyMfa)

**Gaps to fill:**
1. No auth provider to initialize session on page load
2. Login doesn't set cookie for middleware
3. Logout doesn't clear cookie
4. Permissions not fetched after login
5. No tenant switching UI
6. No forgot password page
7. Login page uses raw API calls instead of useLogin hook

---

## File Structure

### Files to Create

| File | Responsibility |
|------|---------------|
| `src/providers/auth-provider.tsx` | Initialize auth on app load, fetch current user |
| `src/app/[locale]/(auth)/forgot-password/page.tsx` | Password reset page |
| `src/hooks/use-permission.ts` | Permission check hook |
| `src/components/business/tenant-switcher.tsx` | Tenant switching dropdown |

### Files to Modify

| File | Changes |
|------|---------|
| `src/app/[locale]/(auth)/login/page.tsx` | Use useLogin hook, set cookie, fetch permissions |
| `src/stores/useAuthStore.ts` | Add setAuth action, cookie helpers |
| `src/lib/api/endpoints.ts` | Add tenant API endpoints |
| `src/providers/app-provider.tsx` | Add AuthProvider |
| `src/components/Layout/header.tsx` | Add tenant switcher |

---

## Tasks

### Task 1: Update AuthStore with Cookie Support

**Files:**
- Modify: `scm-web/src/stores/useAuthStore.ts`

- [ ] **Step 1: Update useAuthStore.ts with cookie helper and setAuth action**

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

function setCookie(name: string, value: string, days = 7) {
  const expires = new Date(Date.now() + days * 864e5).toUTCString()
  document.cookie = `${name}=${encodeURIComponent(value)}; expires=${expires}; path=/; SameSite=Lax`
}

function deleteCookie(name: string) {
  document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/`
}

interface AuthState {
  user: User | null
  accessToken: string | null
  refreshToken: string | null
  permissions: string[]
  isAuthenticated: boolean
  isLoading: boolean
  login: (user: User, accessToken: string, refreshToken: string) => void
  logout: () => void
  setUser: (user: User) => void
  setPermissions: (permissions: string[]) => void
  setLoading: (loading: boolean) => void
  hasPermission: (code: string) => boolean
  initialize: () => Promise<void>
}

export const useAuthStore = create<AuthState>()((set, get) => ({
  user: null,
  accessToken: null,
  refreshToken: null,
  permissions: [],
  isAuthenticated: false,
  isLoading: true,

  login: (user, accessToken, refreshToken) => {
    set({ user, accessToken, refreshToken, isAuthenticated: true, isLoading: false })
    // Set cookie for middleware auth check
    setCookie('access_token', accessToken)
  },

  logout: () => {
    set({
      user: null,
      accessToken: null,
      refreshToken: null,
      permissions: [],
      isAuthenticated: false,
      isLoading: false,
    })
    deleteCookie('access_token')
  },

  setUser: (user) => set({ user }),
  setPermissions: (permissions) => set({ permissions }),
  setLoading: (loading) => set({ isLoading: loading }),

  hasPermission: (code) => {
    const { permissions } = get()
    return permissions.includes(code) || permissions.includes('*')
  },

  initialize: async () => {
    const { accessToken } = get()
    if (!accessToken) {
      set({ isLoading: false })
      return
    }
    // User and permissions will be fetched by AuthProvider
  },
}))
```

- [ ] **Step 2: Verify compilation**

Run: `cd scm-web && npx tsc --noEmit`
Expected: No new errors

- [ ] **Step 3: Commit**

```bash
git add scm-web/src/stores/useAuthStore.ts
git commit -m "feat(frontend): add cookie support and loading state to auth store"
```

---

### Task 2: Create Auth Provider

**Files:**
- Create: `scm-web/src/providers/auth-provider.tsx`

- [ ] **Step 1: Create auth-provider.tsx**

```tsx
// scm-web/src/providers/auth-provider.tsx
'use client'

import { ReactNode, useEffect } from 'react'
import { useAuthStore } from '@/stores/useAuthStore'
import { authApi } from '@/lib/api/endpoints'
import { useTenantStore } from '@/stores/tenant-store'

interface AuthProviderProps {
  children: ReactNode
}

export default function AuthProvider({ children }: AuthProviderProps) {
  const { accessToken, setUser, setPermissions, setLoading, logout } =
    useAuthStore()
  const { setTenant, setTenantList } = useTenantStore()

  useEffect(() => {
    const initAuth = async () => {
      if (!accessToken) {
        setLoading(false)
        return
      }

      try {
        // Fetch current user
        const userResponse = (await authApi.me()) as {
          data: {
            id: string
            username: string
            displayName: string
            email?: string
            avatar?: string
            roleNames?: string[]
            permissions?: string[]
            currentTenant?: { id: string; name: string; code: string }
            tenants?: Array<{ id: string; name: string; code: string }>
          }
        }

        const userData = userResponse.data

        setUser({
          id: userData.id,
          username: userData.username,
          displayName: userData.displayName,
          email: userData.email,
          avatar: userData.avatar,
          roles: userData.roleNames,
        })

        if (userData.permissions) {
          setPermissions(userData.permissions)
        }

        if (userData.currentTenant) {
          setTenant(userData.currentTenant)
        }

        if (userData.tenants) {
          setTenantList(userData.tenants)
        }
      } catch {
        // Token invalid or expired — logout silently
        logout()
      } finally {
        setLoading(false)
      }
    }

    initAuth()
  }, [accessToken, setUser, setPermissions, setLoading, logout, setTenant, setTenantList])

  return <>{children}</>
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd scm-web && npx tsc --noEmit`
Expected: No new errors

- [ ] **Step 3: Commit**

```bash
git add scm-web/src/providers/auth-provider.tsx
git commit -m "feat(frontend): add auth provider for session initialization"
```

---

### Task 3: Update App Provider with Auth Provider

**Files:**
- Modify: `scm-web/src/providers/app-provider.tsx`

- [ ] **Step 1: Add AuthProvider to app-provider.tsx**

```tsx
// scm-web/src/providers/app-provider.tsx
'use client'

import { ReactNode } from 'react'
import QueryProvider from '@/components/providers/QueryProvider'
import ThemeProvider from './theme-provider'
import AuthProvider from './auth-provider'

interface AppProviderProps {
  children: ReactNode
}

export default function AppProvider({ children }: AppProviderProps) {
  return (
    <QueryProvider>
      <ThemeProvider>
        <AuthProvider>{children}</AuthProvider>
      </ThemeProvider>
    </QueryProvider>
  )
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd scm-web && npx tsc --noEmit`
Expected: No new errors

- [ ] **Step 3: Commit**

```bash
git add scm-web/src/providers/app-provider.tsx
git commit -m "feat(frontend): add auth provider to app provider composition"
```

---

### Task 4: Update Login Page to Use Hooks

**Files:**
- Modify: `scm-web/src/app/[locale]/(auth)/login/page.tsx`

- [ ] **Step 1: Rewrite login page with useLogin hook and proper flow**

```tsx
// scm-web/src/app/[locale]/(auth)/login/page.tsx
'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { Form, Input, Button, Card, Typography, message, Checkbox, Space } from 'antd'
import { UserOutlined, LockOutlined, SafetyOutlined } from '@ant-design/icons'
import { useLogin } from '@/lib/api/hooks/useAuth'
import { authApi } from '@/lib/api/endpoints'
import { useAuthStore } from '@/stores/useAuthStore'

const { Title, Text, Link } = Typography

interface LoginForm {
  username: string
  password: string
  remember?: boolean
}

interface TOTPForm {
  code: string
}

export default function LoginPage() {
  const [showTOTP, setShowTOTP] = useState(false)
  const [tempToken, setTempToken] = useState('')
  const [totpLoading, setTotpLoading] = useState(false)
  const router = useRouter()
  const loginMutation = useLogin()
  const { setPermissions } = useAuthStore()

  const onLoginFinish = async (values: LoginForm) => {
    try {
      const response = (await loginMutation.mutateAsync({
        username: values.username,
        password: values.password,
      })) as {
        data: {
          requireMfa?: boolean
          tempToken?: string
          accessToken?: string
          refreshToken?: string
          user?: Record<string, unknown>
          permissions?: string[]
        }
      }

      if (response.data.requireMfa) {
        setTempToken(response.data.tempToken!)
        setShowTOTP(true)
      } else {
        if (response.data.permissions) {
          setPermissions(response.data.permissions)
        }
        message.success('登录成功')
        router.push('/dashboard')
      }
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } } }
      message.error(err.response?.data?.message || '登录失败')
    }
  }

  const onTOTPFinish = async (values: TOTPForm) => {
    setTotpLoading(true)
    try {
      const response = (await authApi.verifyMfa({
        tempToken,
        code: values.code,
      })) as {
        data: {
          user: Record<string, unknown>
          accessToken: string
          refreshToken: string
          permissions?: string[]
        }
      }

      const { login } = useAuthStore.getState()
      login(
        response.data.user as Parameters<typeof login>[0],
        response.data.accessToken,
        response.data.refreshToken
      )

      if (response.data.permissions) {
        setPermissions(response.data.permissions)
      }

      message.success('登录成功')
      router.push('/dashboard')
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } } }
      message.error(err.response?.data?.message || '验证码错误')
    } finally {
      setTotpLoading(false)
    }
  }

  if (showTOTP) {
    return (
      <Card style={{ width: 400 }} bordered={false}>
        <Title level={3} style={{ textAlign: 'center', marginBottom: 8 }}>
          两步验证
        </Title>
        <Text
          type="secondary"
          style={{ display: 'block', textAlign: 'center', marginBottom: 24 }}
        >
          请输入您的TOTP验证码
        </Text>
        <Form name="totp" onFinish={onTOTPFinish} layout="vertical">
          <Form.Item
            name="code"
            rules={[
              { required: true, message: '请输入验证码' },
              { len: 6, message: '验证码为6位数字' },
              { pattern: /^\d+$/, message: '验证码只能包含数字' },
            ]}
          >
            <Input
              prefix={<SafetyOutlined />}
              placeholder="6位验证码"
              maxLength={6}
              style={{ textAlign: 'center', fontSize: 24, letterSpacing: 8 }}
            />
          </Form.Item>
          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              loading={totpLoading}
              block
              size="large"
            >
              验证
            </Button>
          </Form.Item>
          <Button type="link" block onClick={() => setShowTOTP(false)}>
            返回登录
          </Button>
        </Form>
      </Card>
    )
  }

  return (
    <Card style={{ width: 400 }} bordered={false}>
      <Title level={3} style={{ textAlign: 'center', marginBottom: 8 }}>
        SCM Platform
      </Title>
      <Text
        type="secondary"
        style={{ display: 'block', textAlign: 'center', marginBottom: 32 }}
      >
        供应链管理平台
      </Text>
      <Form
        name="login"
        onFinish={onLoginFinish}
        layout="vertical"
        initialValues={{ remember: true }}
      >
        <Form.Item
          name="username"
          rules={[{ required: true, message: '请输入用户名' }]}
        >
          <Input
            prefix={<UserOutlined />}
            placeholder="用户名"
            size="large"
          />
        </Form.Item>
        <Form.Item
          name="password"
          rules={[{ required: true, message: '请输入密码' }]}
        >
          <Input.Password
            prefix={<LockOutlined />}
            placeholder="密码"
            size="large"
          />
        </Form.Item>
        <Form.Item>
          <Space style={{ width: '100%', justifyContent: 'space-between' }}>
            <Form.Item name="remember" valuePropName="checked" noStyle>
              <Checkbox>记住我</Checkbox>
            </Form.Item>
            <Link href="/forgot-password">忘记密码?</Link>
          </Space>
        </Form.Item>
        <Form.Item>
          <Button
            type="primary"
            htmlType="submit"
            loading={loginMutation.isPending}
            block
            size="large"
          >
            登录
          </Button>
        </Form.Item>
      </Form>
    </Card>
  )
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd scm-web && npx tsc --noEmit`
Expected: No new errors

- [ ] **Step 3: Commit**

```bash
git add scm-web/src/app/\[locale\]/\(auth\)/login/page.tsx
git commit -m "feat(frontend): update login page to use hooks and add remember me"
```

---

### Task 5: Create Permission Hook

**Files:**
- Create: `scm-web/src/hooks/use-permission.ts`

- [ ] **Step 1: Create use-permission.ts**

```typescript
// scm-web/src/hooks/use-permission.ts
'use client'

import { useAuthStore } from '@/stores/useAuthStore'

export function usePermission() {
  const { hasPermission, permissions } = useAuthStore()

  const checkPermission = (code: string | string[]) => {
    if (Array.isArray(code)) {
      return code.some((c) => hasPermission(c))
    }
    return hasPermission(code)
  }

  const checkAllPermissions = (codes: string[]) => {
    return codes.every((c) => hasPermission(c))
  }

  return {
    hasPermission: checkPermission,
    hasAllPermissions: checkAllPermissions,
    permissions,
  }
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd scm-web && npx tsc --noEmit`
Expected: No new errors

- [ ] **Step 3: Commit**

```bash
git add scm-web/src/hooks/use-permission.ts
git commit -m "feat(frontend): add permission check hook"
```

---

### Task 6: Create Tenant Switcher Component

**Files:**
- Create: `scm-web/src/components/business/tenant-switcher.tsx`

- [ ] **Step 1: Create tenant-switcher.tsx**

```tsx
// scm-web/src/components/business/tenant-switcher.tsx
'use client'

import { Select, Space, Typography } from 'antd'
import { TeamOutlined } from '@ant-design/icons'
import { useTenantStore } from '@/stores/tenant-store'

const { Text } = Typography

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
```

- [ ] **Step 2: Verify compilation**

Run: `cd scm-web && npx tsc --noEmit`
Expected: No new errors

- [ ] **Step 3: Commit**

```bash
git add scm-web/src/components/business/tenant-switcher.tsx
git commit -m "feat(frontend): add tenant switcher component"
```

---

### Task 7: Add Tenant Switcher to Header

**Files:**
- Modify: `scm-web/src/components/Layout/header.tsx`

- [ ] **Step 1: Add TenantSwitcher import and usage to header**

Add import at top of file:
```typescript
import TenantSwitcher from '@/components/business/tenant-switcher'
```

Add TenantSwitcher before the fullscreen button in the right section:
```tsx
<TenantSwitcher />
```

The right section should look like:
```tsx
<div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
  <TenantSwitcher />
  <Tooltip title="全屏">
    <ExpandOutlined ... />
  </Tooltip>
  ...
</div>
```

- [ ] **Step 2: Verify compilation**

Run: `cd scm-web && npx tsc --noEmit`
Expected: No new errors

- [ ] **Step 3: Commit**

```bash
git add scm-web/src/components/Layout/header.tsx
git commit -m "feat(frontend): add tenant switcher to header"
```

---

### Task 8: Create Forgot Password Page

**Files:**
- Create: `scm-web/src/app/[locale]/(auth)/forgot-password/page.tsx`

- [ ] **Step 1: Create forgot-password page**

```tsx
// scm-web/src/app/[locale]/(auth)/forgot-password/page.tsx
'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { Form, Input, Button, Card, Typography, message, Steps, Result } from 'antd'
import { MailOutlined, LockOutlined, SafetyOutlined } from '@ant-design/icons'

const { Title, Text, Link } = Typography

export default function ForgotPasswordPage() {
  const [currentStep, setCurrentStep] = useState(0)
  const [loading, setLoading] = useState(false)
  const [email, setEmail] = useState('')
  const router = useRouter()

  const onEmailSubmit = async (values: { email: string }) => {
    setLoading(true)
    try {
      // TODO: Call API to send reset code
      setEmail(values.email)
      message.success('验证码已发送到您的邮箱')
      setCurrentStep(1)
    } catch {
      message.error('发送验证码失败')
    } finally {
      setLoading(false)
    }
  }

  const onCodeSubmit = async (values: { code: string }) => {
    setLoading(true)
    try {
      // TODO: Call API to verify code
      setCurrentStep(2)
    } catch {
      message.error('验证码错误')
    } finally {
      setLoading(false)
    }
  }

  const onPasswordSubmit = async (values: {
    password: string
    confirmPassword: string
  }) => {
    setLoading(true)
    try {
      // TODO: Call API to reset password
      message.success('密码重置成功')
      router.push('/login')
    } catch {
      message.error('密码重置失败')
    } finally {
      setLoading(false)
    }
  }

  const steps = [
    {
      title: '验证邮箱',
      content: (
        <Form onFinish={onEmailSubmit} layout="vertical">
          <Form.Item
            name="email"
            rules={[
              { required: true, message: '请输入邮箱地址' },
              { type: 'email', message: '请输入有效的邮箱地址' },
            ]}
          >
            <Input
              prefix={<MailOutlined />}
              placeholder="请输入注册邮箱"
              size="large"
            />
          </Form.Item>
          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              block
              size="large"
            >
              发送验证码
            </Button>
          </Form.Item>
        </Form>
      ),
    },
    {
      title: '验证身份',
      content: (
        <Form onFinish={onCodeSubmit} layout="vertical">
          <Text type="secondary" style={{ display: 'block', marginBottom: 16 }}>
            验证码已发送至 {email}
          </Text>
          <Form.Item
            name="code"
            rules={[
              { required: true, message: '请输入验证码' },
              { len: 6, message: '验证码为6位数字' },
            ]}
          >
            <Input
              prefix={<SafetyOutlined />}
              placeholder="6位验证码"
              maxLength={6}
              size="large"
            />
          </Form.Item>
          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              block
              size="large"
            >
              验证
            </Button>
          </Form.Item>
        </Form>
      ),
    },
    {
      title: '重置密码',
      content: (
        <Form onFinish={onPasswordSubmit} layout="vertical">
          <Form.Item
            name="password"
            rules={[
              { required: true, message: '请输入新密码' },
              { min: 8, message: '密码至少8位' },
            ]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="新密码"
              size="large"
            />
          </Form.Item>
          <Form.Item
            name="confirmPassword"
            dependencies={['password']}
            rules={[
              { required: true, message: '请确认密码' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('password') === value) {
                    return Promise.resolve()
                  }
                  return Promise.reject(new Error('两次密码不一致'))
                },
              }),
            ]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="确认密码"
              size="large"
            />
          </Form.Item>
          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              block
              size="large"
            >
              重置密码
            </Button>
          </Form.Item>
        </Form>
      ),
    },
  ]

  return (
    <Card style={{ width: 440 }} bordered={false}>
      <Title level={3} style={{ textAlign: 'center', marginBottom: 24 }}>
        找回密码
      </Title>
      <Steps current={currentStep} items={steps} style={{ marginBottom: 24 }} />
      {steps[currentStep].content}
      <div style={{ textAlign: 'center', marginTop: 16 }}>
        <Link href="/login">返回登录</Link>
      </div>
    </Card>
  )
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd scm-web && npx tsc --noEmit`
Expected: No new errors

- [ ] **Step 3: Commit**

```bash
git add scm-web/src/app/\[locale\]/\(auth\)/forgot-password/page.tsx
git commit -m "feat(frontend): add forgot password page with step wizard"
```

---

### Task 9: Update API Endpoints for Auth

**Files:**
- Modify: `scm-web/src/lib/api/endpoints.ts`

- [ ] **Step 1: Add missing auth endpoints**

Add these to the existing `authApi` object:

```typescript
forgotPassword: (email: string) =>
  apiClient.post('/api/auth/forgot-password', { email }),
verifyResetCode: (email: string, code: string) =>
  apiClient.post('/api/auth/verify-reset-code', { email, code }),
resetPassword: (token: string, password: string) =>
  apiClient.post('/api/auth/reset-password', { token, password }),
getPermissions: () => apiClient.get('/api/auth/permissions'),
```

Also add tenant API:

```typescript
export const tenantApi = {
  list: () => apiClient.get('/api/tenants'),
  switch: (tenantId: string) => apiClient.post(`/api/tenants/${tenantId}/switch`),
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd scm-web && npx tsc --noEmit`
Expected: No new errors

- [ ] **Step 3: Commit**

```bash
git add scm-web/src/lib/api/endpoints.ts
git commit -m "feat(frontend): add auth and tenant API endpoints"
```

---

### Task 10: Verify Full Build

**Files:**
- None (verification only)

- [ ] **Step 1: Run TypeScript type check**

Run: `cd scm-web && npx tsc --noEmit`
Expected: No new errors (pre-existing errors allowed)

- [ ] **Step 2: Run build**

Run: `cd scm-web && npm run build`
Expected: Build succeeds or only pre-existing errors

- [ ] **Step 3: Commit verification**

```bash
git add scm-web/
git commit -m "chore(frontend): verify Phase 1 authentication build"
```

---

## Summary

| Task | Files Created | Files Modified |
|------|--------------|----------------|
| 1. AuthStore Cookie Support | 0 | 1 |
| 2. Auth Provider | 1 | 0 |
| 3. App Provider Update | 0 | 1 |
| 4. Login Page Update | 0 | 1 |
| 5. Permission Hook | 1 | 0 |
| 6. Tenant Switcher | 1 | 0 |
| 7. Header Update | 0 | 1 |
| 8. Forgot Password | 1 | 0 |
| 9. API Endpoints | 0 | 1 |
| 10. Verify Build | 0 | 0 |
| **Total** | **4** | **5** |

**Phase 1 Deliverables:**
- Auth provider for session initialization on app load
- Login page with useLogin hook, cookie setting, permission loading
- Logout flow with cookie cleanup
- Permission check hook (usePermission)
- Tenant switcher component in header
- Forgot password page with step wizard
- Auth API endpoints for password reset

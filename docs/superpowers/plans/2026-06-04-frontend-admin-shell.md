# Frontend Admin Shell - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the admin frontend shell with login, user management, role/permission, and tenant dashboard - the minimum viable product for demonstrating the platform.

**Architecture:** React 18 + TypeScript + Vite + Ant Design Pro. Auto-generated API client from OpenAPI spec. JWT-based authentication with refresh token rotation.

**Tech Stack:** React 18, TypeScript 5, Vite 5, Ant Design 5, Ant Design Pro, Axios, OpenAPI Generator

---

## File Structure

```
scm-web/
  package.json
  tsconfig.json
  vite.config.ts
  index.html
  src/
    main.tsx
    App.tsx
    vite-env.d.ts
    api/
      generated/                    ← Auto-generated from OpenAPI
        api.ts
        base.ts
        configuration.ts
      client.ts                     ← Axios instance with interceptors
    components/
      Layout/
        MainLayout.tsx              ← Sidebar, header, breadcrumb
        ProtectedRoute.tsx          ← Auth guard
      Loading.tsx
    pages/
      Login/
        LoginPage.tsx
        TOTPVerification.tsx
      Dashboard/
        DashboardPage.tsx
      Users/
        UserListPage.tsx
        UserFormPage.tsx
      Roles/
        RoleListPage.tsx
        RoleFormPage.tsx
        PermissionTree.tsx
      Tenants/
        TenantListPage.tsx
        TenantDetailPage.tsx
    hooks/
      useAuth.ts
      useApi.ts
    store/
      authStore.ts                  ← Zustand or Context
    types/
      index.ts
    utils/
      token.ts
```

---

### Task 1: Project Scaffold and Layout

**Files:**
- Create: `scm-web/package.json`
- Create: `scm-web/tsconfig.json`
- Create: `scm-web/vite.config.ts`
- Create: `scm-web/index.html`
- Create: `scm-web/src/main.tsx`
- Create: `scm-web/src/App.tsx`

- [ ] **Step 1: Initialize Vite + React + TypeScript project**

```bash
cd D:\ProgramProject\scm-platform
npm create vite@latest scm-web -- --template react-ts
cd scm-web
npm install
```

- [ ] **Step 2: Install dependencies**

```bash
npm install antd @ant-design/pro-components @ant-design/icons
npm install axios react-router-dom zustand
npm install -D @openapitools/openapi-generator-cli
```

- [ ] **Step 3: Create main.tsx**

```tsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import App from './App';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ConfigProvider locale={zhCN}>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </ConfigProvider>
  </React.StrictMode>
);
```

- [ ] **Step 4: Create App.tsx with routing**

```tsx
import { Routes, Route, Navigate } from 'react-router-dom';
import MainLayout from './components/Layout/MainLayout';
import ProtectedRoute from './components/Layout/ProtectedRoute';
import LoginPage from './pages/Login/LoginPage';
import DashboardPage from './pages/Dashboard/DashboardPage';
import UserListPage from './pages/Users/UserListPage';
import RoleListPage from './pages/Roles/RoleListPage';
import TenantListPage from './pages/Tenants/TenantListPage';

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <MainLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<DashboardPage />} />
        <Route path="users" element={<UserListPage />} />
        <Route path="roles" element={<RoleListPage />} />
        <Route path="tenants" element={<TenantListPage />} />
      </Route>
    </Routes>
  );
}

export default App;
```

- [ ] **Step 5: Commit**

```bash
git add scm-web/
git commit -m "feat(frontend): initialize React + TypeScript + Vite project"
```

---

### Task 2: OpenAPI Client Generation

**Files:**
- Create: `scm-web/openapitools.json`
- Create: `scm-web/src/api/client.ts`

- [ ] **Step 1: Configure OpenAPI Generator**

Create `scm-web/openapitools.json`:
```json
{
  "$schema": "node_modules/@openapitools/openapi-generator-cli/config.schema.json",
  "spaces": 2,
  "generator-cli": {
    "version": "7.0.1",
    "generators": {
      "scm-api": {
        "generatorName": "typescript-axios",
        "output": "${projectDir}/src/api/generated",
        "inputSpec": "http://localhost:8106/v3/api-docs",
        "templateDir": "${projectDir}/templates",
        "additionalProperties": {
          "supportsES6": true,
          "typescriptThreePlus": true
        }
      }
    }
  }
}
```

- [ ] **Step 2: Create Axios client with interceptors**

Create `scm-web/src/api/client.ts`:
```typescript
import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { getToken, getRefreshToken, setToken, clearToken } from '../utils/token';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8106';

const client = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor - attach JWT
client.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor - handle 401 and token refresh
client.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };
    
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      
      try {
        const refreshToken = getRefreshToken();
        if (!refreshToken) {
          throw new Error('No refresh token');
        }
        
        const response = await axios.post(`${API_BASE_URL}/auth/refresh`, {
          refreshToken,
        });
        
        const { accessToken, refreshToken: newRefreshToken } = response.data.data;
        setToken(accessToken, newRefreshToken);
        
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return client(originalRequest);
      } catch (refreshError) {
        clearToken();
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }
    
    return Promise.reject(error);
  }
);

export default client;
```

- [ ] **Step 3: Generate API client**

```bash
cd scm-web
npm run generate-api
```

- [ ] **Step 4: Commit**

```bash
git add scm-web/
git commit -m "feat(frontend): add OpenAPI client generation and Axios interceptors"
```

---

### Task 3: Authentication UI

**Files:**
- Create: `scm-web/src/pages/Login/LoginPage.tsx`
- Create: `scm-web/src/pages/Login/TOTPVerification.tsx`
- Create: `scm-web/src/hooks/useAuth.ts`
- Create: `scm-web/src/store/authStore.ts`
- Create: `scm-web/src/utils/token.ts`

- [ ] **Step 1: Create token utilities**

Create `scm-web/src/utils/token.ts`:
```typescript
const ACCESS_TOKEN_KEY = 'scm_access_token';
const REFRESH_TOKEN_KEY = 'scm_refresh_token';

export function getToken(): string | null {
  return localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY);
}

export function setToken(accessToken: string, refreshToken: string): void {
  localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
  localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
}

export function clearToken(): void {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
}
```

- [ ] **Step 2: Create auth store**

Create `scm-web/src/store/authStore.ts`:
```typescript
import { create } from 'zustand';
import { clearToken, getToken } from '../utils/token';

interface User {
  id: string;
  username: string;
  email: string;
  roles: string[];
}

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (user: User, accessToken: string, refreshToken: string) => void;
  logout: () => void;
  setLoading: (loading: boolean) => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isAuthenticated: !!getToken(),
  isLoading: false,
  login: (user, accessToken, refreshToken) => {
    localStorage.setItem('scm_access_token', accessToken);
    localStorage.setItem('scm_refresh_token', refreshToken);
    set({ user, isAuthenticated: true });
  },
  logout: () => {
    clearToken();
    set({ user: null, isAuthenticated: false });
  },
  setLoading: (loading) => set({ isLoading: loading }),
}));
```

- [ ] **Step 3: Create LoginPage**

Create `scm-web/src/pages/Login/LoginPage.tsx`:
```tsx
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Form, Input, Button, Card, Typography, message } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { useAuthStore } from '../../store/authStore';
import client from '../../api/client';

const { Title } = Typography;

interface LoginForm {
  username: string;
  password: string;
}

export default function LoginPage() {
  const [loading, setLoading] = useState(false);
  const [showTOTP, setShowTOTP] = useState(false);
  const [tempToken, setTempToken] = useState('');
  const navigate = useNavigate();
  const { login } = useAuthStore();

  const onFinish = async (values: LoginForm) => {
    setLoading(true);
    try {
      const response = await client.post('/auth/login', values);
      const { data } = response.data;
      
      if (data.requireMfa) {
        setTempToken(data.tempToken);
        setShowTOTP(true);
      } else {
        login(data.user, data.accessToken, data.refreshToken);
        message.success('登录成功');
        navigate('/');
      }
    } catch (error: any) {
      message.error(error.response?.data?.message || '登录失败');
    } finally {
      setLoading(false);
    }
  };

  const onTOTPVerify = async (code: string) => {
    setLoading(true);
    try {
      const response = await client.post('/auth/mfa/verify', {
        tempToken,
        code,
      });
      const { data } = response.data;
      login(data.user, data.accessToken, data.refreshToken);
      message.success('登录成功');
      navigate('/');
    } catch (error: any) {
      message.error(error.response?.data?.message || '验证码错误');
    } finally {
      setLoading(false);
    }
  };

  if (showTOTP) {
    return <TOTPVerification onVerify={onTOTPVerify} loading={loading} />;
  }

  return (
    <div style={{ 
      display: 'flex', 
      justifyContent: 'center', 
      alignItems: 'center', 
      minHeight: '100vh',
      background: '#f0f2f5'
    }}>
      <Card style={{ width: 400 }}>
        <Title level={3} style={{ textAlign: 'center' }}>
          SCM Platform
        </Title>
        <Form
          name="login"
          initialValues={{ remember: true }}
          onFinish={onFinish}
        >
          <Form.Item
            name="username"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input prefix={<UserOutlined />} placeholder="用户名" />
          </Form.Item>
          <Form.Item
            name="password"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="密码" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block>
              登录
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
}
```

- [ ] **Step 4: Create ProtectedRoute**

Create `scm-web/src/components/Layout/ProtectedRoute.tsx`:
```tsx
import { Navigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';

interface ProtectedRouteProps {
  children: React.ReactNode;
}

export default function ProtectedRoute({ children }: ProtectedRouteProps) {
  const { isAuthenticated } = useAuthStore();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return <>{children}</>;
}
```

- [ ] **Step 5: Commit**

```bash
git add scm-web/
git commit -m "feat(frontend): add login page with JWT and TOTP support"
```

---

### Task 4: Main Layout

**Files:**
- Create: `scm-web/src/components/Layout/MainLayout.tsx`

- [ ] **Step 1: Create MainLayout with Ant Design Pro**

Create `scm-web/src/components/Layout/MainLayout.tsx`:
```tsx
import { useState } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { ProLayout, ProSettings } from '@ant-design/pro-components';
import { 
  DashboardOutlined, 
  UserOutlined, 
  SafetyOutlined,
  TeamOutlined,
  LogoutOutlined,
} from '@ant-design/icons';
import { Avatar, Dropdown, Space } from 'antd';
import { useAuthStore } from '../../store/authStore';

const menuRoutes = {
  routes: [
    {
      path: '/dashboard',
      name: '仪表盘',
      icon: <DashboardOutlined />,
    },
    {
      path: '/users',
      name: '用户管理',
      icon: <UserOutlined />,
    },
    {
      path: '/roles',
      name: '角色权限',
      icon: <SafetyOutlined />,
    },
    {
      path: '/tenants',
      name: '租户管理',
      icon: <TeamOutlined />,
    },
  ],
};

export default function MainLayout() {
  const [collapsed, setCollapsed] = useState(false);
  const { user, logout } = useAuthStore();
  const navigate = useNavigate();
  const location = useLocation();

  const settings: ProSettings = {
    fixSiderbar: true,
    layout: 'mix',
    splitMenus: false,
    title: 'SCM Platform',
  };

  return (
    <ProLayout
      title="SCM Platform"
      logo={null}
      {...menuRoutes}
      location={{ pathname: location.pathname }}
      collapsed={collapsed}
      onCollapse={setCollapsed}
      menuItemRender={(item, dom) => (
        <div onClick={() => navigate(item.path || '/')}>{dom}</div>
      )}
      avatarProps={{
        src: undefined,
        size: 'small',
        title: user?.username || 'User',
        render: (_, defaultDom) => (
          <Dropdown
            menu={{
              items: [
                {
                  key: 'logout',
                  icon: <LogoutOutlined />,
                  label: '退出登录',
                  onClick: () => {
                    logout();
                    navigate('/login');
                  },
                },
              ],
            }}
          >
            {defaultDom}
          </Dropdown>
        ),
      }}
      {...settings}
    >
      <Outlet />
    </ProLayout>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add scm-web/
git commit -m "feat(frontend): add main layout with sidebar navigation"
```

---

### Task 5: User Management Pages

**Files:**
- Create: `scm-web/src/pages/Users/UserListPage.tsx`
- Create: `scm-web/src/pages/Users/UserFormPage.tsx`

- [ ] **Step 1: Create UserListPage**

Create `scm-web/src/pages/Users/UserListPage.tsx`:
```tsx
import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ProTable, ProColumns } from '@ant-design/pro-components';
import { Button, Tag, Space, message, Popconfirm } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import client from '../../api/client';

interface User {
  id: string;
  username: string;
  email: string;
  phone: string;
  department: string;
  status: number;
  roles: string[];
  createdAt: string;
}

export default function UserListPage() {
  const navigate = useNavigate();

  const columns: ProColumns<User>[] = [
    {
      title: '用户名',
      dataIndex: 'username',
      key: 'username',
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      key: 'email',
    },
    {
      title: '部门',
      dataIndex: 'department',
      key: 'department',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (_, record) => (
        <Tag color={record.status === 1 ? 'green' : 'red'}>
          {record.status === 1 ? '启用' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '角色',
      dataIndex: 'roles',
      key: 'roles',
      render: (_, record) => (
        <Space>
          {record.roles.map((role) => (
            <Tag key={role} color="blue">{role}</Tag>
          ))}
        </Space>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      valueType: 'dateTime',
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => navigate(`/users/edit/${record.id}`)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确定删除此用户？"
            onConfirm={() => handleDelete(record.id)}
          >
            <Button type="link" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const handleDelete = async (id: string) => {
    try {
      await client.delete(`/users/${id}`);
      message.success('删除成功');
    } catch (error: any) {
      message.error(error.response?.data?.message || '删除失败');
    }
  };

  return (
    <ProTable<User>
      columns={columns}
      request={async (params) => {
        const response = await client.get('/users', { params });
        return {
          data: response.data.data.records,
          total: response.data.data.total,
        };
      }}
      rowKey="id"
      headerTitle="用户管理"
      toolBarRender={() => [
        <Button
          key="add"
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => navigate('/users/add')}
        >
          新增用户
        </Button>,
      ]}
    />
  );
}
```

- [ ] **Step 2: Create UserFormPage**

Create `scm-web/src/pages/Users/UserFormPage.tsx`:
```tsx
import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ProForm, ProFormText, ProFormSelect } from '@ant-design/pro-components';
import { Card, message } from 'antd';
import client from '../../api/client';

export default function UserFormPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const isEdit = !!id;

  const handleSubmit = async (values: any) => {
    try {
      if (isEdit) {
        await client.put(`/users/${id}`, values);
        message.success('更新成功');
      } else {
        await client.post('/users', values);
        message.success('创建成功');
      }
      navigate('/users');
    } catch (error: any) {
      message.error(error.response?.data?.message || '操作失败');
    }
  };

  return (
    <Card title={isEdit ? '编辑用户' : '新增用户'}>
      <ProForm
        onFinish={handleSubmit}
        request={async () => {
          if (!isEdit) return {};
          const response = await client.get(`/users/${id}`);
          return response.data.data;
        }}
      >
        <ProFormText
          name="username"
          label="用户名"
          rules={[{ required: true, message: '请输入用户名' }]}
        />
        <ProFormText
          name="email"
          label="邮箱"
          rules={[
            { required: true, message: '请输入邮箱' },
            { type: 'email', message: '请输入正确的邮箱' },
          ]}
        />
        <ProFormText
          name="phone"
          label="手机号"
        />
        <ProFormText
          name="department"
          label="部门"
        />
        <ProFormSelect
          name="roleIds"
          label="角色"
          mode="multiple"
          request={async () => {
            const response = await client.get('/roles');
            return response.data.data.map((role: any) => ({
              label: role.name,
              value: role.id,
            }));
          }}
        />
      </ProForm>
    </Card>
  );
}
```

- [ ] **Step 3: Commit**

```bash
git add scm-web/
git commit -m "feat(frontend): add user management pages with CRUD"
```

---

### Task 6: Role and Permission Management

**Files:**
- Create: `scm-web/src/pages/Roles/RoleListPage.tsx`
- Create: `scm-web/src/pages/Roles/PermissionTree.tsx`

- [ ] **Step 1: Create PermissionTree component**

Create `scm-web/src/pages/Roles/PermissionTree.tsx`:
```tsx
import { useState, useEffect } from 'react';
import { Tree, Checkbox } from 'antd';
import type { DataNode } from 'antd/es/tree';
import client from '../../api/client';

interface PermissionTreeProps {
  value?: string[];
  onChange?: (checkedKeys: string[]) => void;
}

export default function PermissionTree({ value = [], onChange }: PermissionTreeProps) {
  const [treeData, setTreeData] = useState<DataNode[]>([]);
  const [checkedKeys, setCheckedKeys] = useState<string[]>(value);

  useEffect(() => {
    loadPermissions();
  }, []);

  useEffect(() => {
    setCheckedKeys(value);
  }, [value]);

  const loadPermissions = async () => {
    try {
      const response = await client.get('/permissions/tree');
      setTreeData(buildTree(response.data.data));
    } catch (error) {
      console.error('Failed to load permissions:', error);
    }
  };

  const buildTree = (permissions: any[]): DataNode[] => {
    return permissions.map((p) => ({
      title: p.name,
      key: p.id,
      children: p.children ? buildTree(p.children) : undefined,
    }));
  };

  const onCheck = (checked: any) => {
    const keys = Array.isArray(checked) ? checked : checked.checked;
    setCheckedKeys(keys);
    onChange?.(keys);
  };

  return (
    <Tree
      checkable
      checkStrictly
      treeData={treeData}
      checkedKeys={checkedKeys}
      onCheck={onCheck}
    />
  );
}
```

- [ ] **Step 2: Create RoleListPage**

Create `scm-web/src/pages/Roles/RoleListPage.tsx`:
```tsx
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ProTable, ProColumns, ModalForm, ProFormText, ProFormTextArea } from '@ant-design/pro-components';
import { Button, Space, message, Popconfirm } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import PermissionTree from './PermissionTree';
import client from '../../api/client';

interface Role {
  id: string;
  name: string;
  code: string;
  description: string;
  permissions: string[];
  createdAt: string;
}

export default function RoleListPage() {
  const navigate = useNavigate();
  const [editingRole, setEditingRole] = useState<Role | null>(null);
  const [modalVisible, setModalVisible] = useState(false);

  const columns: ProColumns<Role>[] = [
    {
      title: '角色名称',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '角色编码',
      dataIndex: 'code',
      key: 'code',
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      valueType: 'dateTime',
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => {
              setEditingRole(record);
              setModalVisible(true);
            }}
          >
            编辑
          </Button>
          <Popconfirm
            title="确定删除此角色？"
            onConfirm={() => handleDelete(record.id)}
          >
            <Button type="link" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const handleDelete = async (id: string) => {
    try {
      await client.delete(`/roles/${id}`);
      message.success('删除成功');
    } catch (error: any) {
      message.error(error.response?.data?.message || '删除失败');
    }
  };

  const handleSubmit = async (values: any) => {
    try {
      if (editingRole) {
        await client.put(`/roles/${editingRole.id}`, values);
        message.success('更新成功');
      } else {
        await client.post('/roles', values);
        message.success('创建成功');
      }
      setModalVisible(false);
      setEditingRole(null);
    } catch (error: any) {
      message.error(error.response?.data?.message || '操作失败');
    }
  };

  return (
    <>
      <ProTable<Role>
        columns={columns}
        request={async (params) => {
          const response = await client.get('/roles', { params });
          return {
            data: response.data.data.records,
            total: response.data.data.total,
          };
        }}
        rowKey="id"
        headerTitle="角色管理"
        toolBarRender={() => [
          <Button
            key="add"
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => {
              setEditingRole(null);
              setModalVisible(true);
            }}
          >
            新增角色
          </Button>,
        ]}
      />

      <ModalForm
        title={editingRole ? '编辑角色' : '新增角色'}
        open={modalVisible}
        onOpenChange={setModalVisible}
        onFinish={handleSubmit}
        initialValues={editingRole}
      >
        <ProFormText
          name="name"
          label="角色名称"
          rules={[{ required: true, message: '请输入角色名称' }]}
        />
        <ProFormText
          name="code"
          label="角色编码"
          rules={[{ required: true, message: '请输入角色编码' }]}
        />
        <ProFormTextArea
          name="description"
          label="描述"
        />
        <ProForm.Item name="permissionIds" label="权限">
          <PermissionTree />
        </ProForm.Item>
      </ModalForm>
    </>
  );
}
```

- [ ] **Step 3: Commit**

```bash
git add scm-web/
git commit -m "feat(frontend): add role and permission management"
```

---

### Task 7: Tenant Dashboard

**Files:**
- Create: `scm-web/src/pages/Tenants/TenantListPage.tsx`
- Create: `scm-web/src/pages/Tenants/TenantDetailPage.tsx`

- [ ] **Step 1: Create TenantListPage**

Create `scm-web/src/pages/Tenants/TenantListPage.tsx`:
```tsx
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ProTable, ProColumns } from '@ant-design/pro-components';
import { Button, Tag, Space, message, Popconfirm } from 'antd';
import { PlusOutlined, EditOutlined, PauseCircleOutlined, PlayCircleOutlined } from '@ant-design/icons';
import client from '../../api/client';

interface Tenant {
  id: string;
  name: string;
  plan: string;
  status: number;
  userCount: number;
  storageUsed: string;
  createdAt: string;
}

export default function TenantListPage() {
  const navigate = useNavigate();

  const columns: ProColumns<Tenant>[] = [
    {
      title: '租户名称',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '套餐',
      dataIndex: 'plan',
      key: 'plan',
      render: (_, record) => (
        <Tag color={
          record.plan === 'enterprise' ? 'gold' :
          record.plan === 'professional' ? 'blue' : 'default'
        }>
          {record.plan === 'enterprise' ? '企业版' :
           record.plan === 'professional' ? '专业版' : '基础版'}
        </Tag>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (_, record) => (
        <Tag color={record.status === 1 ? 'green' : 'red'}>
          {record.status === 1 ? '正常' : '已停用'}
        </Tag>
      ),
    },
    {
      title: '用户数',
      dataIndex: 'userCount',
      key: 'userCount',
    },
    {
      title: '存储使用',
      dataIndex: 'storageUsed',
      key: 'storageUsed',
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      valueType: 'dateTime',
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            onClick={() => navigate(`/tenants/${record.id}`)}
          >
            详情
          </Button>
          <Popconfirm
            title={record.status === 1 ? '确定停用此租户？' : '确定启用此租户？'}
            onConfirm={() => handleToggleStatus(record)}
          >
            <Button
              type="link"
              icon={record.status === 1 ? <PauseCircleOutlined /> : <PlayCircleOutlined />}
            >
              {record.status === 1 ? '停用' : '启用'}
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const handleToggleStatus = async (tenant: Tenant) => {
    try {
      await client.patch(`/tenants/${tenant.id}/status`, {
        status: tenant.status === 1 ? 0 : 1,
      });
      message.success('操作成功');
    } catch (error: any) {
      message.error(error.response?.data?.message || '操作失败');
    }
  };

  return (
    <ProTable<Tenant>
      columns={columns}
      request={async (params) => {
        const response = await client.get('/tenants', { params });
        return {
          data: response.data.data.records,
          total: response.data.data.total,
        };
      }}
      rowKey="id"
      headerTitle="租户管理"
      toolBarRender={() => [
        <Button
          key="add"
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => navigate('/tenants/add')}
        >
          新增租户
        </Button>,
      ]}
    />
  );
}
```

- [ ] **Step 2: Create TenantDetailPage**

Create `scm-web/src/pages/Tenants/TenantDetailPage.tsx`:
```tsx
import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { Card, Descriptions, Progress, Row, Col, Statistic } from 'antd';
import { UserOutlined, DatabaseOutlined, ShoppingCartOutlined } from '@ant-design/icons';
import client from '../../api/client';

interface TenantDetail {
  id: string;
  name: string;
  plan: string;
  status: number;
  quotas: {
    apiCalls: { used: number; limit: number };
    storage: { used: number; limit: number };
    users: { used: number; limit: number };
    orders: { used: number; limit: number };
  };
  features: Record<string, boolean>;
  createdAt: string;
}

export default function TenantDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [tenant, setTenant] = useState<TenantDetail | null>(null);

  useEffect(() => {
    loadTenant();
  }, [id]);

  const loadTenant = async () => {
    try {
      const response = await client.get(`/tenants/${id}`);
      setTenant(response.data.data);
    } catch (error) {
      console.error('Failed to load tenant:', error);
    }
  };

  if (!tenant) {
    return <div>Loading...</div>;
  }

  return (
    <div>
      <Card title="租户信息" style={{ marginBottom: 16 }}>
        <Descriptions bordered>
          <Descriptions.Item label="租户名称">{tenant.name}</Descriptions.Item>
          <Descriptions.Item label="套餐">
            {tenant.plan === 'enterprise' ? '企业版' :
             tenant.plan === 'professional' ? '专业版' : '基础版'}
          </Descriptions.Item>
          <Descriptions.Item label="状态">
            {tenant.status === 1 ? '正常' : '已停用'}
          </Descriptions.Item>
          <Descriptions.Item label="创建时间">{tenant.createdAt}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="用户数"
              value={tenant.quotas.users.used}
              suffix={`/ ${tenant.quotas.users.limit}`}
              prefix={<UserOutlined />}
            />
            <Progress
              percent={Math.round(tenant.quotas.users.used / tenant.quotas.users.limit * 100)}
              size="small"
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="存储使用"
              value={tenant.quotas.storage.used}
              suffix={`/ ${tenant.quotas.storage.limit} GB`}
              prefix={<DatabaseOutlined />}
            />
            <Progress
              percent={Math.round(tenant.quotas.storage.used / tenant.quotas.storage.limit * 100)}
              size="small"
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="订单数"
              value={tenant.quotas.orders.used}
              suffix={`/ ${tenant.quotas.orders.limit}`}
              prefix={<ShoppingCartOutlined />}
            />
            <Progress
              percent={Math.round(tenant.quotas.orders.used / tenant.quotas.orders.limit * 100)}
              size="small"
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="API调用"
              value={tenant.quotas.apiCalls.used}
              suffix={`/ ${tenant.quotas.apiCalls.limit}`}
            />
            <Progress
              percent={Math.round(tenant.quotas.apiCalls.used / tenant.quotas.apiCalls.limit * 100)}
              size="small"
            />
          </Card>
        </Col>
      </Row>

      <Card title="功能开关">
        <Descriptions bordered column={2}>
          {Object.entries(tenant.features).map(([key, enabled]) => (
            <Descriptions.Item key={key} label={key}>
              <Tag color={enabled ? 'green' : 'default'}>
                {enabled ? '已启用' : '未启用'}
              </Tag>
            </Descriptions.Item>
          ))}
        </Descriptions>
      </Card>
    </div>
  );
}
```

- [ ] **Step 3: Commit**

```bash
git add scm-web/
git commit -m "feat(frontend): add tenant dashboard with quota monitoring"
```

---

### Task 8: Build and Test

- [ ] **Step 1: Build frontend**

```bash
cd scm-web
npm run build
```

Expected: Build succeeds with no errors

- [ ] **Step 2: Run lint**

```bash
npm run lint
```

Expected: No lint errors

- [ ] **Step 3: Commit**

```bash
git add scm-web/
git commit -m "chore(frontend): verify build and lint"
```

---

## Summary

| Task | Description | Status |
|------|-------------|--------|
| T069 | Project scaffold and layout | PENDING |
| T070 | OpenAPI client generation | PENDING |
| T071 | Authentication UI | PENDING |
| T072 | Main layout | PENDING |
| T073 | User management pages | PENDING |
| T074 | Role and permission management | PENDING |
| T075 | Tenant dashboard | PENDING |
| T076 | Build and test | PENDING |

**Total Estimated Effort:** 10-12 days

**Priority:** P1 - Start in Week 2, parallel with P0 backend fixes

**Dependencies:**
- OpenAPI spec must be available (T052-T056)
- Auth API must be stable

'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { Form, Input, Button, Card, Typography, message, Checkbox, Space } from 'antd'
import { UserOutlined, LockOutlined, SafetyOutlined } from '@ant-design/icons'
import { useMutation } from '@tanstack/react-query'
import { authApi } from '@/lib/api/endpoints'
import { useAuthStore } from '@/stores/useAuthStore'
import type { LoginRequest, LoginResponse } from '@/lib/api/types'

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
  const { login, setPermissions } = useAuthStore()

  const loginMutation = useMutation({
    mutationFn: (data: LoginRequest) => authApi.login(data),
  })

  const onLoginFinish = async (values: LoginForm) => {
    try {
      const response = (await loginMutation.mutateAsync({
        username: values.username,
        password: values.password,
      })) as unknown as { data: LoginResponse }

      if (response.data.requireMfa) {
        setTempToken(response.data.tempToken!)
        setShowTOTP(true)
      } else {
        const { user, accessToken, refreshToken, permissions } = response.data
        login(
          {
            id: user.id,
            username: user.username,
            displayName: user.displayName,
            email: user.email,
            avatar: user.avatar,
            roles: user.roleNames,
          },
          accessToken,
          refreshToken
        )
        if (permissions) {
          setPermissions(permissions)
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
      })) as unknown as { data: LoginResponse }

      const { user, accessToken, refreshToken, permissions } = response.data
      login(
        {
          id: user.id,
          username: user.username,
          displayName: user.displayName,
          email: user.email,
          avatar: user.avatar,
          roles: user.roleNames,
        },
        accessToken,
        refreshToken
      )
      if (permissions) {
        setPermissions(permissions)
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
        <Text type="secondary" style={{ display: 'block', textAlign: 'center', marginBottom: 24 }}>
          请输入您的TOTP验证码
        </Text>
        <Form name="totp" onFinish={onTOTPFinish} layout="vertical">
          <Form.Item name="code" rules={[{ required: true, message: '请输入验证码' }, { len: 6, message: '验证码为6位数字' }, { pattern: /^\d+$/, message: '验证码只能包含数字' }]}>
            <Input prefix={<SafetyOutlined />} placeholder="6位验证码" maxLength={6} style={{ textAlign: 'center', fontSize: 24, letterSpacing: 8 }} />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={totpLoading} block size="large">验证</Button>
          </Form.Item>
          <Button type="link" block onClick={() => setShowTOTP(false)}>返回登录</Button>
        </Form>
      </Card>
    )
  }

  return (
    <Card style={{ width: 400 }} bordered={false}>
      <Title level={3} style={{ textAlign: 'center', marginBottom: 8 }}>SCM Platform</Title>
      <Text type="secondary" style={{ display: 'block', textAlign: 'center', marginBottom: 32 }}>供应链管理平台</Text>
      <Form name="login" onFinish={onLoginFinish} layout="vertical" initialValues={{ remember: true }}>
        <Form.Item name="username" rules={[{ required: true, message: '请输入用户名' }]}>
          <Input prefix={<UserOutlined />} placeholder="用户名" size="large" />
        </Form.Item>
        <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
          <Input.Password prefix={<LockOutlined />} placeholder="密码" size="large" />
        </Form.Item>
        <Form.Item>
          <Space style={{ width: '100%', justifyContent: 'space-between' }}>
            <Form.Item name="remember" valuePropName="checked" noStyle><Checkbox>记住我</Checkbox></Form.Item>
            <Link href="/forgot-password">忘记密码?</Link>
          </Space>
        </Form.Item>
        <Form.Item>
          <Button type="primary" htmlType="submit" loading={loginMutation.isPending} block size="large">登录</Button>
        </Form.Item>
      </Form>
    </Card>
  )
}

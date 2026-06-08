'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { Form, Input, Button, Card, Typography, message } from 'antd'
import { UserOutlined, LockOutlined, SafetyOutlined } from '@ant-design/icons'
import { useAuthStore } from '@/stores/useAuthStore'
import { authApi } from '@/lib/api/endpoints'

const { Title } = Typography

interface LoginForm {
  username: string
  password: string
}

interface TOTPForm {
  code: string
}

export default function LoginPage() {
  const [loading, setLoading] = useState(false)
  const [showTOTP, setShowTOTP] = useState(false)
  const [tempToken, setTempToken] = useState('')
  const router = useRouter()
  const { login } = useAuthStore()

  const onLoginFinish = async (values: LoginForm) => {
    setLoading(true)
    try {
      const response = await authApi.login(values) as any
      const { data } = response

      if (data.requireMfa) {
        setTempToken(data.tempToken)
        setShowTOTP(true)
      } else {
        login(data.user, data.accessToken, data.refreshToken)
        message.success('登录成功')
        router.push('/dashboard')
      }
    } catch (error: any) {
      message.error(error.response?.data?.message || '登录失败')
    } finally {
      setLoading(false)
    }
  }

  const onTOTPFinish = async (values: TOTPForm) => {
    setLoading(true)
    try {
      const response = await authApi.verifyMfa({
        tempToken,
        code: values.code,
      }) as any
      const { data } = response
      login(data.user, data.accessToken, data.refreshToken)
      message.success('登录成功')
      router.push('/dashboard')
    } catch (error: any) {
      message.error(error.response?.data?.message || '验证码错误')
    } finally {
      setLoading(false)
    }
  }

  if (showTOTP) {
    return (
      <div
        style={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          minHeight: '100vh',
          background: '#f0f2f5',
        }}
      >
        <Card style={{ width: 400 }}>
          <Title level={3} style={{ textAlign: 'center' }}>
            两步验证
          </Title>
          <p style={{ textAlign: 'center', marginBottom: 24 }}>
            请输入您的TOTP验证码
          </p>
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
              <Button type="primary" htmlType="submit" loading={loading} block>
                验证
              </Button>
            </Form.Item>
          </Form>
        </Card>
      </div>
    )
  }

  return (
    <div
      style={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        minHeight: '100vh',
        background: '#f0f2f5',
      }}
    >
      <Card style={{ width: 400 }}>
        <Title level={3} style={{ textAlign: 'center' }}>
          SCM Platform
        </Title>
        <Form name="login" onFinish={onLoginFinish} layout="vertical">
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
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              block
              size="large"
            >
              登录
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  )
}

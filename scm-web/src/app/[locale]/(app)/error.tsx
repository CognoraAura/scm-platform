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

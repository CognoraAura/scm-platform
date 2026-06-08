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

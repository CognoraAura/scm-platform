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

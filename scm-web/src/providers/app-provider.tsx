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

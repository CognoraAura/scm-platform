import { ReactNode } from 'react'
import AuthLayout from '@/layouts/auth-layout'

export default function AuthRouteLayout({ children }: { children: ReactNode }) {
  return <AuthLayout>{children}</AuthLayout>
}

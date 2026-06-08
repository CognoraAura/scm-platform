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

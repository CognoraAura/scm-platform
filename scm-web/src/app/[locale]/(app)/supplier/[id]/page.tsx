'use client'
import { usePathname } from 'next/navigation'
import { SupplierDetail } from '@/features/supplier/components'

export default function SupplierDetailPage() {
  const pathname = usePathname()
  const id = pathname.split('/').pop() || ''
  return <SupplierDetail id={id} />
}

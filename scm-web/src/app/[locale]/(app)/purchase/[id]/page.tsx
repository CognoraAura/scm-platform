'use client'
import { usePathname } from 'next/navigation'
import { PurchaseOrderDetail } from '@/features/purchase/components'

export default function PurchaseDetailPage() {
  const pathname = usePathname()
  const id = pathname.split('/').pop() || ''
  return <PurchaseOrderDetail id={id} />
}

'use client'
import { usePathname } from 'next/navigation'
import { OrderDetail } from '@/features/order/components'

export default function OrderDetailPage() {
  const pathname = usePathname()
  const id = pathname.split('/').pop() || ''
  return <OrderDetail id={id} />
}

'use client'
import { usePathname } from 'next/navigation'
import { ProductDetail } from '@/features/product/components'

export default function ProductDetailPage() {
  const pathname = usePathname()
  const id = pathname.split('/').pop() || ''
  return <ProductDetail id={id} />
}

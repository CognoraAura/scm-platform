'use client'
import { usePathname } from 'next/navigation'
import { ProductForm } from '@/features/product/components'

export default function EditProductPage() {
  const pathname = usePathname()
  const id = pathname.split('/')[2] || ''
  return <ProductForm id={id} />
}

'use client'
import { useQuery } from '@tanstack/react-query'
import { productService } from '../services/product.service'

export function useCategoryList() {
  return useQuery({ queryKey: ['categories'], queryFn: () => productService.listCategories() })
}

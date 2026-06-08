'use client'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { productService } from '../services/product.service'
import type { ProductSearchParams } from '../types'

export function useProductList(params?: ProductSearchParams) {
  return useQuery({ queryKey: ['products', 'list', params], queryFn: () => productService.list(params) })
}
export function useProductDetail(id: string) {
  return useQuery({ queryKey: ['products', 'detail', id], queryFn: () => productService.getById(id), enabled: !!id })
}
export function useCreateProduct() {
  const qc = useQueryClient()
  return useMutation({ mutationFn: (data: Parameters<typeof productService.create>[0]) => productService.create(data), onSuccess: () => { qc.invalidateQueries({ queryKey: ['products'] }); message.success('创建成功') } })
}
export function useUpdateProduct() {
  const qc = useQueryClient()
  return useMutation({ mutationFn: ({ id, data }: { id: string; data: Parameters<typeof productService.update>[1] }) => productService.update(id, data), onSuccess: () => { qc.invalidateQueries({ queryKey: ['products'] }); message.success('更新成功') } })
}
export function useDeleteProduct() {
  const qc = useQueryClient()
  return useMutation({ mutationFn: (id: string) => productService.delete(id), onSuccess: () => { qc.invalidateQueries({ queryKey: ['products'] }); message.success('删除成功') } })
}

'use client'
import { useQuery } from '@tanstack/react-query'
import { purchaseService } from '../services/purchase.service'
import type { PurchaseSearchParams } from '../types'

export function usePurchaseOrderList(params?: PurchaseSearchParams) {
  return useQuery({ queryKey: ['purchase', 'list', params], queryFn: () => purchaseService.list(params) })
}
export function usePurchaseOrderDetail(id: string) {
  return useQuery({ queryKey: ['purchase', 'detail', id], queryFn: () => purchaseService.getById(id), enabled: !!id })
}

'use client'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { inventoryService } from '../services/inventory.service'
import type { InventorySearchParams } from '../types'

export function useInventoryList(params?: InventorySearchParams) {
  return useQuery({ queryKey: ['inventory', 'list', params], queryFn: () => inventoryService.list(params) })
}
export function useStockAlerts() {
  return useQuery({ queryKey: ['inventory', 'alerts'], queryFn: () => inventoryService.getAlerts() })
}
export function useStockMovements(skuId?: string) {
  return useQuery({ queryKey: ['inventory', 'movements', skuId], queryFn: () => inventoryService.getMovements(skuId) })
}
export function useAdjustStock() {
  const qc = useQueryClient()
  return useMutation({ mutationFn: ({ skuId, quantity, reason }: { skuId: string; quantity: number; reason: string }) => inventoryService.adjustStock(skuId, quantity, reason), onSuccess: () => { qc.invalidateQueries({ queryKey: ['inventory'] }); message.success('库存调整成功') } })
}

'use client'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { orderService } from '../services/order.service'
import type { OrderSearchParams, OrderStatus } from '../types'

export function useOrderList(params?: OrderSearchParams) {
  return useQuery({ queryKey: ['orders', 'list', params], queryFn: () => orderService.list(params) })
}
export function useOrderDetail(id: string) {
  return useQuery({ queryKey: ['orders', 'detail', id], queryFn: () => orderService.getById(id), enabled: !!id })
}
export function useUpdateOrderStatus() {
  const qc = useQueryClient()
  return useMutation({ mutationFn: ({ id, status }: { id: string; status: OrderStatus }) => orderService.updateStatus(id, status), onSuccess: () => { qc.invalidateQueries({ queryKey: ['orders'] }); message.success('状态更新成功') } })
}

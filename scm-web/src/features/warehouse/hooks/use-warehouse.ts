'use client'
import { useQuery } from '@tanstack/react-query'
import { warehouseService } from '../services/warehouse.service'

export function useWarehouseList() {
  return useQuery({ queryKey: ['warehouses'], queryFn: () => warehouseService.listWarehouses() })
}
export function useInboundList() {
  return useQuery({ queryKey: ['warehouse', 'inbound'], queryFn: () => warehouseService.listInbound() })
}
export function useOutboundList() {
  return useQuery({ queryKey: ['warehouse', 'outbound'], queryFn: () => warehouseService.listOutbound() })
}

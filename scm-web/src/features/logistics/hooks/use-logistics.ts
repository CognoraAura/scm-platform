'use client'
import { useQuery } from '@tanstack/react-query'
import { logisticsService } from '../services/logistics.service'

export function useWaybillList() {
  return useQuery({ queryKey: ['logistics', 'waybills'], queryFn: () => logisticsService.listWaybills() })
}
export function useTrackingEvents(waybillId: string) {
  return useQuery({ queryKey: ['logistics', 'tracking', waybillId], queryFn: () => logisticsService.getTracking(waybillId), enabled: !!waybillId })
}

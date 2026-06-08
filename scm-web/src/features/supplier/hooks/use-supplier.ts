'use client'
import { useQuery } from '@tanstack/react-query'
import { supplierService } from '../services/supplier.service'
import type { SupplierSearchParams } from '../types'

export function useSupplierList(params?: SupplierSearchParams) {
  return useQuery({ queryKey: ['suppliers', 'list', params], queryFn: () => supplierService.list(params) })
}
export function useSupplierDetail(id: string) {
  return useQuery({ queryKey: ['suppliers', 'detail', id], queryFn: () => supplierService.getById(id), enabled: !!id })
}

'use client'
import { useQuery } from '@tanstack/react-query'
import { financeService } from '../services/finance.service'

export function useSettlementList() {
  return useQuery({ queryKey: ['finance', 'settlements'], queryFn: () => financeService.listSettlements() })
}
export function useInvoiceList() {
  return useQuery({ queryKey: ['finance', 'invoices'], queryFn: () => financeService.listInvoices() })
}

import type { Settlement, Invoice } from '../types'

const mockSettlements: Settlement[] = [
  { id: '1', settlementNo: 'STL-2026-001', supplierName: 'Apple供应链', amount: 550000, status: 'completed', statusLabel: '已完成', period: '2026-05', createdAt: '2026-06-01' },
  { id: '2', settlementNo: 'STL-2026-002', supplierName: 'Samsung代理', amount: 480000, status: 'processing', statusLabel: '处理中', period: '2026-06', createdAt: '2026-06-08' },
]

const mockInvoices: Invoice[] = [
  { id: '1', invoiceNo: 'INV-2026-001', settlementNo: 'STL-2026-001', amount: 550000, taxAmount: 71500, status: 'received', statusLabel: '已收票', createdAt: '2026-06-05' },
  { id: '2', invoiceNo: 'INV-2026-002', settlementNo: 'STL-2026-002', amount: 480000, taxAmount: 62400, status: 'pending', statusLabel: '待开票', createdAt: '2026-06-08' },
]

export const financeService = {
  listSettlements: async () => mockSettlements,
  listInvoices: async () => mockInvoices,
}

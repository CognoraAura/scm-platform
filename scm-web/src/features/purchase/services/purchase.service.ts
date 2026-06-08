import type { PurchaseOrder, PurchaseSearchParams } from '../types'

const statusMap: Record<string, { label: string; color: string }> = {
  draft: { label: '草稿', color: 'default' },
  pending: { label: '待审批', color: 'warning' },
  approved: { label: '已审批', color: 'processing' },
  ordered: { label: '已下单', color: 'blue' },
  received: { label: '已收货', color: 'purple' },
  completed: { label: '已完成', color: 'success' },
  cancelled: { label: '已取消', color: 'error' },
}

const mockPurchaseOrders: PurchaseOrder[] = [
  { id: '1', purchaseNo: 'PO-2026-001', supplierId: '1', supplierName: 'Apple供应链', items: [
    { id: '11', purchaseOrderId: '1', skuId: '11', skuCode: 'SKU-001-128', productName: 'iPhone 15 Pro 128G', quantity: 100, unitPrice: 5500, totalPrice: 550000, receivedQuantity: 100 },
  ], totalAmount: 550000, status: 'completed', statusLabel: '已完成', createdAt: '2026-05-15', updatedAt: '2026-06-01' },
  { id: '2', purchaseNo: 'PO-2026-002', supplierId: '1', supplierName: 'Apple供应链', items: [
    { id: '21', purchaseOrderId: '2', skuId: '21', skuCode: 'SKU-002-256', productName: 'MacBook Air M3 256G', quantity: 50, unitPrice: 7000, totalPrice: 350000, receivedQuantity: 30 },
  ], totalAmount: 350000, status: 'received', statusLabel: '已收货', createdAt: '2026-06-01', updatedAt: '2026-06-08' },
  { id: '3', purchaseNo: 'PO-2026-003', supplierId: '2', supplierName: 'Samsung代理', items: [
    { id: '31', purchaseOrderId: '3', skuId: '41', skuCode: 'SKU-004', productName: 'Galaxy S24 Ultra', quantity: 80, unitPrice: 6000, totalPrice: 480000, receivedQuantity: 0 },
  ], totalAmount: 480000, status: 'approved', statusLabel: '已审批', createdAt: '2026-06-05', updatedAt: '2026-06-07' },
]

export const purchaseService = {
  list: async (params?: PurchaseSearchParams) => {
    let filtered = [...mockPurchaseOrders]
    if (params?.keyword) filtered = filtered.filter(o => o.purchaseNo.includes(params.keyword!) || o.supplierName.includes(params.keyword!))
    if (params?.status) filtered = filtered.filter(o => o.status === params.status)
    const start = ((params?.current || 1) - 1) * (params?.pageSize || 20)
    return { records: filtered.slice(start, start + (params?.pageSize || 20)), total: filtered.length, current: params?.current || 1, pageSize: params?.pageSize || 20 }
  },
  getById: async (id: string): Promise<PurchaseOrder> => {
    const o = mockPurchaseOrders.find(x => x.id === id)
    if (!o) throw new Error('PurchaseOrder not found')
    return o
  },
  getStatusMap: () => statusMap,
}

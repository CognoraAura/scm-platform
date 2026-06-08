export interface PurchaseOrder {
  id: string
  purchaseNo: string
  supplierId: string
  supplierName: string
  items: PurchaseOrderItem[]
  totalAmount: number
  status: 'draft' | 'pending' | 'approved' | 'ordered' | 'received' | 'completed' | 'cancelled'
  statusLabel: string
  remark?: string
  createdAt: string
  updatedAt: string
}

export interface PurchaseOrderItem {
  id: string
  purchaseOrderId: string
  skuId: string
  skuCode: string
  productName: string
  quantity: number
  unitPrice: number
  totalPrice: number
  receivedQuantity: number
}

export interface PurchaseSearchParams {
  current?: number
  pageSize?: number
  keyword?: string
  status?: string
  supplierId?: string
}

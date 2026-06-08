export interface Order {
  id: string
  orderNo: string
  customerName: string
  customerPhone?: string
  items: OrderItem[]
  totalAmount: number
  discountAmount: number
  payAmount: number
  status: OrderStatus
  statusLabel: string
  payTime?: string
  shipTime?: string
  completeTime?: string
  cancelTime?: string
  remark?: string
  createdAt: string
  updatedAt: string
}

export interface OrderItem {
  id: string
  orderId: string
  skuId: string
  skuCode: string
  productName: string
  quantity: number
  unitPrice: number
  totalPrice: number
}

export type OrderStatus = 'PENDING_PAYMENT' | 'PAID' | 'PENDING_SHIP' | 'SHIPPED' | 'IN_TRANSIT' | 'DELIVERED' | 'COMPLETED' | 'CANCELLED' | 'REFUNDING'

export interface RefundRequest {
  id: string
  orderId: string
  orderNo: string
  reason: string
  amount: number
  status: 'pending' | 'approved' | 'rejected' | 'completed'
  createdAt: string
}

export interface OrderSearchParams {
  current?: number
  pageSize?: number
  keyword?: string
  status?: string
}

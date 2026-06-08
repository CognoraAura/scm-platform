import type { Order, RefundRequest, OrderSearchParams, OrderStatus } from '../types'

const statusFlow: Record<OrderStatus, { label: string; color: string }> = {
  PENDING_PAYMENT: { label: '待付款', color: 'warning' },
  PAID: { label: '已付款', color: 'processing' },
  PENDING_SHIP: { label: '待发货', color: 'processing' },
  SHIPPED: { label: '已发货', color: 'purple' },
  IN_TRANSIT: { label: '运输中', color: 'blue' },
  DELIVERED: { label: '已送达', color: 'cyan' },
  COMPLETED: { label: '已完成', color: 'success' },
  CANCELLED: { label: '已取消', color: 'default' },
  REFUNDING: { label: '退款中', color: 'error' },
}

const mockOrders: Order[] = [
  { id: '1', orderNo: 'ORD-2026-001', customerName: '张三', customerPhone: '13800138001', items: [
    { id: '11', orderId: '1', skuId: '11', skuCode: 'SKU-001-128', productName: 'iPhone 15 Pro 128G', quantity: 1, unitPrice: 7999, totalPrice: 7999 },
  ], totalAmount: 7999, discountAmount: 0, payAmount: 7999, status: 'COMPLETED', statusLabel: '已完成', payTime: '2026-06-01 10:30', shipTime: '2026-06-02 14:00', completeTime: '2026-06-05 16:30', createdAt: '2026-06-01 10:00', updatedAt: '2026-06-05 16:30' },
  { id: '2', orderNo: 'ORD-2026-002', customerName: '李四', items: [
    { id: '21', orderId: '2', skuId: '21', skuCode: 'SKU-002-256', productName: 'MacBook Air M3 256G', quantity: 1, unitPrice: 9999, totalPrice: 9999 },
  ], totalAmount: 9999, discountAmount: 500, payAmount: 9499, status: 'SHIPPED', statusLabel: '已发货', payTime: '2026-06-07 09:00', shipTime: '2026-06-08 11:00', createdAt: '2026-06-07 08:30', updatedAt: '2026-06-08 11:00' },
  { id: '3', orderNo: 'ORD-2026-003', customerName: '王五', items: [
    { id: '31', orderId: '3', skuId: '31', skuCode: 'SKU-003', productName: 'AirPods Pro 2', quantity: 2, unitPrice: 1899, totalPrice: 3798 },
  ], totalAmount: 3798, discountAmount: 0, payAmount: 3798, status: 'PENDING_PAYMENT', statusLabel: '待付款', createdAt: '2026-06-08 14:00', updatedAt: '2026-06-08 14:00' },
  { id: '4', orderNo: 'ORD-2026-004', customerName: '赵六', items: [
    { id: '41', orderId: '4', skuId: '11', skuCode: 'SKU-001-128', productName: 'iPhone 15 Pro 128G', quantity: 2, unitPrice: 7999, totalPrice: 15998 },
  ], totalAmount: 15998, discountAmount: 1000, payAmount: 14998, status: 'PAID', statusLabel: '已付款', payTime: '2026-06-08 10:30', createdAt: '2026-06-08 10:00', updatedAt: '2026-06-08 10:30' },
  { id: '5', orderNo: 'ORD-2026-005', customerName: '钱七', items: [
    { id: '51', orderId: '5', skuId: '12', skuCode: 'SKU-001-256', productName: 'iPhone 15 Pro 256G', quantity: 1, unitPrice: 8999, totalPrice: 8999 },
  ], totalAmount: 8999, discountAmount: 0, payAmount: 8999, status: 'CANCELLED', statusLabel: '已取消', cancelTime: '2026-06-07 15:00', createdAt: '2026-06-07 14:00', updatedAt: '2026-06-07 15:00' },
]

export const orderService = {
  list: async (params?: OrderSearchParams) => {
    let filtered = [...mockOrders]
    if (params?.keyword) filtered = filtered.filter(o => o.orderNo.includes(params.keyword!) || o.customerName.includes(params.keyword!))
    if (params?.status) filtered = filtered.filter(o => o.status === params.status)
    const start = ((params?.current || 1) - 1) * (params?.pageSize || 20)
    return { records: filtered.slice(start, start + (params?.pageSize || 20)), total: filtered.length, current: params?.current || 1, pageSize: params?.pageSize || 20 }
  },
  getById: async (id: string): Promise<Order> => {
    const o = mockOrders.find(x => x.id === id)
    if (!o) throw new Error('Order not found')
    return o
  },
  updateStatus: async (id: string, status: OrderStatus): Promise<void> => {},
  getStatusFlow: () => statusFlow,
}

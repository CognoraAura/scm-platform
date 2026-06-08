import type { Warehouse, InboundOrder, OutboundOrder } from '../types'

const mockWarehouses: Warehouse[] = [
  { id: '1', name: '主仓库', code: 'WH-001', address: '上海市浦东新区', contactPerson: '张仓管', contactPhone: '13800000001', status: 'active', createdAt: '2025-01-01' },
  { id: '2', name: '华东仓', code: 'WH-002', address: '杭州市余杭区', contactPerson: '李仓管', contactPhone: '13800000002', status: 'active', createdAt: '2025-02-01' },
]

const mockInbound: InboundOrder[] = [
  { id: '1', inboundNo: 'IN-2026-001', warehouseName: '主仓库', supplierName: 'Apple供应链', items: [
    { id: '11', skuCode: 'SKU-001-128', productName: 'iPhone 15 Pro 128G', expectedQuantity: 100, actualQuantity: 100 },
  ], status: 'completed', statusLabel: '已完成', createdAt: '2026-06-01' },
  { id: '2', inboundNo: 'IN-2026-002', warehouseName: '主仓库', supplierName: 'Apple供应链', items: [
    { id: '21', skuCode: 'SKU-002-256', productName: 'MacBook Air M3 256G', expectedQuantity: 50, actualQuantity: 30 },
  ], status: 'receiving', statusLabel: '收货中', createdAt: '2026-06-08' },
]

const mockOutbound: OutboundOrder[] = [
  { id: '1', outboundNo: 'OUT-2026-001', warehouseName: '主仓库', items: [
    { id: '11', skuCode: 'SKU-001-128', productName: 'iPhone 15 Pro 128G', quantity: 2 },
  ], status: 'completed', statusLabel: '已完成', createdAt: '2026-06-05' },
  { id: '2', outboundNo: 'OUT-2026-002', warehouseName: '主仓库', items: [
    { id: '21', skuCode: 'SKU-002-256', productName: 'MacBook Air M3 256G', quantity: 1 },
  ], status: 'picking', statusLabel: '拣货中', createdAt: '2026-06-08' },
]

export const warehouseService = {
  listWarehouses: async () => mockWarehouses,
  listInbound: async () => mockInbound,
  listOutbound: async () => mockOutbound,
}

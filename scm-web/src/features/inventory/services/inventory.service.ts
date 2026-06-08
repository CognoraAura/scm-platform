import type { InventoryItem, StockMovement, StockAlert, InventorySearchParams } from '../types'

const mockInventory: InventoryItem[] = [
  { id: '1', skuId: '11', skuCode: 'SKU-001-128', productName: 'iPhone 15 Pro 128G', warehouseId: '1', warehouseName: '主仓库', totalStock: 50, availableStock: 45, reservedStock: 5, threshold: 20, status: 'normal', lastUpdatedAt: '2026-06-08 14:30' },
  { id: '2', skuId: '12', skuCode: 'SKU-001-256', productName: 'iPhone 15 Pro 256G', warehouseId: '1', warehouseName: '主仓库', totalStock: 12, availableStock: 10, reservedStock: 2, threshold: 20, status: 'low', lastUpdatedAt: '2026-06-08 13:15' },
  { id: '3', skuId: '21', skuCode: 'SKU-002-256', productName: 'MacBook Air M3 256G', warehouseId: '1', warehouseName: '主仓库', totalStock: 5, availableStock: 3, reservedStock: 2, threshold: 15, status: 'critical', lastUpdatedAt: '2026-06-08 12:00' },
  { id: '4', skuId: '31', skuCode: 'SKU-003', productName: 'AirPods Pro 2', warehouseId: '1', warehouseName: '主仓库', totalStock: 0, availableStock: 0, reservedStock: 0, threshold: 50, status: 'out', lastUpdatedAt: '2026-06-08 10:00' },
]

const mockMovements: StockMovement[] = [
  { id: '1', skuId: '11', skuCode: 'SKU-001-128', productName: 'iPhone 15 Pro 128G', type: 'inbound', quantity: 100, beforeStock: 0, afterStock: 100, reason: '采购入库', operator: '管理员', createdAt: '2026-06-01 10:00' },
  { id: '2', skuId: '11', skuCode: 'SKU-001-128', productName: 'iPhone 15 Pro 128G', type: 'outbound', quantity: 30, beforeStock: 100, afterStock: 70, reason: '销售出库', operator: '张三', createdAt: '2026-06-05 14:30' },
  { id: '3', skuId: '11', skuCode: 'SKU-001-128', productName: 'iPhone 15 Pro 128G', type: 'adjust', quantity: -20, beforeStock: 70, afterStock: 50, reason: '盘点调整', operator: '管理员', createdAt: '2026-06-08 09:00' },
]

const mockAlerts: StockAlert[] = [
  { id: '2', skuId: '12', skuCode: 'SKU-001-256', productName: 'iPhone 15 Pro 256G', currentStock: 12, threshold: 20, status: 'low', warehouseName: '主仓库' },
  { id: '3', skuId: '21', skuCode: 'SKU-002-256', productName: 'MacBook Air M3 256G', currentStock: 5, threshold: 15, status: 'critical', warehouseName: '主仓库' },
  { id: '4', skuId: '31', skuCode: 'SKU-003', productName: 'AirPods Pro 2', currentStock: 0, threshold: 50, status: 'out', warehouseName: '主仓库' },
]

export const inventoryService = {
  list: async (params?: InventorySearchParams) => {
    let filtered = [...mockInventory]
    if (params?.keyword) filtered = filtered.filter(i => i.productName.includes(params.keyword!) || i.skuCode.includes(params.keyword!))
    if (params?.status) filtered = filtered.filter(i => i.status === params.status)
    if (params?.lowStock) filtered = filtered.filter(i => i.status !== 'normal')
    const start = ((params?.current || 1) - 1) * (params?.pageSize || 20)
    return { records: filtered.slice(start, start + (params?.pageSize || 20)), total: filtered.length, current: params?.current || 1, pageSize: params?.pageSize || 20 }
  },
  getAlerts: async (): Promise<StockAlert[]> => mockAlerts,
  getMovements: async (skuId?: string): Promise<StockMovement[]> => skuId ? mockMovements.filter(m => m.skuId === skuId) : mockMovements,
  adjustStock: async (skuId: string, quantity: number, reason: string): Promise<void> => {},
}

export interface InventoryItem {
  id: string
  skuId: string
  skuCode: string
  productName: string
  warehouseId: string
  warehouseName: string
  totalStock: number
  availableStock: number
  reservedStock: number
  threshold: number
  status: 'normal' | 'low' | 'critical' | 'out'
  lastUpdatedAt: string
}

export interface StockMovement {
  id: string
  skuId: string
  skuCode: string
  productName: string
  type: 'inbound' | 'outbound' | 'adjust' | 'transfer'
  quantity: number
  beforeStock: number
  afterStock: number
  reason: string
  operator: string
  createdAt: string
}

export interface StockAlert {
  id: string
  skuId: string
  skuCode: string
  productName: string
  currentStock: number
  threshold: number
  status: 'low' | 'critical' | 'out'
  warehouseName: string
}

export interface InventorySearchParams {
  current?: number
  pageSize?: number
  keyword?: string
  warehouseId?: string
  status?: string
  lowStock?: boolean
}

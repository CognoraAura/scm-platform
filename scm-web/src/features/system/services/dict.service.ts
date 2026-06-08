import type { DictType, DictItem } from '../types'

const mockDictTypes: DictType[] = [
  { id: '1', name: '订单状态', code: 'order_status', description: '订单状态字典', status: 'active', itemCount: 5 },
  { id: '2', name: '商品状态', code: 'product_status', description: '商品上下架状态', status: 'active', itemCount: 3 },
]

const mockDictItems: DictItem[] = [
  { id: '11', dictTypeId: '1', label: '待付款', value: 'PENDING_PAYMENT', sort: 1, status: 'active' },
  { id: '12', dictTypeId: '1', label: '已付款', value: 'PAID', sort: 2, status: 'active' },
  { id: '13', dictTypeId: '1', label: '已发货', value: 'SHIPPED', sort: 3, status: 'active' },
  { id: '14', dictTypeId: '1', label: '已完成', value: 'COMPLETED', sort: 4, status: 'active' },
  { id: '15', dictTypeId: '1', label: '已取消', value: 'CANCELLED', sort: 5, status: 'active' },
  { id: '21', dictTypeId: '2', label: '在售', value: 'ON_SALE', sort: 1, status: 'active' },
  { id: '22', dictTypeId: '2', label: '下架', value: 'OFF_SALE', sort: 2, status: 'active' },
]

export const dictService = {
  listTypes: async (): Promise<DictType[]> => mockDictTypes,
  getTypeById: async (id: string): Promise<DictType> => {
    const t = mockDictTypes.find(d => d.id === id)
    if (!t) throw new Error('DictType not found')
    return t
  },
  createType: async (data: Partial<DictType>): Promise<DictType> => ({ ...data, id: String(Date.now()), itemCount: 0, status: 'active' } as DictType),
  updateType: async (id: string, data: Partial<DictType>): Promise<DictType> => ({ ...data, id } as DictType),
  deleteType: async (id: string): Promise<void> => {},
  listItems: async (dictTypeId: string): Promise<DictItem[]> => mockDictItems.filter(i => i.dictTypeId === dictTypeId),
  createItem: async (data: Partial<DictItem>): Promise<DictItem> => ({ ...data, id: String(Date.now()), status: 'active' } as DictItem),
  updateItem: async (id: string, data: Partial<DictItem>): Promise<DictItem> => ({ ...data, id } as DictItem),
  deleteItem: async (id: string): Promise<void> => {},
}

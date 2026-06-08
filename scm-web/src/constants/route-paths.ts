export const ROUTE_PATHS = {
  LOGIN: '/login',
  DASHBOARD: '/dashboard',
  PRODUCT: {
    BASE: '/product',
    CREATE: '/product/create',
    BY_ID: (id: string) => `/product/${id}`,
    EDIT: (id: string) => `/product/${id}/edit`,
  },
  ORDER: {
    BASE: '/order',
    CREATE: '/order/create',
    BY_ID: (id: string) => `/order/${id}`,
  },
  INVENTORY: {
    BASE: '/inventory',
    ALERTS: '/inventory/alerts',
  },
  WAREHOUSE: {
    BASE: '/warehouse',
    INBOUND: '/warehouse/inbound',
    OUTBOUND: '/warehouse/outbound',
  },
  PURCHASE: {
    BASE: '/purchase',
    RFQ: '/purchase/rfq',
    QUOTATION: '/purchase/quotation',
  },
  SUPPLIER: {
    BASE: '/supplier',
  },
  LOGISTICS: {
    BASE: '/logistics',
    TRACKING: '/logistics/tracking',
    CARRIER: '/logistics/carrier',
  },
  FINANCE: {
    BASE: '/finance',
    SETTLEMENT: '/finance/settlement',
    INVOICE: '/finance/invoice',
  },
  SYSTEM: {
    USER: '/system/user',
    ROLE: '/system/role',
    PERMISSION: '/system/permission',
    DEPT: '/system/dept',
    DICTIONARY: '/system/dictionary',
  },
  SETTINGS: {
    PROFILE: '/settings/profile',
    SECURITY: '/settings/security',
    PREFERENCES: '/settings/preferences',
  },
} as const

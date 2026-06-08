export const API_PATHS = {
  AUTH: {
    LOGIN: '/api/auth/login',
    LOGOUT: '/api/auth/logout',
    REFRESH: '/api/auth/refresh',
    ME: '/api/auth/me',
    MFA_VERIFY: '/api/auth/mfa/verify',
  },
  USER: {
    BASE: '/api/users',
    BY_ID: (id: string) => `/api/users/${id}`,
  },
  ORDER: {
    BASE: '/api/orders',
    BY_ID: (orderNo: string) => `/api/orders/${orderNo}`,
    STATUS: (orderNo: string) => `/api/orders/${orderNo}/status`,
  },
  PRODUCT: {
    BASE: '/api/products',
    BY_ID: (id: string) => `/api/products/${id}`,
    SEARCH: '/api/products/search',
  },
  INVENTORY: {
    BASE: '/api/inventory',
    BY_SKU: (skuId: string) => `/api/inventory/${skuId}`,
  },
} as const

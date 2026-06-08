export const PERMISSIONS = {
  DASHBOARD: {
    VIEW: 'dashboard:view',
  },
  PRODUCT: {
    VIEW: 'product:view',
    CREATE: 'product:create',
    EDIT: 'product:edit',
    DELETE: 'product:delete',
    EXPORT: 'product:export',
    IMPORT: 'product:import',
  },
  ORDER: {
    VIEW: 'order:view',
    CREATE: 'order:create',
    EDIT: 'order:edit',
    DELETE: 'order:delete',
    CANCEL: 'order:cancel',
    REFUND: 'order:refund',
  },
  INVENTORY: {
    VIEW: 'inventory:view',
    ADJUST: 'inventory:adjust',
    EXPORT: 'inventory:export',
  },
  SYSTEM: {
    USER_VIEW: 'system:user:view',
    USER_CREATE: 'system:user:create',
    USER_EDIT: 'system:user:edit',
    USER_DELETE: 'system:user:delete',
    ROLE_VIEW: 'system:role:view',
    ROLE_CREATE: 'system:role:create',
    ROLE_EDIT: 'system:role:edit',
    ROLE_DELETE: 'system:role:delete',
  },
} as const

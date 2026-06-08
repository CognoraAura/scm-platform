export const STORAGE_KEYS = {
  AUTH: 'scm-auth',
  UI: 'scm-ui',
  TENANT: 'scm-tenant',
  PREFERENCE: 'scm-preference',
  TABLE_COLUMNS: (key: string) => `scm-table-columns-${key}`,
} as const

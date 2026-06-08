import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface Tenant {
  id: string
  name: string
  code: string
}

interface TenantState {
  currentTenant: Tenant | null
  tenantList: Tenant[]
  tenantLoading: boolean
  setTenant: (tenant: Tenant) => void
  setTenantList: (list: Tenant[]) => void
  setTenantLoading: (loading: boolean) => void
  clearTenant: () => void
}

export const useTenantStore = create<TenantState>()(
  persist(
    (set) => ({
      currentTenant: null,
      tenantList: [],
      tenantLoading: false,
      setTenant: (tenant) => set({ currentTenant: tenant }),
      setTenantList: (list) => set({ tenantList: list }),
      setTenantLoading: (loading) => set({ tenantLoading: loading }),
      clearTenant: () =>
        set({ currentTenant: null, tenantList: [], tenantLoading: false }),
    }),
    {
      name: 'scm-tenant',
      partialize: (state) => ({ currentTenant: state.currentTenant }),
    }
  )
)

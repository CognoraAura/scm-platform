'use client'

import { ReactNode, useEffect } from 'react'
import { useAuthStore } from '@/stores/useAuthStore'
import { authApi } from '@/lib/api/endpoints'
import { useTenantStore } from '@/stores/tenant-store'

interface AuthProviderProps {
  children: ReactNode
}

export default function AuthProvider({ children }: AuthProviderProps) {
  const { accessToken, setUser, setPermissions, setLoading, logout } =
    useAuthStore()
  const { setTenant, setTenantList } = useTenantStore()

  useEffect(() => {
    const initAuth = async () => {
      if (!accessToken) {
        setLoading(false)
        return
      }

      try {
        const userResponse = (await authApi.me()) as {
          data: {
            id: string
            username: string
            displayName: string
            email?: string
            avatar?: string
            roleNames?: string[]
            permissions?: string[]
            currentTenant?: { id: string; name: string; code: string }
            tenants?: Array<{ id: string; name: string; code: string }>
          }
        }

        const userData = userResponse.data

        setUser({
          id: userData.id,
          username: userData.username,
          displayName: userData.displayName,
          email: userData.email,
          avatar: userData.avatar,
          roles: userData.roleNames,
        })

        if (userData.permissions) {
          setPermissions(userData.permissions)
        }

        if (userData.currentTenant) {
          setTenant(userData.currentTenant)
        }

        if (userData.tenants) {
          setTenantList(userData.tenants)
        }
      } catch {
        logout()
      } finally {
        setLoading(false)
      }
    }

    initAuth()
  }, [accessToken, setUser, setPermissions, setLoading, logout, setTenant, setTenantList])

  return <>{children}</>
}

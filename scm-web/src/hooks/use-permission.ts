'use client'

import { useAuthStore } from '@/stores/useAuthStore'

export function usePermission() {
  const { hasPermission, permissions } = useAuthStore()

  const checkPermission = (code: string | string[]) => {
    if (Array.isArray(code)) {
      return code.some((c) => hasPermission(c))
    }
    return hasPermission(code)
  }

  const checkAllPermissions = (codes: string[]) => {
    return codes.every((c) => hasPermission(c))
  }

  return {
    hasPermission: checkPermission,
    hasAllPermissions: checkAllPermissions,
    permissions,
  }
}

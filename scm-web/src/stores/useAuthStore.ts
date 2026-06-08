import { create } from 'zustand'

interface User {
  id: string
  username: string
  displayName: string
  email?: string
  avatar?: string
  roles?: string[]
}

interface AuthState {
  user: User | null
  accessToken: string | null
  refreshToken: string | null
  permissions: string[]
  isAuthenticated: boolean
  login: (user: User, accessToken: string, refreshToken: string) => void
  logout: () => void
  setUser: (user: User) => void
  setPermissions: (permissions: string[]) => void
  hasPermission: (code: string) => boolean
}

export const useAuthStore = create<AuthState>()((set, get) => ({
  user: null,
  accessToken: null,
  refreshToken: null,
  permissions: [],
  isAuthenticated: false,
  login: (user, accessToken, refreshToken) => {
    set({ user, accessToken, refreshToken, isAuthenticated: true })
  },
  logout: () => {
    set({
      user: null,
      accessToken: null,
      refreshToken: null,
      permissions: [],
      isAuthenticated: false,
    })
  },
  setUser: (user) => set({ user }),
  setPermissions: (permissions) => set({ permissions }),
  hasPermission: (code) => {
    const { permissions } = get()
    return permissions.includes(code) || permissions.includes('*')
  },
}))

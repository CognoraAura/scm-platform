import { create } from 'zustand'

interface User {
  id: string
  username: string
  displayName: string
  email?: string
  avatar?: string
  roles?: string[]
}

function setCookie(name: string, value: string, days = 7) {
  const expires = new Date(Date.now() + days * 864e5).toUTCString()
  document.cookie = `${name}=${encodeURIComponent(value)}; expires=${expires}; path=/; SameSite=Lax`
}

function deleteCookie(name: string) {
  document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/`
}

interface AuthState {
  user: User | null
  accessToken: string | null
  refreshToken: string | null
  permissions: string[]
  isAuthenticated: boolean
  isLoading: boolean
  login: (user: User, accessToken: string, refreshToken: string) => void
  logout: () => void
  setUser: (user: User) => void
  setPermissions: (permissions: string[]) => void
  setLoading: (loading: boolean) => void
  hasPermission: (code: string) => boolean
  initialize: () => Promise<void>
}

export const useAuthStore = create<AuthState>()((set, get) => ({
  user: null,
  accessToken: null,
  refreshToken: null,
  permissions: [],
  isAuthenticated: false,
  isLoading: true,

  login: (user, accessToken, refreshToken) => {
    set({ user, accessToken, refreshToken, isAuthenticated: true, isLoading: false })
    setCookie('access_token', accessToken)
  },

  logout: () => {
    set({
      user: null,
      accessToken: null,
      refreshToken: null,
      permissions: [],
      isAuthenticated: false,
      isLoading: false,
    })
    deleteCookie('access_token')
  },

  setUser: (user) => set({ user }),
  setPermissions: (permissions) => set({ permissions }),
  setLoading: (loading) => set({ isLoading: loading }),

  hasPermission: (code) => {
    const { permissions } = get()
    return permissions.includes(code) || permissions.includes('*')
  },

  initialize: async () => {
    const { accessToken } = get()
    if (!accessToken) {
      set({ isLoading: false })
      return
    }
  },
}))

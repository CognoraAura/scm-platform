import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface PreferenceState {
  locale: string
  tablePageSize: number
  dateFormat: string
  setLocale: (locale: string) => void
  setTablePageSize: (size: number) => void
  setDateFormat: (format: string) => void
}

export const usePreferenceStore = create<PreferenceState>()(
  persist(
    (set) => ({
      locale: 'zh-CN',
      tablePageSize: 20,
      dateFormat: 'YYYY-MM-DD HH:mm:ss',
      setLocale: (locale) => set({ locale }),
      setTablePageSize: (size) => set({ tablePageSize: size }),
      setDateFormat: (format) => set({ dateFormat: format }),
    }),
    { name: 'scm-preference' }
  )
)

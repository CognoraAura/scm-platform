export interface Notification {
  id: string
  title: string
  content: string
  type: 'system' | 'order' | 'inventory' | 'approval' | 'security'
  typeLabel: string
  isRead: boolean
  link?: string
  createdAt: string
}

export interface NotificationSearchParams {
  current?: number
  pageSize?: number
  type?: string
  isRead?: boolean
}

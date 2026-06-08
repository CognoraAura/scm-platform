'use client'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { notificationService } from '../services/notification.service'
import type { NotificationSearchParams } from '../types'

export function useNotificationList(params?: NotificationSearchParams) {
  return useQuery({ queryKey: ['notifications', 'list', params], queryFn: () => notificationService.list(params) })
}
export function useUnreadCount() {
  return useQuery({ queryKey: ['notifications', 'unread'], queryFn: () => notificationService.getUnreadCount(), refetchInterval: 60000 })
}
export function useMarkAsRead() {
  const qc = useQueryClient()
  return useMutation({ mutationFn: (id: string) => notificationService.markAsRead(id), onSuccess: () => { qc.invalidateQueries({ queryKey: ['notifications'] }) } })
}
export function useMarkAllAsRead() {
  const qc = useQueryClient()
  return useMutation({ mutationFn: () => notificationService.markAllAsRead(), onSuccess: () => { qc.invalidateQueries({ queryKey: ['notifications'] }); message.success('已全部标为已读') } })
}

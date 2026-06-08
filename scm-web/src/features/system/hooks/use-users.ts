'use client'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { userService } from '../services/user.service'
import type { PageParams } from '../types'

export function useUserList(params?: PageParams) {
  return useQuery({ queryKey: ['users', 'list', params], queryFn: () => userService.list(params) })
}
export function useUserDetail(id: string) {
  return useQuery({ queryKey: ['users', 'detail', id], queryFn: () => userService.getById(id), enabled: !!id })
}
export function useCreateUser() {
  const qc = useQueryClient()
  return useMutation({ mutationFn: (data: Parameters<typeof userService.create>[0]) => userService.create(data), onSuccess: () => { qc.invalidateQueries({ queryKey: ['users'] }); message.success('创建成功') } })
}
export function useUpdateUser() {
  const qc = useQueryClient()
  return useMutation({ mutationFn: ({ id, data }: { id: string; data: Parameters<typeof userService.update>[1] }) => userService.update(id, data), onSuccess: () => { qc.invalidateQueries({ queryKey: ['users'] }); message.success('更新成功') } })
}
export function useDeleteUser() {
  const qc = useQueryClient()
  return useMutation({ mutationFn: (id: string) => userService.delete(id), onSuccess: () => { qc.invalidateQueries({ queryKey: ['users'] }); message.success('删除成功') } })
}

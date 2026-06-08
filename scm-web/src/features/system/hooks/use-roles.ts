'use client'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { roleService } from '../services/role.service'

export function useRoleList() {
  return useQuery({ queryKey: ['roles'], queryFn: () => roleService.list() })
}
export function useRoleDetail(id: string) {
  return useQuery({ queryKey: ['roles', 'detail', id], queryFn: () => roleService.getById(id), enabled: !!id })
}
export function useCreateRole() {
  const qc = useQueryClient()
  return useMutation({ mutationFn: (data: Parameters<typeof roleService.create>[0]) => roleService.create(data), onSuccess: () => { qc.invalidateQueries({ queryKey: ['roles'] }); message.success('创建成功') } })
}
export function useUpdateRole() {
  const qc = useQueryClient()
  return useMutation({ mutationFn: ({ id, data }: { id: string; data: Parameters<typeof roleService.update>[1] }) => roleService.update(id, data), onSuccess: () => { qc.invalidateQueries({ queryKey: ['roles'] }); message.success('更新成功') } })
}
export function useDeleteRole() {
  const qc = useQueryClient()
  return useMutation({ mutationFn: (id: string) => roleService.delete(id), onSuccess: () => { qc.invalidateQueries({ queryKey: ['roles'] }); message.success('删除成功') } })
}

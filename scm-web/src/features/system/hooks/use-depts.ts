'use client'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { deptService } from '../services/dept.service'

export function useDeptList() {
  return useQuery({ queryKey: ['depts'], queryFn: () => deptService.list() })
}
export function useCreateDept() {
  const qc = useQueryClient()
  return useMutation({ mutationFn: (data: Parameters<typeof deptService.create>[0]) => deptService.create(data), onSuccess: () => { qc.invalidateQueries({ queryKey: ['depts'] }); message.success('创建成功') } })
}
export function useDeleteDept() {
  const qc = useQueryClient()
  return useMutation({ mutationFn: (id: string) => deptService.delete(id), onSuccess: () => { qc.invalidateQueries({ queryKey: ['depts'] }); message.success('删除成功') } })
}

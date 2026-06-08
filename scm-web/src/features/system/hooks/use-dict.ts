'use client'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { dictService } from '../services/dict.service'

export function useDictTypeList() {
  return useQuery({ queryKey: ['dict', 'types'], queryFn: () => dictService.listTypes() })
}
export function useDictItems(dictTypeId: string) {
  return useQuery({ queryKey: ['dict', 'items', dictTypeId], queryFn: () => dictService.listItems(dictTypeId), enabled: !!dictTypeId })
}
export function useCreateDictType() {
  const qc = useQueryClient()
  return useMutation({ mutationFn: (data: Parameters<typeof dictService.createType>[0]) => dictService.createType(data), onSuccess: () => { qc.invalidateQueries({ queryKey: ['dict', 'types'] }); message.success('创建成功') } })
}
export function useDeleteDictType() {
  const qc = useQueryClient()
  return useMutation({ mutationFn: (id: string) => dictService.deleteType(id), onSuccess: () => { qc.invalidateQueries({ queryKey: ['dict'] }); message.success('删除成功') } })
}
export function useCreateDictItem() {
  const qc = useQueryClient()
  return useMutation({ mutationFn: (data: Parameters<typeof dictService.createItem>[0]) => dictService.createItem(data), onSuccess: () => { qc.invalidateQueries({ queryKey: ['dict', 'items'] }); message.success('创建成功') } })
}

'use client'
import { Tree, Card, Button } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { useCategoryList } from '../hooks'
import type { Category } from '../types'

function buildTreeData(categories: Category[]): any[] {
  return categories.map(c => ({
    key: c.id,
    title: c.name,
    children: c.children ? buildTreeData(c.children) : undefined,
  }))
}

export default function CategoryTree() {
  const { data: categories, isLoading } = useCategoryList()
  return (
    <Card title="商品分类" extra={<Button type="primary" size="small" icon={<PlusOutlined />}>新建分类</Button>} loading={isLoading}>
      <Tree treeData={buildTreeData(categories || [])} defaultExpandAll showLine />
    </Card>
  )
}

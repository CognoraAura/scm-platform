'use client'

import dynamic from 'next/dynamic'
import { Card } from 'antd'
import type { SalesTrendData } from '../types'

const ReactECharts = dynamic(() => import('echarts-for-react'), { ssr: false })

interface SalesChartProps {
  data: SalesTrendData
  loading?: boolean
}

export default function SalesChart({ data, loading }: SalesChartProps) {
  const option = {
    tooltip: { trigger: 'axis' as const },
    legend: { data: data.series.map((s) => s.name), bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '10%', containLabel: true },
    xAxis: { type: 'category' as const, boundaryGap: false, data: data.dates },
    yAxis: { type: 'value' as const },
    series: data.series.map((s) => ({
      name: s.name,
      type: 'line',
      smooth: true,
      data: s.data,
      itemStyle: { color: s.color },
      areaStyle: {
        color: {
          type: 'linear' as const, x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: `${s.color}33` },
            { offset: 1, color: `${s.color}05` },
          ],
        },
      },
    })),
  }

  return (
    <Card title="销售趋势" loading={loading}>
      <ReactECharts option={option} style={{ height: 300 }} />
    </Card>
  )
}

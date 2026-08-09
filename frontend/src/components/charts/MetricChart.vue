<template>
  <div ref="chartRef" class="metric-chart" />
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { ECharts } from 'echarts/core'
import { metricName } from '@/shared/i18n/display'
import { CHART_PALETTE } from '@/shared/chartTheme'

echarts.use([BarChart, GridComponent, TooltipComponent, CanvasRenderer])

const props = defineProps<{ metrics: Record<string, number> }>()
const chartRef = ref<HTMLDivElement>()
let chart: ECharts | null = null

onMounted(() => {
  if (chartRef.value) {
    chart = echarts.init(chartRef.value)
    render()
    window.addEventListener('resize', resize)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  chart?.dispose()
})

watch(() => props.metrics, render, { deep: true })

function render() {
  if (!chart) {
    return
  }
  const entries = Object.entries(props.metrics ?? {})
  chart.setOption({
    color: [...CHART_PALETTE],
    tooltip: {
      trigger: 'axis',
      formatter: (params: unknown) => {
        const item = Array.isArray(params) ? params[0] as { name: string; value: number } : null
        return item ? `${metricName(item.name)}：${item.value}` : ''
      }
    },
    grid: { left: 48, right: 16, top: 18, bottom: 76 },
    xAxis: {
      type: 'category',
      data: entries.map(([key]) => key),
      axisLabel: {
        interval: 0,
        rotate: 30,
        fontSize: 11,
        formatter: (value: string) => metricName(value)
      }
    },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: entries.map(([, value]) => value), itemStyle: { color: CHART_PALETTE[0] } }]
  })
}

function resize() {
  chart?.resize()
}
</script>
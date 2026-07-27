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
    tooltip: { trigger: 'axis' },
    grid: { left: 48, right: 16, top: 18, bottom: 76 },
    xAxis: {
      type: 'category',
      data: entries.map(([key]) => key),
      axisLabel: { interval: 0, rotate: 36, fontSize: 11 }
    },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: entries.map(([, value]) => value), itemStyle: { color: '#245c73' } }]
  })
}

function resize() {
  chart?.resize()
}
</script>
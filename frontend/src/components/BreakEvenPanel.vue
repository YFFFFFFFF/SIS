<template>
  <div class="breakeven-panel">
    <div class="toolbar">
      <el-button type="primary" :loading="loading" :disabled="!scenarioId" @click="run">运行盈亏平衡分析</el-button>
      <span v-if="result?.solvable" class="hint">达产年（负荷 100%）税前口径 · 交点即盈亏平衡点</span>
    </div>

    <template v-if="result">
      <el-alert v-if="!result.solvable" type="error" :closable="false" show-icon :title="result.unsolvableReason ?? '当前参数下无法计算盈亏平衡点'" />
      <template v-else>
        <div class="stat-cards">
          <div class="stat">
            <div class="stat-label">BEP 产量</div>
            <div class="stat-value">{{ fmt(result.bepOutput) }}</div>
            <div class="stat-sub">达产产量 {{ fmt(result.annualOutput) }}</div>
          </div>
          <div class="stat">
            <div class="stat-label">BEP 产能利用率</div>
            <div class="stat-value">{{ pct(result.bepUtilization) }}</div>
            <div class="stat-sub">越低抗风险能力越强</div>
          </div>
          <div class="stat">
            <div class="stat-label">盈亏平衡售价</div>
            <div class="stat-value">{{ fmt(result.bepPrice) }}</div>
            <div class="stat-sub">当前售价 {{ fmt(result.pricePerUnit) }}</div>
          </div>
          <div class="stat">
            <div class="stat-label">单位边际贡献</div>
            <div class="stat-value">{{ fmt(result.contributionMargin) }}</div>
            <div class="stat-sub">售价 − 单位可变成本</div>
          </div>
          <div class="stat">
            <div class="stat-label">年固定成本</div>
            <div class="stat-value">{{ fmt(result.annualFixedCost) }}</div>
            <div class="stat-sub">固定经营成本 + 折旧摊销</div>
          </div>
        </div>
        <div ref="chartRef" class="chart" />
      </template>
      <el-alert v-if="result.assumptionNote" type="info" :closable="false" show-icon title="适用边界与假设" :description="result.assumptionNote" class="note" />
    </template>
    <el-empty v-else description="基于达产年成本性态分解，计算 BEP 产量 / 产能利用率 / 盈亏平衡售价" :image-size="90" />
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, MarkLineComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { ECharts } from 'echarts/core'
import { apiGet } from '@/shared/api/http'
import type { BreakEvenResult } from '@/shared/types/domain'
import { CHART_NEGATIVE, CHART_POSITIVE } from '@/shared/chartTheme'

echarts.use([LineChart, GridComponent, LegendComponent, MarkLineComponent, TooltipComponent, CanvasRenderer])

const props = defineProps<{ scenarioId: number | null }>()

const loading = ref(false)
const result = ref<BreakEvenResult | null>(null)
const chartRef = ref<HTMLDivElement>()
let chart: ECharts | null = null

async function run() {
  if (!props.scenarioId) return
  loading.value = true
  try {
    result.value = await apiGet<BreakEvenResult>(`/scenarios/${props.scenarioId}/break-even`)
    await nextTick()
    renderChart()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '盈亏平衡分析失败')
  } finally {
    loading.value = false
  }
}

function renderChart() {
  if (!chartRef.value || !result.value?.solvable) return
  chart = chart ?? echarts.init(chartRef.value)
  const curve = result.value.curve ?? []
  const bep = result.value.bepOutput
  chart.setOption({
    title: { text: '盈亏平衡图', left: 'center', textStyle: { fontSize: 13 } },
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0 },
    grid: { left: 70, right: 30, top: 40, bottom: 50 },
    xAxis: { type: 'value', name: '产量' },
    yAxis: { type: 'value', name: '金额' },
    series: [
      {
        name: '营业收入',
        type: 'line',
        showSymbol: false,
        itemStyle: { color: CHART_POSITIVE },
        data: curve.map(p => [p.output, p.revenue]),
        markLine: bep != null ? {
          symbol: 'none',
          lineStyle: { type: 'dashed', color: CHART_NEGATIVE },
          label: { formatter: `BEP 产量 ${fmt(bep)}`, fontSize: 10 },
          data: [{ xAxis: bep }]
        } : undefined
      },
      {
        name: '总成本',
        type: 'line',
        showSymbol: false,
        itemStyle: { color: CHART_NEGATIVE },
        data: curve.map(p => [p.output, p.totalCost])
      }
    ]
  }, true)
  chart.resize()
}

function fmt(v?: number | null) {
  if (v == null) return '—'
  return v.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}
function pct(v?: number | null) {
  if (v == null) return '—'
  return (v * 100).toFixed(1) + '%'
}

watch(() => props.scenarioId, () => { result.value = null })

onBeforeUnmount(() => { chart?.dispose() })
</script>

<style scoped>
.breakeven-panel { display: flex; flex-direction: column; gap: 12px; }
.toolbar { display: flex; align-items: center; gap: 12px; }
.hint { font-size: 12px; color: #6b7280; }
.stat-cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(170px, 1fr)); gap: 10px; }
.stat { border: 1px solid #e5e7eb; border-radius: 8px; padding: 10px 12px; background: #fafafa; }
.stat-label { font-size: 12px; color: #6b7280; margin-bottom: 4px; }
.stat-value { font-size: 18px; font-weight: 600; font-variant-numeric: tabular-nums; }
.stat-sub { font-size: 11px; color: #9ca3af; margin-top: 2px; }
.chart { height: 340px; }
.note :deep(.el-alert__description) { font-size: 12px; line-height: 1.7; white-space: pre-wrap; }
</style>

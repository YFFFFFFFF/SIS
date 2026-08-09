<template>
  <div class="montecarlo-panel">
    <el-form label-position="top" class="dense-form">
      <div class="config-row">
        <el-form-item label="目标指标">
          <el-select v-model="form.targetMetric">
            <el-option label="净现值 NPV" value="NPV" />
            <el-option label="内部收益率 IRR" value="IRR" />
          </el-select>
        </el-form-item>
        <el-form-item label="抽样次数">
          <el-select v-model="form.iterations">
            <el-option :value="1000" label="1,000（快速预览）" />
            <el-option :value="10000" label="10,000（推荐）" />
            <el-option :value="50000" label="50,000（精细）" />
          </el-select>
        </el-form-item>
        <el-form-item label="随机种子（可空=随机）">
          <el-input-number v-model="form.seed" :min="0" :max="999999999" controls-position="right" placeholder="留空随机" class="seed-input" />
        </el-form-item>
        <el-form-item label=" ">
          <el-button type="primary" :loading="loading" :disabled="!scenarioId || form.variables.length === 0" @click="run">运行蒙特卡洛</el-button>
        </el-form-item>
      </div>

      <div class="vars-header">
        <span class="vars-title">抽样变量分布</span>
        <el-button size="small" :disabled="form.variables.length >= 4" @click="addVariable">+ 添加变量</el-button>
      </div>
      <div v-for="(v, i) in form.variables" :key="i" class="var-row">
        <el-select v-model="v.variable" class="var-select" placeholder="变量">
          <el-option v-for="opt in availableVariables(i)" :key="opt" :label="monteCarloVariableName(opt)" :value="opt" />
        </el-select>
        <el-select v-model="v.type" class="type-select">
          <el-option label="三角分布" value="TRIANGULAR" />
          <el-option label="正态分布" value="NORMAL" />
        </el-select>
        <template v-if="v.type === 'TRIANGULAR'">
          <el-input-number v-model="v.min" :step="0.05" :precision="2" controls-position="right" placeholder="下限" class="num" />
          <el-input-number v-model="v.mode" :step="0.05" :precision="2" controls-position="right" placeholder="最可能" class="num" />
          <el-input-number v-model="v.max" :step="0.05" :precision="2" controls-position="right" placeholder="上限" class="num" />
        </template>
        <template v-else>
          <el-input-number v-model="v.mean" :step="0.05" :precision="2" controls-position="right" placeholder="均值" class="num" />
          <el-input-number v-model="v.stdDev" :step="0.01" :precision="3" :min="0.001" controls-position="right" placeholder="标准差" class="num" />
        </template>
        <el-button size="small" text type="danger" @click="form.variables.splice(i, 1)">删除</el-button>
      </div>
      <div class="var-hint">数值为比例扰动（如 -0.20 = 基准下浮 20%）；三角分布需满足 下限 ≤ 最可能 ≤ 上限</div>
    </el-form>

    <template v-if="result">
      <div class="stat-cards">
        <div class="stat highlight" :class="result.probPositive >= 0.8 ? 'good' : result.probPositive >= 0.5 ? 'mid' : 'bad'">
          <div class="stat-label">P({{ metricName(result.targetMetric) }} &gt; 0)</div>
          <div class="stat-value">{{ pct(result.probPositive) }}</div>
        </div>
        <div class="stat">
          <div class="stat-label">期望值</div>
          <div class="stat-value">{{ fmt(result.mean) }}</div>
        </div>
        <div class="stat">
          <div class="stat-label">标准差</div>
          <div class="stat-value">{{ fmt(result.stdDev) }}</div>
        </div>
        <div class="stat">
          <div class="stat-label">VaR(95%)</div>
          <div class="stat-value">{{ fmt(result.var95) }}</div>
          <div class="stat-sub">95% 置信不低于该值</div>
        </div>
        <div class="stat">
          <div class="stat-label">P5 / P50 / P95</div>
          <div class="stat-value small">{{ fmt(result.p5) }} / {{ fmt(result.p50) }} / {{ fmt(result.p95) }}</div>
        </div>
        <div class="stat">
          <div class="stat-label">种子 / 次数</div>
          <div class="stat-value small">{{ result.seed }} / {{ result.iterations.toLocaleString() }}</div>
          <div class="stat-sub">同种子可复现</div>
        </div>
      </div>
      <div class="chart-grid">
        <div ref="histogramRef" class="chart" />
        <div ref="cumulativeRef" class="chart" />
      </div>
    </template>
    <el-empty v-else description="配置变量分布后运行，输出期望值 / P(>0) / VaR / 直方图 / 累计概率曲线" :image-size="90" />
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts/core'
import { BarChart, LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { ECharts } from 'echarts/core'
import { apiPost } from '@/shared/api/http'
import type { MonteCarloResultView, MonteCarloVariableSpec } from '@/shared/types/domain'
import { metricName, monteCarloVariableName } from '@/shared/i18n/display'
import { CHART_PALETTE, CHART_POSITIVE } from '@/shared/chartTheme'

echarts.use([BarChart, LineChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const props = defineProps<{ scenarioId: number | null }>()

const allVariables = ['PRICE', 'UNIT_COST', 'INVESTMENT', 'ANNUAL_OUTPUT']
const form = reactive({
  targetMetric: 'NPV',
  iterations: 10000,
  seed: undefined as number | undefined,
  variables: [
    { variable: 'PRICE', type: 'TRIANGULAR', min: -0.2, mode: 0, max: 0.2, mean: 0, stdDev: 0.1 },
    { variable: 'UNIT_COST', type: 'TRIANGULAR', min: -0.1, mode: 0, max: 0.15, mean: 0, stdDev: 0.1 }
  ] as MonteCarloVariableSpec[]
})
const loading = ref(false)
const result = ref<MonteCarloResultView | null>(null)
const histogramRef = ref<HTMLDivElement>()
const cumulativeRef = ref<HTMLDivElement>()
let histogramChart: ECharts | null = null
let cumulativeChart: ECharts | null = null

function availableVariables(index: number) {
  const used = form.variables.map((v, i) => (i === index ? null : v.variable))
  return allVariables.filter(v => !used.includes(v))
}

function addVariable() {
  const remaining = availableVariables(-1)
  if (remaining.length === 0) return
  form.variables.push({ variable: remaining[0], type: 'TRIANGULAR', min: -0.2, mode: 0, max: 0.2, mean: 0, stdDev: 0.1 })
}

async function run() {
  if (!props.scenarioId) return
  loading.value = true
  try {
    result.value = await apiPost<MonteCarloResultView>(`/scenarios/${props.scenarioId}/monte-carlo-runs`, {
      targetMetric: form.targetMetric,
      iterations: form.iterations,
      seed: form.seed ?? null,
      variables: form.variables.map(v => ({
        variable: v.variable,
        type: v.type,
        min: v.type === 'TRIANGULAR' ? v.min : null,
        mode: v.type === 'TRIANGULAR' ? v.mode : null,
        max: v.type === 'TRIANGULAR' ? v.max : null,
        mean: v.type === 'NORMAL' ? v.mean : null,
        stdDev: v.type === 'NORMAL' ? v.stdDev : null
      }))
    })
    await nextTick()
    renderCharts()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '蒙特卡洛分析失败')
  } finally {
    loading.value = false
  }
}

function renderCharts() {
  renderHistogram()
  renderCumulative()
}

function renderHistogram() {
  if (!histogramRef.value || !result.value) return
  histogramChart = histogramChart ?? echarts.init(histogramRef.value)
  const buckets = result.value.histogram ?? []
  histogramChart.setOption({
    color: [...CHART_PALETTE],
    title: { text: `${metricName(result.value.targetMetric)} 概率分布直方图`, left: 'center', textStyle: { fontSize: 13 } },
    tooltip: { formatter: (p: { dataIndex: number }) => {
      const b = buckets[p.dataIndex]
      return `[${fmt(b.from)}, ${fmt(b.to)})<br/>样本数：${b.count}（${pct(b.ratio)}）`
    } },
    grid: { left: 60, right: 20, top: 40, bottom: 50 },
    xAxis: { type: 'category', data: buckets.map(b => fmt(b.from)), axisLabel: { fontSize: 9, rotate: 40 } },
    yAxis: { type: 'value', name: '样本数' },
    series: [{ type: 'bar', barWidth: '85%', itemStyle: { color: CHART_PALETTE[1] }, data: buckets.map(b => b.count) }]
  }, true)
  histogramChart.resize()
}

function renderCumulative() {
  if (!cumulativeRef.value || !result.value) return
  cumulativeChart = cumulativeChart ?? echarts.init(cumulativeRef.value)
  const points = result.value.cumulative ?? []
  cumulativeChart.setOption({
    color: [...CHART_PALETTE],
    title: { text: '累计概率曲线', left: 'center', textStyle: { fontSize: 13 } },
    tooltip: { trigger: 'axis', formatter: (ps: { data: number[] }[]) => {
      const p = ps[0]
      return `${metricName(result.value!.targetMetric)} ≤ ${fmt(p.data[0])}<br/>累计概率：${pct(p.data[1])}`
    } },
    grid: { left: 60, right: 20, top: 40, bottom: 50 },
    xAxis: { type: 'value', name: metricName(result.value.targetMetric) },
    yAxis: { type: 'value', name: 'P(≤x)', max: 1, axisLabel: { formatter: (v: number) => (v * 100).toFixed(0) + '%' } },
    series: [{ type: 'line', showSymbol: true, symbolSize: 4, itemStyle: { color: CHART_POSITIVE },
      data: points.map(p => [p.value, p.probability]) }]
  }, true)
  cumulativeChart.resize()
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

onBeforeUnmount(() => { histogramChart?.dispose(); cumulativeChart?.dispose() })
</script>

<style scoped>
.montecarlo-panel { display: flex; flex-direction: column; gap: 12px; }
.config-row { display: grid; grid-template-columns: repeat(4, minmax(150px, 1fr)); gap: 10px; }
.seed-input { width: 100%; }
.vars-header { display: flex; align-items: center; justify-content: space-between; margin: 4px 0 8px; }
.vars-title { font-size: 13px; font-weight: 600; color: #374151; }
.var-row { display: grid; grid-template-columns: 130px 110px repeat(3, 1fr) 60px; gap: 8px; margin-bottom: 8px; align-items: center; }
.var-row .num { width: 100%; }
.var-hint { font-size: 12px; color: #9ca3af; }
.stat-cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 10px; }
.stat { border: 1px solid #e5e7eb; border-radius: 8px; padding: 10px 12px; background: #fafafa; }
.stat-label { font-size: 12px; color: #6b7280; margin-bottom: 4px; }
.stat-value { font-size: 18px; font-weight: 600; font-variant-numeric: tabular-nums; }
.stat-value.small { font-size: 13px; }
.stat-sub { font-size: 11px; color: #9ca3af; margin-top: 2px; }
.stat.highlight.good { background: #f0fdf4; border-color: #86efac; }
.stat.highlight.mid { background: #fffbeb; border-color: #fcd34d; }
.stat.highlight.bad { background: #fef2f2; border-color: #fca5a5; }
.chart-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.chart { height: 300px; }
</style>

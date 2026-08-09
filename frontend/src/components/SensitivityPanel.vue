<template>
  <div class="sensitivity-panel">
    <el-form label-position="top" class="dense-form sensitivity-form">
      <div class="factor-row">
        <el-form-item label="目标指标">
          <el-select v-model="form.targetMetric"><el-option label="净现值 NPV" value="NPV" /><el-option label="内部收益率 IRR" value="IRR" /></el-select>
        </el-form-item>
        <el-form-item label="因素1">
          <el-select v-model="form.variable1"><el-option v-for="v in variables" :key="v" :label="sensitivityVariableName(v)" :value="v" /></el-select>
        </el-form-item>
        <el-form-item label="波动区间 ±">
          <el-input-number v-model="form.range1" :min="0.05" :max="0.6" :step="0.05" :precision="2" />
        </el-form-item>
        <el-form-item label="步数">
          <el-input-number v-model="form.steps1" :min="3" :max="21" :step="2" />
        </el-form-item>
      </div>
      <div class="factor-row">
        <el-form-item label="因素2（可空）">
          <el-select v-model="form.variable2" clearable placeholder="单因素"><el-option v-for="v in variables" :key="v" :label="sensitivityVariableName(v)" :value="v" /></el-select>
        </el-form-item>
        <el-form-item label="波动区间 ±">
          <el-input-number v-model="form.range2" :min="0.05" :max="0.6" :step="0.05" :precision="2" :disabled="!form.variable2" />
        </el-form-item>
        <el-form-item label="步数">
          <el-input-number v-model="form.steps2" :min="3" :max="21" :step="2" :disabled="!form.variable2" />
        </el-form-item>
        <el-form-item label=" ">
          <el-button type="primary" :loading="loading" :disabled="!scenarioId" @click="run">运行敏感性分析</el-button>
        </el-form-item>
      </div>
    </el-form>

    <template v-if="result">
      <div class="result-grid">
        <div ref="heatmapRef" class="chart heatmap" />
        <div class="side">
          <table class="coef-tbl">
            <thead><tr><th>变量</th><th class="num">敏感系数</th><th class="num">临界值</th><th>等级</th></tr></thead>
            <tbody>
              <tr v-for="row in coefRows" :key="row.variable">
                <td>{{ sensitivityVariableName(row.variable) }}</td>
                <td class="num">{{ row.coefficient ?? '—' }}</td>
                <td class="num">{{ row.critical != null ? (row.critical * 100).toFixed(1) + '%' : '—' }}</td>
                <td><span class="level" :class="row.level?.toLowerCase()">{{ sensitivityLevelName(row.level) }}</span></td>
              </tr>
            </tbody>
          </table>
          <div ref="tornadoRef" class="chart tornado" />
        </div>
      </div>
    </template>
    <el-empty v-else description="配置因素后运行，生成多因素热力图与敏感性结论" :image-size="90" />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts/core'
import { BarChart, HeatmapChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, VisualMapComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { ECharts } from 'echarts/core'
import { apiPost } from '@/shared/api/http'
import type { SensitivityResult } from '@/shared/types/domain'
import { sensitivityLevelName, sensitivityVariableName } from '@/shared/i18n/display'
import { CHART_PALETTE } from '@/shared/chartTheme'

echarts.use([BarChart, HeatmapChart, GridComponent, TooltipComponent, VisualMapComponent, CanvasRenderer])

const props = defineProps<{ scenarioId: number | null }>()

const variables = ['PRICE', 'UNIT_COST', 'INVESTMENT', 'CONSTRUCTION_PERIOD']
const form = reactive({ targetMetric: 'NPV', variable1: 'PRICE', range1: 0.2, steps1: 9, variable2: 'UNIT_COST', range2: 0.2, steps2: 9 })
const loading = ref(false)
const result = ref<SensitivityResult | null>(null)
const heatmapRef = ref<HTMLDivElement>()
const tornadoRef = ref<HTMLDivElement>()
let heatmap: ECharts | null = null
let tornado: ECharts | null = null

const coefRows = computed(() => {
  if (!result.value) return []
  const rows = [{ variable: result.value.variable1, coefficient: result.value.coefficient1, critical: result.value.criticalFactor1, level: result.value.level1 }]
  if (result.value.variable2) rows.push({ variable: result.value.variable2, coefficient: result.value.coefficient2, critical: result.value.criticalFactor2, level: result.value.level2 })
  return rows
})

async function run() {
  if (!props.scenarioId) return
  loading.value = true
  try {
    const payload: Record<string, unknown> = {
      targetMetric: form.targetMetric,
      variable1: form.variable1, range1: form.range1, steps1: form.steps1
    }
    if (form.variable2) {
      payload.variable2 = form.variable2
      payload.range2 = form.range2
      payload.steps2 = form.steps2
    }
    result.value = await apiPost<SensitivityResult>(`/scenarios/${props.scenarioId}/sensitivity`, payload)
    await renderCharts()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '敏感性分析失败')
  } finally {
    loading.value = false
  }
}

async function renderCharts() {
  await Promise.resolve()
  renderHeatmap()
  renderTornado()
}

function uniqueFactors(axis: 1 | 2): number[] {
  const set = new Set<number>()
  for (const c of result.value?.matrix ?? []) {
    const v = axis === 1 ? c.factor1 : c.factor2
    if (v != null) set.add(v)
  }
  return [...set].sort((a, b) => a - b)
}

function pct(v: number) { return (v * 100).toFixed(0) + '%' }

function renderHeatmap() {
  if (!heatmapRef.value || !result.value) return
  heatmap = heatmap ?? echarts.init(heatmapRef.value)
  const x = uniqueFactors(2)   // 因素2 → x 轴
  const y = uniqueFactors(1)   // 因素1 → y 轴
  const single = x.length === 0
  const data = (result.value.matrix ?? []).map(c => {
    const xi = single ? 0 : x.indexOf(c.factor2 ?? 0)
    const yi = y.indexOf(c.factor1)
    return [xi, yi, c.metricValue]
  })
  const values = data.map(d => d[2])
  heatmap.setOption({
    title: { text: single ? '单因素敏感性' : '多因素敏感性热力图', left: 'center', textStyle: { fontSize: 13 } },
    tooltip: { formatter: (p: { data: number[] }) => single
      ? `${sensitivityVariableName(result.value!.variable1)} ${pct(y[p.data[1]])}<br/>${result.value!.targetMetric}：<b>${p.data[2]}</b>`
      : `${sensitivityVariableName(result.value!.variable1)} ${pct(y[p.data[1]])} × ${sensitivityVariableName(result.value!.variable2!)} ${pct(x[p.data[0]])}<br/>${result.value!.targetMetric}：<b>${p.data[2]}</b>` },
    grid: { left: 70, right: 24, top: 40, bottom: 70 },
    xAxis: { type: 'category', name: single ? '' : sensitivityVariableName(result.value.variable2), data: single ? [''] : x.map(pct) },
    yAxis: { type: 'category', name: sensitivityVariableName(result.value.variable1), data: y.map(pct) },
    visualMap: { min: Math.min(...values), max: Math.max(...values), calculable: true, orient: 'horizontal', left: 'center', bottom: 0,
      inRange: { color: ['#dc2626', '#fbbf24', '#f8fafc', '#86efac', '#16a34a'] } },
    series: [{ type: 'heatmap', data, label: { show: true, fontSize: 9 }, itemStyle: { borderColor: '#fff', borderWidth: 1 } }]
  }, true)
  heatmap.resize()
}

function renderTornado() {
  if (!tornadoRef.value || !result.value) return
  tornado = tornado ?? echarts.init(tornadoRef.value)
  const rows = [...coefRows.value].sort((a, b) => (a.coefficient ?? 0) - (b.coefficient ?? 0))
  tornado.setOption({
    color: [...CHART_PALETTE],
    title: { text: '龙卷风图 · 敏感系数排序', left: 'center', textStyle: { fontSize: 12 } },
    tooltip: {},
    grid: { left: 90, right: 30, top: 34, bottom: 24 },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: rows.map(r => sensitivityVariableName(r.variable)) },
    series: [{ type: 'bar', barWidth: 14, itemStyle: { color: CHART_PALETTE[1] }, data: rows.map(r => r.coefficient ?? 0), label: { show: true, position: 'right', fontSize: 10 } }]
  }, true)
  tornado.resize()
}

watch(() => props.scenarioId, () => { result.value = null })

onBeforeUnmount(() => { heatmap?.dispose(); tornado?.dispose() })
</script>

<style scoped>
.sensitivity-panel { display: flex; flex-direction: column; gap: 12px; }
.sensitivity-form .factor-row { display: grid; grid-template-columns: repeat(4, minmax(140px, 1fr)); gap: 10px; }
.result-grid { display: grid; grid-template-columns: 1.5fr 1fr; gap: 16px; }
.chart.heatmap { height: 360px; }
.chart.tornado { height: 180px; margin-top: 12px; }
.side { min-width: 0; }
.coef-tbl { width: 100%; border-collapse: collapse; font-size: 13px; }
.coef-tbl th, .coef-tbl td { border-bottom: 1px solid #e5e7eb; padding: 6px 8px; text-align: left; }
.coef-tbl .num { text-align: right; font-variant-numeric: tabular-nums; }
.level { padding: 1px 8px; border-radius: 10px; font-size: 12px; background: #e5e7eb; }
.level.high { background: #fee2e2; color: #b91c1c; }
.level.medium { background: #fef3c7; color: #b45309; }
.level.low { background: #dcfce7; color: #15803d; }
</style>

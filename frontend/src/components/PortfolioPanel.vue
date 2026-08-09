<template>
  <div class="portfolio-panel">
    <el-form label-position="top" class="dense-form">
      <div class="config-row">
        <el-form-item label="资金池预算">
          <el-input-number v-model="form.budget" :min="1" :step="50000" :precision="0" controls-position="right" class="full" />
        </el-form-item>
        <el-form-item label="数量上限（可空 = 不限）">
          <el-input-number v-model="form.maxCount" :min="1" :max="50" controls-position="right" class="full" placeholder="不限" />
        </el-form-item>
        <el-form-item label=" ">
          <el-button type="primary" :loading="loading" @click="run">运行组合优化</el-button>
        </el-form-item>
      </div>
      <div class="hint">候选池：全部已测算成功的方案（每方案取最新一次测算指标）· 目标：组合 NPV 最大化 · 求解器：oj! Algorithms（0-1 整数规划）</div>
    </el-form>

    <template v-if="result">
      <div class="stat-cards">
        <div class="stat">
          <div class="stat-label">组合 NPV</div>
          <div class="stat-value good">{{ fmt(result.totalNpv) }}</div>
        </div>
        <div class="stat">
          <div class="stat-label">组合总投资</div>
          <div class="stat-value">{{ fmt(result.totalInvestment) }}</div>
          <div class="stat-sub">预算 {{ fmt(result.budget) }} · 利用率 {{ utilization }}</div>
        </div>
        <div class="stat">
          <div class="stat-label">入选 / 候选</div>
          <div class="stat-value">{{ selectedCount }} / {{ result.candidateCount }}</div>
        </div>
      </div>

      <el-alert v-if="result.explanation" type="info" :closable="false" show-icon title="求解解释" :description="result.explanation" class="note" />

      <div class="grid">
        <div>
          <div class="section-title">入选组合（Top-N 按 NPV 降序）</div>
          <el-table :data="result.members" size="small" class="tbl" :row-class-name="rowClass">
            <el-table-column label="排名" width="60" align="center">
              <template #default="{ row }">
                <span v-if="row.rankNo" class="rank">{{ row.rankNo }}</span>
                <span v-else class="muted">—</span>
              </template>
            </el-table-column>
            <el-table-column prop="scenarioName" label="方案" min-width="120" />
            <el-table-column prop="projectName" label="项目" min-width="100" show-overflow-tooltip />
            <el-table-column label="NPV" width="110" align="right">
              <template #default="{ row }"><span class="num">{{ fmt(row.npv) }}</span></template>
            </el-table-column>
            <el-table-column label="总投资" width="110" align="right">
              <template #default="{ row }"><span class="num">{{ fmt(row.investment) }}</span></template>
            </el-table-column>
            <el-table-column label="IRR" width="80" align="right">
              <template #default="{ row }"><span class="num">{{ row.irr != null ? (row.irr * 100).toFixed(1) + '%' : '—' }}</span></template>
            </el-table-column>
            <el-table-column label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.selected ? 'success' : 'info'">{{ row.selected ? '入选' : '未入选' }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div>
          <div class="section-title">帕累托前沿（预算 → 最优组合 NPV）</div>
          <div ref="frontierRef" class="chart" />
        </div>
      </div>
    </template>
    <el-empty v-else description="设定资金池与数量上限后运行，输出 Top-N 入选组合、组合 NPV 与帕累托前沿" :image-size="90" />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { ECharts } from 'echarts/core'
import { apiPost } from '@/shared/api/http'
import type { PortfolioResultView } from '@/shared/types/domain'
import { CHART_PALETTE } from '@/shared/chartTheme'

echarts.use([LineChart, GridComponent, TooltipComponent, CanvasRenderer])

const form = reactive({ budget: 500000, maxCount: undefined as number | undefined })
const loading = ref(false)
const result = ref<PortfolioResultView | null>(null)
const frontierRef = ref<HTMLDivElement>()
let frontierChart: ECharts | null = null

const selectedCount = computed(() => result.value?.members.filter(m => m.selected).length ?? 0)
const utilization = computed(() => {
  if (!result.value || !result.value.budget) return '—'
  return ((result.value.totalInvestment / result.value.budget) * 100).toFixed(1) + '%'
})

async function run() {
  loading.value = true
  try {
    result.value = await apiPost<PortfolioResultView>('/portfolio-runs', {
      budget: form.budget,
      maxCount: form.maxCount ?? null
    })
    await nextTick()
    renderFrontier()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '组合优化失败')
  } finally {
    loading.value = false
  }
}

function renderFrontier() {
  if (!frontierRef.value || !result.value) return
  frontierChart = frontierChart ?? echarts.init(frontierRef.value)
  const points = result.value.frontier ?? []
  frontierChart.setOption({
    color: [...CHART_PALETTE],
    tooltip: { trigger: 'axis', formatter: (ps: { data: number[] }[]) => {
      const p = ps[0]
      return `预算 ${fmt(p.data[0])}<br/>最优组合 NPV：<b>${fmt(p.data[1])}</b>`
    } },
    grid: { left: 80, right: 24, top: 20, bottom: 40 },
    xAxis: { type: 'value', name: '预算', axisLabel: { formatter: (v: number) => (v / 10000) + '万' } },
    yAxis: { type: 'value', name: '组合 NPV', axisLabel: { formatter: (v: number) => (v / 10000) + '万' } },
    series: [{ type: 'line', showSymbol: true, symbolSize: 5, areaStyle: { opacity: 0.12 },
      itemStyle: { color: CHART_PALETTE[7] }, data: points.map(p => [p.budget, p.npv]) }]
  }, true)
  frontierChart.resize()
}

function rowClass({ row }: { row: { selected: boolean } }) {
  return row.selected ? 'row-selected' : ''
}

function fmt(v?: number | null) {
  if (v == null) return '—'
  return v.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

onBeforeUnmount(() => { frontierChart?.dispose() })
</script>

<style scoped>
.portfolio-panel { display: flex; flex-direction: column; gap: 12px; }
.config-row { display: grid; grid-template-columns: repeat(3, minmax(160px, 1fr)); gap: 10px; }
.full { width: 100%; }
.hint { font-size: 12px; color: #9ca3af; }
.stat-cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 10px; }
.stat { border: 1px solid #e5e7eb; border-radius: 8px; padding: 10px 12px; background: #fafafa; }
.stat-label { font-size: 12px; color: #6b7280; margin-bottom: 4px; }
.stat-value { font-size: 18px; font-weight: 600; font-variant-numeric: tabular-nums; }
.stat-value.good { color: #16a34a; }
.stat-sub { font-size: 11px; color: #9ca3af; margin-top: 2px; }
.note :deep(.el-alert__description) { font-size: 12px; line-height: 1.7; white-space: pre-wrap; }
.grid { display: grid; grid-template-columns: 1.2fr 1fr; gap: 16px; }
.section-title { font-size: 13px; font-weight: 600; color: #374151; margin-bottom: 8px; }
.tbl { width: 100%; }
.chart { height: 300px; }
.rank { display: inline-block; min-width: 20px; padding: 0 4px; border-radius: 10px; background: #dcfce7; color: #15803d; font-weight: 600; font-size: 12px; }
.muted { color: #9ca3af; }
.num { font-variant-numeric: tabular-nums; }
:deep(.row-selected) { background: #f0fdf4; }
</style>

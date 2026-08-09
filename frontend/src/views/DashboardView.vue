<template>
  <div class="dashboard">
    <el-row :gutter="14" class="kpi-row">
      <el-col :span="6"><el-card shadow="hover" class="kpi"><div class="lbl">在管项目总数</div><div class="val">{{ summary?.kpis.projectCount ?? '—' }} <small>个</small></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover" class="kpi green"><div class="lbl">组合加权平均 IRR</div><div class="val">{{ irrText }}<small>%</small></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover" class="kpi orange"><div class="lbl">组合总 NPV</div><div class="val">{{ npvText }} <small>万元</small></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover" class="kpi red"><div class="lbl">风险预警</div><div class="val">{{ summary?.kpis.warningCount ?? '—' }} <small>项</small></div></el-card></el-col>
    </el-row>

    <el-row :gutter="14" class="row">
      <el-col :span="15">
        <el-card shadow="never">
          <template #header>投资组合绩效<span class="sub">　NPV — IRR 分布（气泡大小 = 投资额）</span></template>
          <div ref="bubbleRef" class="chart tall" />
        </el-card>
      </el-col>
      <el-col :span="9">
        <el-card shadow="never">
          <template #header>风险信号灯<span class="sub">　IRR 相对 8% 基准（R-12 后替换为阈值监控）</span></template>
          <el-table :data="summary?.riskSignals ?? []" size="small" height="330">
            <el-table-column prop="variable" label="监控对象" min-width="120" show-overflow-tooltip />
            <el-table-column prop="currentValue" label="当前值" width="90" align="right" />
            <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="tagType(row.level)" size="small" effect="dark">{{ row.note }}</el-tag></template></el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="14" class="row">
      <el-col :span="8"><el-card shadow="never"><template #header>项目阶段分布</template><div ref="stageRef" class="chart" /></el-card></el-col>
      <el-col :span="8"><el-card shadow="never"><template #header>行业分布（按投资额）</template><div ref="industryRef" class="chart" /></el-card></el-col>
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>待办与审批<el-button class="todo-more" size="small" text type="primary" @click="goGovernance">全部 ›</el-button></template>
          <el-table :data="summary?.todos ?? []" size="small" height="260">
            <el-table-column label="事项" min-width="160"><template #default="{ row }">{{ row.projectName }} · {{ row.scenarioName }}</template></el-table-column>
            <el-table-column label="节点" width="100"><template #default="{ row }">{{ approvalNodeName(row.currentNode) }}</template></el-table-column>
            <el-table-column prop="updatedAt" label="时间" width="120" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts/core'
import { BarChart, PieChart, ScatterChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { ECharts } from 'echarts/core'
import { apiGet } from '@/shared/api/http'
import type { DashboardSummary } from '@/shared/types/domain'
import { approvalNodeName, projectStatusName } from '@/shared/i18n/display'
import { CHART_PALETTE } from '@/shared/chartTheme'

echarts.use([BarChart, PieChart, ScatterChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const router = useRouter()
const summary = ref<DashboardSummary | null>(null)
const bubbleRef = ref<HTMLDivElement>()
const stageRef = ref<HTMLDivElement>()
const industryRef = ref<HTMLDivElement>()
let bubble: ECharts | null = null
let stage: ECharts | null = null
let industry: ECharts | null = null

const irrText = computed(() => summary.value?.kpis.weightedIrr != null ? (summary.value.kpis.weightedIrr * 100).toFixed(1) : '—')
const npvText = computed(() => summary.value?.kpis.totalNpv != null ? Number(summary.value.kpis.totalNpv).toLocaleString(undefined, { maximumFractionDigits: 0 }) : '—')

onMounted(async () => {
  try {
    summary.value = await apiGet<DashboardSummary>('/dashboard/summary')
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '看板加载失败')
    return
  }
  renderCharts()
})

onBeforeUnmount(() => { bubble?.dispose(); stage?.dispose(); industry?.dispose() })

function renderCharts() {
  const data = summary.value
  if (!data) return
  if (bubbleRef.value) {
    bubble = echarts.init(bubbleRef.value)
    bubble.setOption({
      color: [...CHART_PALETTE],
      tooltip: { formatter: (p: { data: { value: number[]; name: string } }) => `${p.data.name}<br/>NPV：${p.data.value[0].toLocaleString()} 万元<br/>IRR：${(p.data.value[1] * 100).toFixed(2)}%<br/>投资：${p.data.value[2].toLocaleString()} 万元` },
      grid: { left: 70, right: 24, top: 24, bottom: 44 },
      xAxis: { name: 'NPV（万元）', type: 'value', splitLine: { lineStyle: { type: 'dashed' } } },
      yAxis: { name: 'IRR', type: 'value', axisLabel: { formatter: (v: number) => (v * 100).toFixed(0) + '%' } },
      series: [{ type: 'scatter', data: data.bubbles.map((b) => ({ name: `${b.projectName} · ${b.scenarioName}`, value: [b.npv, b.irr, b.investment] })), symbolSize: (val: number[]) => Math.max(14, Math.min(56, Math.sqrt(Math.abs(val[2])) / 3)), itemStyle: { color: CHART_PALETTE[0], opacity: 0.75 } }]
    })
  }
  if (stageRef.value) {
    stage = echarts.init(stageRef.value)
    stage.setOption({
      color: [...CHART_PALETTE],
      tooltip: { trigger: 'item' },
      legend: { bottom: 0, textStyle: { fontSize: 11 } },
      series: [{ type: 'pie', radius: ['40%', '68%'], center: ['50%', '44%'], label: { fontSize: 11 }, data: data.stageCounts.map((s) => ({ name: projectStatusName(s.name), value: s.value })) }]
    })
  }
  if (industryRef.value) {
    industry = echarts.init(industryRef.value)
    industry.setOption({
      color: [...CHART_PALETTE],
      tooltip: { trigger: 'axis', formatter: (params: unknown) => { const it = Array.isArray(params) ? params[0] as { name: string; value: number } : null; return it ? `${industryName(it.name)}：${Number(it.value).toLocaleString()} 万元` : '' } },
      grid: { left: 90, right: 20, top: 16, bottom: 28 },
      xAxis: { type: 'value' },
      yAxis: { type: 'category', data: data.industryAmounts.map((i) => industryName(i.name)), axisLabel: { fontSize: 11 } },
      series: [{ type: 'bar', data: data.industryAmounts.map((i) => i.value), itemStyle: { color: CHART_PALETTE[0] } }]
    })
  }
}

function tagType(level: string) { return level === 'RED' ? 'danger' : level === 'YELLOW' ? 'warning' : 'success' }
function industryName(code: string) { return ({ INDUSTRIAL: '产业项目', INFRASTRUCTURE: '基础设施', TECHNOLOGY: '科技项目', OTHER: '其他' } as Record<string, string>)[code] ?? code }
function goGovernance() { router.push({ name: 'governance' }) }
</script>

<style scoped>
.kpi-row { margin-bottom: 14px; }
.row { margin-bottom: 14px; }
.kpi .lbl { color: #909399; font-size: 13px; margin-bottom: 8px; }
.kpi .val { font-size: 26px; font-weight: 700; color: #303133; }
.kpi .val small { font-size: 13px; font-weight: 400; color: #909399; margin-left: 2px; }
.kpi.green .val { color: #16a34a; }
.kpi.orange .val { color: #d97706; }
.kpi.red .val { color: #dc2626; }
.sub { color: #909399; font-size: 12px; font-weight: 400; }
.chart { height: 260px; }
.chart.tall { height: 330px; }
.todo-more { float: right; }
</style>

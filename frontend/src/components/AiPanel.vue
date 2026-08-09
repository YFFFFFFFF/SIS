<template>
  <div class="ai-panel">
    <div class="toolbar">
      <el-button type="primary" :loading="loadingScore" :disabled="!scenarioId" @click="runScore">智能打分</el-button>
      <el-button :loading="loadingParams" :disabled="!scenarioId" @click="runRecommend">参数推荐</el-button>
      <span class="hint">规则化加权模型 · 因子与权重全量公开 · 不替代人工决策</span>
    </div>

    <template v-if="score">
      <div class="score-row">
        <div class="score-card" :class="score.label.toLowerCase()">
          <div class="score-value">{{ score.totalScore.toFixed(1) }}</div>
          <div class="score-label">{{ labelName(score.label) }}</div>
          <div class="score-model">{{ score.modelCode }} v{{ score.modelVersion }}</div>
        </div>
        <div class="factor-col">
          <div class="section-title">因子明细（可解释）</div>
          <el-table :data="score.factors" size="small" class="tbl">
            <el-table-column prop="name" label="因子" width="150" />
            <el-table-column prop="rawValue" label="原始值" width="130" show-overflow-tooltip />
            <el-table-column label="得分" width="90" align="right">
              <template #default="{ row }"><span class="num">{{ row.score.toFixed(0) }}</span></template>
            </el-table-column>
            <el-table-column label="权重" width="80" align="right">
              <template #default="{ row }"><span class="num">{{ (row.weight * 100).toFixed(0) }}%</span></template>
            </el-table-column>
            <el-table-column label="加权" width="80" align="right">
              <template #default="{ row }"><span class="num strong">{{ row.weighted.toFixed(1) }}</span></template>
            </el-table-column>
            <el-table-column prop="explain" label="打分依据" min-width="280" show-overflow-tooltip />
          </el-table>
        </div>
      </div>
      <el-alert type="warning" :closable="false" show-icon :title="score.disclaimer" class="disclaimer" />
    </template>

    <template v-if="params">
      <div class="section-title">参数推荐（{{ params.basisSummary }}）</div>
      <el-table :data="params.items" size="small" class="tbl">
        <el-table-column label="参数" width="150">
          <template #default="{ row }">{{ paramName(row.param) }}</template>
        </el-table-column>
        <el-table-column label="当前值" width="120" align="right">
          <template #default="{ row }"><span class="num">{{ fmt(row.current) }}</span></template>
        </el-table-column>
        <el-table-column label="建议区间" width="200" align="right">
          <template #default="{ row }"><span class="num">{{ fmt(row.recommendedLow) }} ~ {{ fmt(row.recommendedHigh) }}</span></template>
        </el-table-column>
        <el-table-column prop="basis" label="依据来源" min-width="300" show-overflow-tooltip />
      </el-table>
    </template>

    <el-empty v-if="!score && !params" description="运行智能打分或参数推荐，输出可解释的六因子评分与参数建议区间" :image-size="90" />
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { apiGet } from '@/shared/api/http'
import type { AiParamRecommendation, AiScoreResult } from '@/shared/types/domain'

const props = defineProps<{ scenarioId: number | null }>()

const score = ref<AiScoreResult | null>(null)
const params = ref<AiParamRecommendation | null>(null)
const loadingScore = ref(false)
const loadingParams = ref(false)

async function runScore() {
  if (!props.scenarioId) return
  loadingScore.value = true
  try {
    score.value = await apiGet<AiScoreResult>(`/scenarios/${props.scenarioId}/ai/score`)
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '智能打分失败')
  } finally {
    loadingScore.value = false
  }
}

async function runRecommend() {
  if (!props.scenarioId) return
  loadingParams.value = true
  try {
    params.value = await apiGet<AiParamRecommendation>(`/scenarios/${props.scenarioId}/ai/param-recommendation`)
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '参数推荐失败')
  } finally {
    loadingParams.value = false
  }
}

function labelName(label: string) {
  return { RECOMMEND: '建议立项', CAUTION: '谨慎推进', HOLD: '建议暂缓' }[label] ?? label
}
function paramName(p: string) {
  return { wacc: '基准折现率 WACC', pricePerUnit: '产品售价', unitCost: '单位成本', sensitivityRange: '敏感性区间' }[p] ?? p
}
function fmt(v?: number | null) {
  if (v == null) return '—'
  return v.toLocaleString('zh-CN', { maximumFractionDigits: 4 })
}

watch(() => props.scenarioId, () => { score.value = null; params.value = null })
</script>

<style scoped>
.ai-panel { display: flex; flex-direction: column; gap: 12px; }
.toolbar { display: flex; align-items: center; gap: 12px; }
.hint { font-size: 12px; color: #9ca3af; }
.score-row { display: grid; grid-template-columns: 200px 1fr; gap: 16px; align-items: start; }
.score-card { border-radius: 12px; padding: 20px 16px; text-align: center; border: 2px solid #e5e7eb; }
.score-card.recommend { border-color: #16a34a; background: #f0fdf4; }
.score-card.caution { border-color: #f59e0b; background: #fffbeb; }
.score-card.hold { border-color: #dc2626; background: #fef2f2; }
.score-value { font-size: 36px; font-weight: 700; font-variant-numeric: tabular-nums; }
.score-card.recommend .score-value { color: #16a34a; }
.score-card.caution .score-value { color: #b45309; }
.score-card.hold .score-value { color: #dc2626; }
.score-label { font-size: 15px; font-weight: 600; margin-top: 4px; }
.score-model { font-size: 11px; color: #9ca3af; margin-top: 6px; }
.section-title { font-size: 13px; font-weight: 600; color: #374151; margin-bottom: 8px; }
.tbl { width: 100%; }
.num { font-variant-numeric: tabular-nums; }
.num.strong { font-weight: 600; }
.disclaimer { font-size: 12px; }
</style>

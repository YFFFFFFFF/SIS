<template>
  <div class="reverse-panel">
    <el-form label-position="top" class="dense-form reverse-form">
      <div class="factor-row">
        <el-form-item label="目标指标">
          <el-select v-model="form.targetMetric">
            <el-option label="净现值 NPV" value="NPV" />
            <el-option label="内部收益率 IRR" value="IRR" />
            <el-option label="静态回收期（年）" value="STATIC_PAYBACK_YEARS" />
          </el-select>
        </el-form-item>
        <el-form-item :label="targetValueLabel">
          <el-input-number v-model="form.targetValue" :precision="targetPrecision" :step="targetStep" controls-position="right" class="target-input" />
        </el-form-item>
        <el-form-item label="反算变量">
          <el-select v-model="form.variable">
            <el-option v-for="v in variables" :key="v" :label="reverseVariableName(v)" :value="v" />
          </el-select>
        </el-form-item>
        <el-form-item label=" ">
          <el-button type="primary" :loading="loading" :disabled="!scenarioId" @click="run">目标反算</el-button>
        </el-form-item>
      </div>
    </el-form>

    <template v-if="result">
      <el-alert
        :type="result.feasible ? 'success' : 'error'"
        :closable="false"
        show-icon
        class="result-alert"
      >
        <template #title>
          <span v-if="result.feasible">
            临界{{ reverseVariableName(result.variable) }} ≈ <b>{{ formatNumber(result.solvedValue) }}</b>
            （基准 {{ formatNumber(result.baseValue) }}，变动 {{ formatPercent(result.factor - 1) }}）
          </span>
          <span v-else>在允许区间（基准的 1% ~ 1000%）内无解，目标不可达</span>
        </template>
      </el-alert>
      <div class="result-grid">
        <div class="stat-cards">
          <div class="stat">
            <div class="stat-label">反算临界值</div>
            <div class="stat-value">{{ formatNumber(result.solvedValue) }}</div>
          </div>
          <div class="stat">
            <div class="stat-label">基准值</div>
            <div class="stat-value">{{ formatNumber(result.baseValue) }}</div>
          </div>
          <div class="stat">
            <div class="stat-label">变动幅度</div>
            <div class="stat-value">{{ formatPercent(result.factor - 1) }}</div>
          </div>
          <div class="stat">
            <div class="stat-label">达成指标值（{{ metricName(result.targetMetric) }}）</div>
            <div class="stat-value">{{ formatNumber(result.achievedValue) }}</div>
          </div>
          <div class="stat">
            <div class="stat-label">迭代次数</div>
            <div class="stat-value">{{ result.iterations }}</div>
          </div>
        </div>
        <div class="notes">
          <el-alert v-if="result.sensitivityNote" type="info" :closable="false" show-icon title="敏感性说明" :description="result.sensitivityNote" class="note" />
          <el-alert v-if="result.boundaryNote" type="warning" :closable="false" show-icon title="适用边界与假设" :description="result.boundaryNote" class="note" />
        </div>
      </div>
    </template>
    <el-empty v-else description="设定目标指标与反算变量，求解临界值（如：NPV=0 时的最低售价）" :image-size="90" />
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { apiPost } from '@/shared/api/http'
import type { ReverseResult } from '@/shared/types/domain'
import { metricName, reverseVariableName } from '@/shared/i18n/display'

const props = defineProps<{ scenarioId: number | null }>()

const variables = ['PRICE', 'INVESTMENT', 'ANNUAL_OUTPUT', 'UNIT_COST']
const form = reactive({ targetMetric: 'NPV', targetValue: 0, variable: 'PRICE' })
const loading = ref(false)
const result = ref<ReverseResult | null>(null)

const targetValueLabel = computed(() => {
  if (form.targetMetric === 'IRR') return '目标值（小数，如 0.12）'
  if (form.targetMetric === 'STATIC_PAYBACK_YEARS') return '目标值（年，不超过）'
  return '目标值'
})
const targetPrecision = computed(() => (form.targetMetric === 'IRR' ? 4 : 2))
const targetStep = computed(() => (form.targetMetric === 'IRR' ? 0.01 : form.targetMetric === 'STATIC_PAYBACK_YEARS' ? 0.5 : 1000))

async function run() {
  if (!props.scenarioId) return
  loading.value = true
  try {
    result.value = await apiPost<ReverseResult>(`/scenarios/${props.scenarioId}/reverse-runs`, {
      targetMetric: form.targetMetric,
      targetValue: form.targetValue,
      variable: form.variable
    })
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '目标反算失败')
  } finally {
    loading.value = false
  }
}

function formatNumber(v?: number | null) {
  if (v == null) return '—'
  return v.toLocaleString('zh-CN', { maximumFractionDigits: 4 })
}
function formatPercent(v?: number | null) {
  if (v == null) return '—'
  return (v * 100).toFixed(2) + '%'
}

watch(() => props.scenarioId, () => { result.value = null })
</script>

<style scoped>
.reverse-panel { display: flex; flex-direction: column; gap: 12px; }
.reverse-form .factor-row { display: grid; grid-template-columns: repeat(4, minmax(150px, 1fr)); gap: 10px; }
.target-input { width: 100%; }
.result-alert { font-size: 13px; }
.result-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.stat-cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); gap: 10px; align-content: start; }
.stat { border: 1px solid #e5e7eb; border-radius: 8px; padding: 10px 12px; background: #fafafa; }
.stat-label { font-size: 12px; color: #6b7280; margin-bottom: 4px; }
.stat-value { font-size: 18px; font-weight: 600; font-variant-numeric: tabular-nums; }
.notes { display: flex; flex-direction: column; gap: 10px; min-width: 0; }
.note :deep(.el-alert__description) { font-size: 12px; line-height: 1.7; white-space: pre-wrap; }
</style>

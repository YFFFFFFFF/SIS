<template>
  <div class="risk-alert-panel">
    <div class="toolbar">
      <el-button type="primary" :loading="evaluating" :disabled="!scenarioId" @click="evaluate">按最新测算评估风险</el-button>
      <el-button :loading="loading" @click="loadAll">刷新</el-button>
      <span class="hint">规则由管理员维护；触发 → 红灯/黄灯事件留痕，恢复自动标记</span>
    </div>

    <el-alert v-if="evalResult" :closable="false" show-icon class="eval-result"
      :type="evalResult.triggered.length > 0 ? 'error' : evalResult.recovered.length > 0 ? 'success' : 'success'"
      :title="evalSummary" />

    <div class="section-title">预警事件（当前方案）</div>
    <el-table :data="scenarioAlerts" size="small" empty-text="当前方案暂无预警事件" class="tbl">
      <el-table-column label="级别" width="70" align="center">
        <template #default="{ row }">
          <span class="lamp" :class="row.level.toLowerCase()" />
        </template>
      </el-table-column>
      <el-table-column prop="metricCode" label="指标" width="110">
        <template #default="{ row }">{{ metricName(row.metricCode) }}</template>
      </el-table-column>
      <el-table-column label="指标值 / 阈值" width="170">
        <template #default="{ row }">
          <span class="num">{{ fmt(row.metricValue) }}</span> / <span class="num muted">{{ fmt(row.thresholdValue) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="message" label="预警内容" min-width="320" show-overflow-tooltip />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 'OPEN' ? 'danger' : row.status === 'ACKED' ? 'warning' : 'success'">
            {{ riskAlertStatusName(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" align="center">
        <template #default="{ row }">
          <el-button v-if="row.status === 'OPEN'" size="small" text type="primary" @click="ack(row)">确认</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="section-title">
      阈值规则
      <el-button v-if="isAdmin" size="small" class="add-btn" @click="openCreate">+ 新建规则</el-button>
    </div>
    <el-table :data="rules" size="small" empty-text="暂无规则" class="tbl">
      <el-table-column prop="metricCode" label="监控指标" width="130">
        <template #default="{ row }">{{ metricName(row.metricCode) }}</template>
      </el-table-column>
      <el-table-column label="条件" width="160">
        <template #default="{ row }">
          {{ row.direction === 'BELOW' ? '<' : '>' }} <span class="num">{{ fmt(row.thresholdValue) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="级别" width="70" align="center">
        <template #default="{ row }"><span class="lamp" :class="row.level.toLowerCase()" /></template>
      </el-table-column>
      <el-table-column prop="strategy" label="策略建议" min-width="280" show-overflow-tooltip />
      <el-table-column label="启用" width="70" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column v-if="isAdmin" label="操作" width="130" align="center">
        <template #default="{ row }">
          <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" text type="danger" @click="removeRule(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingRule ? '编辑规则' : '新建规则'" width="520px">
      <el-form label-width="90px">
        <el-form-item label="监控指标">
          <el-select v-model="ruleForm.metricCode" class="full">
            <el-option v-for="m in metricOptions" :key="m" :label="metricName(m)" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="触发方向">
          <el-radio-group v-model="ruleForm.direction">
            <el-radio value="BELOW">低于阈值触发</el-radio>
            <el-radio value="ABOVE">高于阈值触发</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="阈值">
          <el-input-number v-model="ruleForm.thresholdValue" :precision="4" controls-position="right" class="full" />
        </el-form-item>
        <el-form-item label="预警级别">
          <el-radio-group v-model="ruleForm.level">
            <el-radio value="RED">红灯</el-radio>
            <el-radio value="YELLOW">黄灯</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="策略建议">
          <el-input v-model="ruleForm.strategy" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="ruleForm.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRule">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { apiDelete, apiGet, apiPost, apiPut } from '@/shared/api/http'
import type { RiskAlert, RiskEvaluationResult, RiskRule } from '@/shared/types/domain'
import { metricName, riskAlertStatusName } from '@/shared/i18n/display'
import { useWorkbenchStore } from '@/stores/workbench'

const props = defineProps<{ scenarioId: number | null }>()
const wb = useWorkbenchStore()

const metricOptions = ['NPV', 'IRR', 'STATIC_PAYBACK_YEARS', 'DYNAMIC_PAYBACK_YEARS', 'ROI', 'CAPITAL_NET_PROFIT_RATE', 'EQUITY_IRR', 'EQUITY_NPV']
const isAdmin = computed(() => wb.hasRole('ADMIN', 'SYSTEM_ADMINISTRATOR'))

const rules = ref<RiskRule[]>([])
const scenarioAlerts = ref<RiskAlert[]>([])
const evalResult = ref<RiskEvaluationResult | null>(null)
const loading = ref(false)
const evaluating = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingRule = ref<RiskRule | null>(null)
const ruleForm = reactive({ metricCode: 'IRR', direction: 'BELOW' as 'BELOW' | 'ABOVE', thresholdValue: 0.08, level: 'RED' as 'RED' | 'YELLOW', strategy: '', enabled: true })

const evalSummary = computed(() => {
  if (!evalResult.value) return ''
  const r = evalResult.value
  if (r.triggered.length > 0) return `评估 ${r.evaluatedRules} 条规则：触发 ${r.triggered.length} 条预警（${r.triggered.map(t => metricName(t.metricCode)).join('、')}）`
  if (r.recovered.length > 0) return `评估 ${r.evaluatedRules} 条规则：${r.recovered.length} 条预警已恢复`
  return `评估 ${r.evaluatedRules} 条规则：全部通过，无预警`
})

async function loadAll() {
  loading.value = true
  try {
    rules.value = await apiGet<RiskRule[]>('/risk-rules')
    if (props.scenarioId) {
      scenarioAlerts.value = await apiGet<RiskAlert[]>(`/scenarios/${props.scenarioId}/risk-alerts`)
    } else {
      scenarioAlerts.value = []
    }
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '加载失败')
  } finally {
    loading.value = false
  }
}

async function evaluate() {
  if (!props.scenarioId) return
  evaluating.value = true
  try {
    evalResult.value = await apiPost<RiskEvaluationResult>(`/scenarios/${props.scenarioId}/risk-alerts/evaluate`, {})
    await loadAll()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '风险评估失败')
  } finally {
    evaluating.value = false
  }
}

async function ack(row: RiskAlert) {
  try {
    await apiPost(`/risk-alerts/${row.id}/ack`, {})
    ElMessage.success('已确认')
    await loadAll()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '确认失败')
  }
}

function openCreate() {
  editingRule.value = null
  Object.assign(ruleForm, { metricCode: 'IRR', direction: 'BELOW', thresholdValue: 0.08, level: 'RED', strategy: '', enabled: true })
  dialogVisible.value = true
}

function openEdit(row: RiskRule) {
  editingRule.value = row
  Object.assign(ruleForm, { metricCode: row.metricCode, direction: row.direction, thresholdValue: row.thresholdValue, level: row.level, strategy: row.strategy ?? '', enabled: row.enabled })
  dialogVisible.value = true
}

async function saveRule() {
  saving.value = true
  try {
    if (editingRule.value) {
      await apiPut(`/risk-rules/${editingRule.value.id}`, ruleForm)
    } else {
      await apiPost('/risk-rules', ruleForm)
    }
    ElMessage.success('规则已保存')
    dialogVisible.value = false
    await loadAll()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function removeRule(row: RiskRule) {
  try {
    await ElMessageBox.confirm(`确认删除规则「${metricName(row.metricCode)} ${row.direction === 'BELOW' ? '<' : '>'} ${row.thresholdValue}」？`, '删除规则', { type: 'warning' })
    await apiDelete(`/risk-rules/${row.id}`)
    ElMessage.success('已删除')
    await loadAll()
  } catch (err) {
    if (err !== 'cancel') ElMessage.error(err instanceof Error ? err.message : '删除失败')
  }
}

function fmt(v?: number | null) {
  if (v == null) return '—'
  return v.toLocaleString('zh-CN', { maximumFractionDigits: 4 })
}

watch(() => props.scenarioId, () => { evalResult.value = null; loadAll() })

onMounted(loadAll)
</script>

<style scoped>
.risk-alert-panel { display: flex; flex-direction: column; gap: 12px; }
.toolbar { display: flex; align-items: center; gap: 12px; }
.hint { font-size: 12px; color: #9ca3af; }
.eval-result { font-size: 13px; }
.section-title { font-size: 13px; font-weight: 600; color: #374151; display: flex; align-items: center; gap: 10px; }
.add-btn { margin-left: auto; }
.tbl { width: 100%; }
.lamp { display: inline-block; width: 12px; height: 12px; border-radius: 50%; }
.lamp.red { background: #dc2626; box-shadow: 0 0 4px #dc2626; }
.lamp.yellow { background: #f59e0b; box-shadow: 0 0 4px #f59e0b; }
.lamp.green { background: #16a34a; }
.num { font-variant-numeric: tabular-nums; }
.muted { color: #9ca3af; }
.full { width: 100%; }
</style>

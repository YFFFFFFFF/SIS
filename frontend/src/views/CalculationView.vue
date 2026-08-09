<template>
  <section class="stack-panel">
    <el-card shadow="never">
      <template #header>执行测算<span v-if="!wb.selectedScenario" class="risk-hint">　请先在“测算方案”页选择方案</span></template>
      <div class="inline-actions"><el-input v-model="wb.requestKey" class="request-key" placeholder="请求标识" /><el-button :disabled="!wb.canRunCalculation" :loading="wb.loading.calculation" type="primary" @click="runCalculation">开始测算</el-button><el-button :disabled="!wb.currentTask" @click="wb.refreshResults">刷新结果</el-button></div>
      <el-alert v-if="wb.currentTask?.errorMessage" :closable="false" :title="wb.currentTask.errorMessage" type="error" />
      <el-descriptions v-if="wb.currentTask" :column="4" border class="task-summary"><el-descriptions-item label="任务 ID">{{ wb.currentTask.id }}</el-descriptions-item><el-descriptions-item label="状态">{{ calculationStatusName(wb.currentTask.status) }}</el-descriptions-item><el-descriptions-item label="进度">{{ wb.currentTask.progress }}%</el-descriptions-item><el-descriptions-item label="方案 ID">{{ wb.currentTask.scenarioId }}</el-descriptions-item></el-descriptions>
    </el-card>
    <el-card shadow="never"><template #header>核心指标</template><MetricChart v-if="wb.hasMetrics" :metrics="wb.calculation.metrics" /><el-table :data="metricRows" height="260"><el-table-column prop="code" label="指标编码" width="180" /><el-table-column prop="name" label="指标名称" min-width="180" /><el-table-column prop="value" label="指标值" min-width="160" /></el-table></el-card>
    <el-card shadow="never"><template #header>AI 决策辅助（FR-05）<span v-if="!wb.selectedScenario" class="risk-hint">　请先选择方案并完成测算</span></template><AiPanel :scenario-id="wb.selectedScenario?.id ?? null" /></el-card>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { apiPost, apiGet } from '@/shared/api/http'
import MetricChart from '@/components/charts/MetricChart.vue'
import AiPanel from '@/components/AiPanel.vue'
import { useWorkbenchStore } from '@/stores/workbench'
import type { CalculationRun, CalculationTask } from '@/shared/types/domain'
import { calculationStatusName, metricName } from '@/shared/i18n/display'

const wb = useWorkbenchStore()
const metricRows = computed(() => Object.entries(wb.calculation.metrics).map(([code, value]) => ({ code, name: metricName(code), value })))

async function runCalculation() {
  if (!wb.selectedScenario || !wb.canRunCalculation) return wb.notifyForbidden()
  wb.loading.calculation = true
  try {
    const response = await apiPost<CalculationRun>(`/scenarios/${wb.selectedScenario.id}/calculation-tasks`, { taskType: 'FINANCIAL', requestKey: wb.requestKey })
    wb.currentTask = response.task
    await pollTask(response.task.id)
  } catch (err) { wb.notifyError(err) } finally { wb.loading.calculation = false }
}

async function pollTask(taskId: number) {
  for (let i = 0; i < 12; i += 1) {
    const task = await apiGet<CalculationTask>(`/calculation-tasks/${taskId}`)
    wb.currentTask = task
    if (task.status === 'SUCCESS') return wb.refreshResults()
    if (task.status === 'FAILED') return
    await new Promise((resolve) => window.setTimeout(resolve, 1200))
  }
}
</script>

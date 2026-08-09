<template>
  <section class="split-panel">
    <el-card shadow="never">
      <template #header><div class="card-header-row"><span>测算方案（{{ wb.selectedProject?.name ?? '未选择项目' }}）</span><el-button :disabled="!wb.selectedProject" size="small" @click="wb.loadScenarios">刷新</el-button></div></template>
      <el-empty v-if="!wb.selectedProject" description="请先在“项目管理”页选择一个项目" />
      <el-table v-else :data="pagedScenarios" highlight-current-row empty-text="暂无数据" @current-change="wb.selectScenario">
        <el-table-column prop="name" label="方案名称" min-width="180" /><el-table-column prop="versionNo" label="版本" width="90" />
        <el-table-column label="状态" width="120"><template #default="{ row }">{{ scenarioStatusName(row.status) }}</template></el-table-column><el-table-column prop="horizonYears" label="测算期（年）" width="110" />
      </el-table>
      <Pager v-model:current-page="scnPage" v-model:page-size="scnSize" :total="wb.scenarios.length" />
    </el-card>
    <el-card shadow="never">
      <template #header>{{ wb.selectedScenario ? '编辑测算方案' : '创建测算方案' }}</template>
      <el-form label-position="top" class="dense-form">
        <el-form-item label="方案名称"><el-input v-model="wb.scenarioForm.name" :disabled="!wb.canEditScenario" /></el-form-item>
        <el-form-item label="状态"><el-select v-model="wb.scenarioForm.status" :disabled="!wb.canEditScenario"><el-option label="草稿" value="DRAFT" /><el-option label="已提交" value="SUBMITTED" /><el-option label="已通过" value="APPROVED" /><el-option label="已驳回" value="REJECTED" /></el-select></el-form-item>
        <el-form-item label="测算期（年）"><el-input-number v-model="wb.scenarioForm.horizonYears" :disabled="!wb.canEditScenario" :min="1" :max="50" /></el-form-item>
        <el-form-item label="建设期（年）"><el-input-number v-model="wb.scenarioForm.constructionYears" :disabled="!wb.canEditScenario" :min="0" :max="20" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="wb.scenarioForm.remarks" :disabled="!wb.canEditScenario" :rows="3" type="textarea" /></el-form-item>
        <div class="form-actions"><el-button @click="resetScenarioForm">重置</el-button><el-button :disabled="!wb.selectedProject || !wb.canEditScenario" :loading="wb.loading.scenarios" type="primary" @click="saveScenario">保存方案</el-button></div>
      </el-form>
    </el-card>
  </section>
  <el-card v-if="wb.selectedScenario" shadow="never" class="collab-card">
    <template #header>协同编辑（FR-04-02）—— {{ wb.selectedScenario.name }}</template>
    <CollabPanel :scenario-id="wb.selectedScenario.id" />
  </el-card>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { apiPost, apiPut } from '@/shared/api/http'
import { useWorkbenchStore } from '@/stores/workbench'
import type { Scenario } from '@/shared/types/domain'
import { scenarioStatusName } from '@/shared/i18n/display'
import CollabPanel from '@/components/CollabPanel.vue'
import Pager from '@/components/Pager.vue'

const wb = useWorkbenchStore()

const scnPage = ref(1)
const scnSize = ref(10)
const pagedScenarios = computed(() => wb.scenarios.slice((scnPage.value - 1) * scnSize.value, scnPage.value * scnSize.value))

async function saveScenario() {
  if (!wb.selectedProject || !wb.canEditScenario) return wb.notifyForbidden()
  wb.loading.scenarios = true
  try {
    if (wb.selectedScenario) await apiPut<Scenario>(`/scenarios/${wb.selectedScenario.id}`, wb.scenarioForm)
    else await apiPost<Scenario>(`/projects/${wb.selectedProject.id}/scenarios`, wb.scenarioForm)
    ElMessage.success('测算方案已保存')
    resetScenarioForm()
    await wb.loadScenarios()
  } catch (err) { wb.notifyError(err) } finally { wb.loading.scenarios = false }
}

function resetScenarioForm() {
  wb.selectedScenario = null
  Object.assign(wb.scenarioForm, { name: '', status: 'DRAFT', horizonYears: 5, constructionYears: 1, remarks: '' })
}
</script>

<style scoped>
.collab-card { margin-top: 14px; }
</style>

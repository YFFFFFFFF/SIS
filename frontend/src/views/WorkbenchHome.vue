<template>
  <WorkbenchLayout>
    <el-tabs v-model="activeTab" class="workflow-tabs">
      <el-tab-pane label="Projects" name="projects">
        <section class="split-panel">
          <el-card shadow="never">
            <template #header>Project List</template>
            <el-table :data="projects" highlight-current-row height="360" @current-change="selectProject">
              <el-table-column prop="code" label="Code" width="150" />
              <el-table-column prop="name" label="Name" min-width="180" />
              <el-table-column prop="status" label="Status" width="110" />
              <el-table-column prop="department" label="Department" width="140" />
            </el-table>
          </el-card>
          <el-card shadow="never">
            <template #header>{{ selectedProject ? 'Edit Project' : 'Create Project' }}</template>
            <el-form label-position="top" class="dense-form">
              <el-form-item label="Code">
                <el-input v-model="projectForm.code" :disabled="Boolean(selectedProject)" />
              </el-form-item>
              <el-form-item label="Name">
                <el-input v-model="projectForm.name" />
              </el-form-item>
              <el-form-item label="Type">
                <el-input v-model="projectForm.projectType" />
              </el-form-item>
              <el-form-item label="Status">
                <el-select v-model="projectForm.status">
                  <el-option label="Draft" value="DRAFT" />
                  <el-option label="Active" value="ACTIVE" />
                  <el-option label="Archived" value="ARCHIVED" />
                </el-select>
              </el-form-item>
              <el-form-item label="Department">
                <el-input v-model="projectForm.department" />
              </el-form-item>
              <el-form-item label="Tags">
                <el-input v-model="projectForm.tags" />
              </el-form-item>
              <el-form-item label="Description">
                <el-input v-model="projectForm.description" :rows="3" type="textarea" />
              </el-form-item>
              <div class="form-actions">
                <el-button @click="resetProjectForm">Reset</el-button>
                <el-button :loading="loading.projects" type="primary" @click="saveProject">Save Project</el-button>
              </div>
            </el-form>
          </el-card>
        </section>
      </el-tab-pane>

      <el-tab-pane label="Scenarios" name="scenarios">
        <section class="split-panel">
          <el-card shadow="never">
            <template #header>
              <div class="card-header-row">
                <span>Scenarios</span>
                <el-button :disabled="!selectedProject" size="small" @click="loadScenarios">Refresh</el-button>
              </div>
            </template>
            <el-empty v-if="!selectedProject" description="Select a project first" />
            <el-table v-else :data="scenarios" highlight-current-row height="360" @current-change="selectScenario">
              <el-table-column prop="name" label="Name" min-width="180" />
              <el-table-column prop="versionNo" label="Version" width="90" />
              <el-table-column prop="status" label="Status" width="120" />
              <el-table-column prop="horizonYears" label="Horizon" width="95" />
            </el-table>
          </el-card>
          <el-card shadow="never">
            <template #header>{{ selectedScenario ? 'Edit Scenario' : 'Create Scenario' }}</template>
            <el-form label-position="top" class="dense-form">
              <el-form-item label="Name">
                <el-input v-model="scenarioForm.name" />
              </el-form-item>
              <el-form-item label="Status">
                <el-select v-model="scenarioForm.status">
                  <el-option label="Draft" value="DRAFT" />
                  <el-option label="Submitted" value="SUBMITTED" />
                  <el-option label="Approved" value="APPROVED" />
                  <el-option label="Rejected" value="REJECTED" />
                </el-select>
              </el-form-item>
              <el-form-item label="Horizon Years">
                <el-input-number v-model="scenarioForm.horizonYears" :min="1" :max="50" />
              </el-form-item>
              <el-form-item label="Construction Years">
                <el-input-number v-model="scenarioForm.constructionYears" :min="0" :max="20" />
              </el-form-item>
              <el-form-item label="Remarks">
                <el-input v-model="scenarioForm.remarks" :rows="3" type="textarea" />
              </el-form-item>
              <div class="form-actions">
                <el-button @click="resetScenarioForm">Reset</el-button>
                <el-button :disabled="!selectedProject" :loading="loading.scenarios" type="primary" @click="saveScenario">Save Scenario</el-button>
              </div>
            </el-form>
          </el-card>
        </section>
      </el-tab-pane>

      <el-tab-pane label="Inputs" name="inputs">
        <section class="triple-panel">
          <el-card shadow="never">
            <template #header>Parameters</template>
            <el-form label-position="top" class="dense-form two-cols">
              <el-form-item label="WACC"><el-input-number v-model="parameterForm.wacc" :precision="4" :step="0.01" /></el-form-item>
              <el-form-item label="Tax Rate"><el-input-number v-model="parameterForm.taxRate" :precision="4" :step="0.01" /></el-form-item>
              <el-form-item label="Depreciation Years"><el-input-number v-model="parameterForm.depreciationYears" :min="1" /></el-form-item>
              <el-form-item label="Residual Rate"><el-input-number v-model="parameterForm.residualRate" :precision="4" :step="0.01" /></el-form-item>
              <el-form-item label="Loan Ratio Limit"><el-input-number v-model="parameterForm.loanRatioLimit" :precision="4" :step="0.01" /></el-form-item>
              <el-form-item label="Price Per Unit"><el-input-number v-model="parameterForm.pricePerUnit" :min="0" /></el-form-item>
              <el-form-item label="Unit Cost"><el-input-number v-model="parameterForm.unitCost" :min="0" /></el-form-item>
              <el-form-item label="Annual Output"><el-input-number v-model="parameterForm.annualOutput" :min="0" /></el-form-item>
              <el-form-item label="Fixed Cost"><el-input-number v-model="parameterForm.fixedOperatingCost" :min="0" /></el-form-item>
              <el-form-item label="Formula Version"><el-input v-model="parameterForm.formulaVersion" /></el-form-item>
            </el-form>
            <el-button :disabled="!selectedScenario" :loading="loading.parameters" type="primary" @click="saveParameters">Save Parameters</el-button>
          </el-card>
          <el-card shadow="never">
            <template #header>Investment Item</template>
            <el-form label-position="top" class="dense-form">
              <el-form-item label="Category"><el-input v-model="investmentForm.category" /></el-form-item>
              <el-form-item label="Name"><el-input v-model="investmentForm.name" /></el-form-item>
              <el-form-item label="Amount"><el-input-number v-model="investmentForm.amount" :min="0" /></el-form-item>
              <el-form-item label="Year No"><el-input-number v-model="investmentForm.yearNo" :min="0" /></el-form-item>
              <el-button :disabled="!selectedScenario" :loading="loading.inputs" type="primary" @click="addInvestmentItem">Add Investment</el-button>
            </el-form>
          </el-card>
          <el-card shadow="never">
            <template #header>Financing Plan</template>
            <el-form label-position="top" class="dense-form">
              <el-form-item label="Source Type"><el-input v-model="financingForm.sourceType" /></el-form-item>
              <el-form-item label="Ratio"><el-input-number v-model="financingForm.ratio" :precision="4" :step="0.1" /></el-form-item>
              <el-form-item label="Amount"><el-input-number v-model="financingForm.amount" :min="0" /></el-form-item>
              <el-form-item label="Interest Rate"><el-input-number v-model="financingForm.interestRate" :precision="4" :step="0.01" /></el-form-item>
              <el-form-item label="Term Years"><el-input-number v-model="financingForm.termYears" :min="0" /></el-form-item>
              <el-button :disabled="!selectedScenario" :loading="loading.inputs" type="primary" @click="addFinancingPlan">Add Financing</el-button>
            </el-form>
          </el-card>
        </section>
      </el-tab-pane>

      <el-tab-pane label="Calculation" name="calculation">
        <section class="stack-panel">
          <el-card shadow="never">
            <template #header>Run Calculation</template>
            <div class="inline-actions">
              <el-input v-model="requestKey" class="request-key" placeholder="Request key" />
              <el-button :disabled="!selectedScenario" :loading="loading.calculation" type="primary" @click="runCalculation">Calculate</el-button>
              <el-button :disabled="!currentTask" @click="refreshResults">Refresh Results</el-button>
              <el-button :disabled="!currentTask" :loading="loading.report" @click="generateReport">Generate Report</el-button>
              <el-button :disabled="!currentReport" @click="downloadReport">Download Report</el-button>
            </div>
            <el-alert v-if="currentTask?.errorMessage" :closable="false" :title="currentTask.errorMessage" type="error" />
            <el-descriptions v-if="currentTask" :column="4" border class="task-summary">
              <el-descriptions-item label="Task">{{ currentTask.id }}</el-descriptions-item>
              <el-descriptions-item label="Status">{{ currentTask.status }}</el-descriptions-item>
              <el-descriptions-item label="Progress">{{ currentTask.progress }}%</el-descriptions-item>
              <el-descriptions-item label="Scenario">{{ currentTask.scenarioId }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
          <el-card shadow="never">
            <template #header>Metrics</template>
            <MetricChart v-if="hasMetrics" :metrics="calculation.metrics" />
            <el-table :data="metricRows" height="260">
              <el-table-column prop="code" label="Metric" min-width="220" />
              <el-table-column prop="value" label="Value" min-width="160" />
            </el-table>
          </el-card>
          <el-card shadow="never">
            <template #header>Cash Flow</template>
            <el-table :data="calculation.cashFlowRows" height="300">
              <el-table-column prop="periodNo" label="Period" width="90" />
              <el-table-column prop="inflow" label="Inflow" />
              <el-table-column prop="outflow" label="Outflow" />
              <el-table-column prop="netCashFlow" label="Net CF" />
              <el-table-column prop="discountedCashFlow" label="Discounted CF" />
              <el-table-column prop="cumulativeCashFlow" label="Cumulative CF" />
            </el-table>
          </el-card>
        </section>
      </el-tab-pane>

      <el-tab-pane label="Governance" name="governance">
        <section class="split-panel">
          <el-card shadow="never">
            <template #header>Approval</template>
            <div class="vertical-actions">
              <el-button :disabled="!selectedScenario" type="primary" @click="submitApproval">Submit Scenario</el-button>
              <el-input-number v-model="approvalInstanceId" :min="1" placeholder="Instance id" />
              <el-button :disabled="!approvalInstanceId" @click="reviewApprove">Review Approve</el-button>
              <el-button :disabled="!approvalInstanceId" @click="finalApprove">Final Approve</el-button>
              <el-button :disabled="!approvalInstanceId" type="danger" @click="rejectApproval">Reject</el-button>
            </div>
            <el-descriptions v-if="approval" :column="2" border class="task-summary">
              <el-descriptions-item label="Instance">{{ approval.id }}</el-descriptions-item>
              <el-descriptions-item label="Node">{{ approval.currentNode }}</el-descriptions-item>
              <el-descriptions-item label="Status">{{ approval.status }}</el-descriptions-item>
              <el-descriptions-item label="Scenario">{{ approval.scenarioId }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
          <el-card shadow="never">
            <template #header>Edit Lock</template>
            <el-form label-position="top" class="dense-form">
              <el-form-item label="Holder Id"><el-input-number v-model="lockForm.holderId" :min="1" /></el-form-item>
              <el-form-item label="Holder Name"><el-input v-model="lockForm.holderName" /></el-form-item>
              <el-form-item label="TTL Minutes"><el-input-number v-model="lockForm.ttlMinutes" :min="1" /></el-form-item>
              <div class="form-actions">
                <el-button :disabled="!selectedScenario" type="primary" @click="acquireLock">Acquire Lock</el-button>
                <el-button :disabled="!selectedScenario" @click="releaseLock">Release Lock</el-button>
              </div>
            </el-form>
            <el-descriptions v-if="editLock" :column="2" border class="task-summary">
              <el-descriptions-item label="Holder">{{ editLock.holderName }}</el-descriptions-item>
              <el-descriptions-item label="Expire At">{{ editLock.expireAt }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </section>
      </el-tab-pane>

      <el-tab-pane label="Audit" name="audit">
        <el-card shadow="never">
          <template #header>Audit Query</template>
          <div class="inline-actions">
            <el-input v-model="auditQuery.targetType" class="audit-input" placeholder="Target type" />
            <el-input v-model="auditQuery.targetId" class="audit-input" placeholder="Target id" />
            <el-button :loading="loading.audit" type="primary" @click="loadAuditEvents">Query</el-button>
          </div>
          <el-table :data="auditEvents" height="420">
            <el-table-column prop="createdAt" label="Time" width="190" />
            <el-table-column prop="action" label="Action" width="180" />
            <el-table-column prop="targetType" label="Target" width="140" />
            <el-table-column prop="targetId" label="Target Id" width="120" />
            <el-table-column prop="afterValue" label="After" min-width="260" show-overflow-tooltip />
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </WorkbenchLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import WorkbenchLayout from '@/layouts/WorkbenchLayout.vue'
import MetricChart from '@/components/MetricChart.vue'
import { apiDelete, apiDownload, apiGet, apiPost, apiPut } from '@/shared/api/http'
import type { ApprovalInstance, AuditEvent, CalculationRun, CalculationTask, EditLock, Project, ReportDocument, Scenario } from '@/shared/types/domain'

const activeTab = ref('projects')
const projects = ref<Project[]>([])
const scenarios = ref<Scenario[]>([])
const selectedProject = ref<Project | null>(null)
const selectedScenario = ref<Scenario | null>(null)
const currentTask = ref<CalculationTask | null>(null)
const currentReport = ref<ReportDocument | null>(null)
const approval = ref<ApprovalInstance | null>(null)
const editLock = ref<EditLock | null>(null)
const approvalInstanceId = ref<number>()
const requestKey = ref(`manual-${Date.now()}`)
const auditEvents = ref<AuditEvent[]>([])
const calculation = reactive<CalculationRun>({ task: {} as CalculationTask, metrics: {}, cashFlowRows: [] })
const loading = reactive({ projects: false, scenarios: false, parameters: false, inputs: false, calculation: false, report: false, audit: false })

const projectForm = reactive({ code: '', name: '', projectType: 'INDUSTRIAL', status: 'DRAFT', department: '', tags: '', description: '' })
const scenarioForm = reactive({ name: '', status: 'DRAFT', horizonYears: 5, constructionYears: 1, remarks: '' })
const parameterForm = reactive({ wacc: 0.1, waccSource: 'manual benchmark', taxRate: 0.25, depreciationYears: 5, residualRate: 0, loanRatioLimit: 0.7, pricePerUnit: 140, unitCost: 40, annualOutput: 1000, fixedOperatingCost: 10000, formulaVersion: 'fin-m1-1.0.0' })
const investmentForm = reactive({ category: 'CONSTRUCTION', name: 'Construction Investment', amount: 200000, yearNo: 0 })
const financingForm = reactive({ sourceType: 'EQUITY', ratio: 1, amount: 220000, interestRate: 0, termYears: 0 })
const lockForm = reactive({ holderId: 1, holderName: 'Analyst', ttlMinutes: 30 })
const auditQuery = reactive({ targetType: 'SCENARIO', targetId: '' })

const hasMetrics = computed(() => Object.keys(calculation.metrics).length > 0)
const metricRows = computed(() => Object.entries(calculation.metrics).map(([code, value]) => ({ code, value })))

onMounted(loadProjects)

async function loadProjects() {
  loading.projects = true
  try {
    projects.value = await apiGet<Project[]>('/projects')
  } catch (err) {
    notifyError(err)
  } finally {
    loading.projects = false
  }
}

function selectProject(project: Project | null) {
  selectedProject.value = project
  selectedScenario.value = null
  scenarios.value = []
  if (project) {
    Object.assign(projectForm, project)
    loadScenarios()
  }
}

async function saveProject() {
  loading.projects = true
  try {
    if (selectedProject.value) {
      await apiPut<Project>(`/projects/${selectedProject.value.id}`, projectForm)
    } else {
      await apiPost<Project>('/projects', projectForm)
    }
    ElMessage.success('Project saved')
    resetProjectForm()
    await loadProjects()
  } catch (err) {
    notifyError(err)
  } finally {
    loading.projects = false
  }
}

function resetProjectForm() {
  selectedProject.value = null
  Object.assign(projectForm, { code: '', name: '', projectType: 'INDUSTRIAL', status: 'DRAFT', department: '', tags: '', description: '' })
}

async function loadScenarios() {
  if (!selectedProject.value) return
  loading.scenarios = true
  try {
    scenarios.value = await apiGet<Scenario[]>(`/projects/${selectedProject.value.id}/scenarios`)
  } catch (err) {
    notifyError(err)
  } finally {
    loading.scenarios = false
  }
}

function selectScenario(scenario: Scenario | null) {
  selectedScenario.value = scenario
  if (scenario) {
    Object.assign(scenarioForm, scenario)
    auditQuery.targetId = String(scenario.id)
  }
}

async function saveScenario() {
  if (!selectedProject.value) return
  loading.scenarios = true
  try {
    if (selectedScenario.value) {
      await apiPut<Scenario>(`/scenarios/${selectedScenario.value.id}`, scenarioForm)
    } else {
      await apiPost<Scenario>(`/projects/${selectedProject.value.id}/scenarios`, scenarioForm)
    }
    ElMessage.success('Scenario saved')
    resetScenarioForm()
    await loadScenarios()
  } catch (err) {
    notifyError(err)
  } finally {
    loading.scenarios = false
  }
}

function resetScenarioForm() {
  selectedScenario.value = null
  Object.assign(scenarioForm, { name: '', status: 'DRAFT', horizonYears: 5, constructionYears: 1, remarks: '' })
}

async function saveParameters() {
  if (!selectedScenario.value) return
  loading.parameters = true
  try {
    await apiPut(`/scenarios/${selectedScenario.value.id}/parameters`, parameterForm)
    ElMessage.success('Parameters saved')
  } catch (err) {
    notifyError(err)
  } finally {
    loading.parameters = false
  }
}

async function addInvestmentItem() {
  if (!selectedScenario.value) return
  loading.inputs = true
  try {
    await apiPost(`/scenarios/${selectedScenario.value.id}/investment-items`, investmentForm)
    ElMessage.success('Investment item added')
  } catch (err) {
    notifyError(err)
  } finally {
    loading.inputs = false
  }
}

async function addFinancingPlan() {
  if (!selectedScenario.value) return
  loading.inputs = true
  try {
    await apiPost(`/scenarios/${selectedScenario.value.id}/financing-plans`, financingForm)
    ElMessage.success('Financing plan added')
  } catch (err) {
    notifyError(err)
  } finally {
    loading.inputs = false
  }
}

async function runCalculation() {
  if (!selectedScenario.value) return
  loading.calculation = true
  try {
    const response = await apiPost<CalculationRun>(`/scenarios/${selectedScenario.value.id}/calculation-tasks`, { taskType: 'FINANCIAL', requestKey: requestKey.value })
    currentTask.value = response.task
    await pollTask(response.task.id)
  } catch (err) {
    notifyError(err)
  } finally {
    loading.calculation = false
  }
}

async function pollTask(taskId: number) {
  for (let i = 0; i < 12; i += 1) {
    const task = await apiGet<CalculationTask>(`/calculation-tasks/${taskId}`)
    currentTask.value = task
    if (task.status === 'SUCCESS') {
      await refreshResults()
      return
    }
    if (task.status === 'FAILED') {
      return
    }
    await new Promise((resolve) => window.setTimeout(resolve, 1200))
  }
}

async function refreshResults() {
  if (!currentTask.value) return
  const response = await apiGet<CalculationRun>(`/calculation-tasks/${currentTask.value.id}/results`)
  currentTask.value = response.task
  calculation.task = response.task
  calculation.metrics = response.metrics ?? {}
  calculation.cashFlowRows = response.cashFlowRows ?? []
}

async function generateReport() {
  if (!currentTask.value) return
  loading.report = true
  try {
    currentReport.value = await apiPost<ReportDocument>(`/calculation-tasks/${currentTask.value.id}/reports`)
    ElMessage.success('Report generated')
  } catch (err) {
    notifyError(err)
  } finally {
    loading.report = false
  }
}

async function downloadReport() {
  if (!currentReport.value) return
  const blob = await apiDownload(`/reports/${currentReport.value.id}/download`)
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = currentReport.value.fileName
  link.click()
  URL.revokeObjectURL(url)
}

async function submitApproval() {
  if (!selectedScenario.value) return
  approval.value = await apiPost<ApprovalInstance>(`/scenarios/${selectedScenario.value.id}/approval/submit`, { comment: 'Submitted from workbench' })
  approvalInstanceId.value = approval.value.id
}

async function reviewApprove() {
  if (!approvalInstanceId.value) return
  approval.value = await apiPost<ApprovalInstance>(`/approval-instances/${approvalInstanceId.value}/review/approve`, { comment: 'Review approved' })
}

async function finalApprove() {
  if (!approvalInstanceId.value) return
  approval.value = await apiPost<ApprovalInstance>(`/approval-instances/${approvalInstanceId.value}/approve`, { comment: 'Approved' })
}

async function rejectApproval() {
  if (!approvalInstanceId.value) return
  approval.value = await apiPost<ApprovalInstance>(`/approval-instances/${approvalInstanceId.value}/reject`, { comment: 'Rejected from workbench' })
}

async function acquireLock() {
  if (!selectedScenario.value) return
  editLock.value = await apiPost<EditLock>(`/scenarios/${selectedScenario.value.id}/lock`, lockForm)
}

async function releaseLock() {
  if (!selectedScenario.value) return
  await apiDelete(`/scenarios/${selectedScenario.value.id}/lock`, { holderId: lockForm.holderId })
  editLock.value = null
  ElMessage.success('Lock released')
}

async function loadAuditEvents() {
  loading.audit = true
  try {
    auditEvents.value = await apiGet<AuditEvent[]>(`/audit-events?targetType=${encodeURIComponent(auditQuery.targetType)}&targetId=${encodeURIComponent(auditQuery.targetId)}`)
  } catch (err) {
    notifyError(err)
  } finally {
    loading.audit = false
  }
}

function notifyError(err: unknown) {
  ElMessage.error(err instanceof Error ? err.message : 'Operation failed')
}
</script>
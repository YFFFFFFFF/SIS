import { defineStore } from 'pinia'
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { apiGet } from '@/shared/api/http'
import { useAuthStore } from '@/stores/auth'
import type { CalculationRun, CalculationTask, CashFlowRow, LoanScheduleRow, ProfitFlowItem, Project, ReportDocument, Scenario } from '@/shared/types/domain'

/**
 * R-03 工作台共享状态：路由拆分后，项目/方案选中、测算任务与各表单
 * 在多个 view 间共享，避免跨页丢失。业务逻辑保持原 WorkbenchHome 语义不变。
 */
export const useWorkbenchStore = defineStore('workbench', () => {
  const auth = useAuthStore()

  // ---- 状态 ----
  const projects = ref<Project[]>([])
  const scenarios = ref<Scenario[]>([])
  const selectedProject = ref<Project | null>(null)
  const selectedScenario = ref<Scenario | null>(null)
  const currentTask = ref<CalculationTask | null>(null)
  const currentReport = ref<ReportDocument | null>(null)
  const requestKey = ref(`manual-${Date.now()}`)
  const calculation = reactive<CalculationRun>({ task: {} as CalculationTask, metrics: {}, cashFlowRows: [] })
  const statementRows = ref<CashFlowRow[]>([])
  const profitFlow = ref<ProfitFlowItem[]>([])
  const loanSchedule = ref<LoanScheduleRow[]>([])
  const activeStatementType = ref('PROJECT_CASH_FLOW')
  const loading = reactive({ projects: false, scenarios: false, parameters: false, inputs: false, calculation: false, report: false, audit: false })

  const projectForm = reactive({ code: '', name: '', projectType: 'INDUSTRIAL', status: 'DRAFT', department: '', tags: '', description: '' })
  const scenarioForm = reactive({ name: '', status: 'DRAFT', horizonYears: 5, constructionYears: 1, remarks: '' })
  const parameterForm = reactive({ wacc: 0.1, waccSource: '手工录入基准值', taxRate: 0.25, depreciationYears: 5, residualRate: 0, loanRatioLimit: 0.7, pricePerUnit: 140, unitCost: 40, annualOutput: 1000, fixedOperatingCost: 10000, formulaVersion: 'fin-std-2.0.0', depreciationPolicy: 'STRAIGHT_LINE', amortizationYears: 0, amortizableAmount: 0, repaymentMethod: 'EQUAL_PRINCIPAL', taxSchedule: '', rampUp: '' })
  const investmentForm = reactive({ category: 'CONSTRUCTION', name: '建设投资', amount: 200000, yearNo: 0, itemCode: '' })
  const costForm = reactive({ category: 'RAW_MATERIAL', name: '外购原材料及燃料动力', amount: 40000, yearNo: 0 })
  const financingForm = reactive({ sourceType: 'EQUITY', ratio: 1, amount: 220000, interestRate: 0, termYears: 0, repaymentMethod: 'EQUAL_PRINCIPAL', graceYears: 0 })

  // ---- 角色权限（保持原 WorkbenchHome 矩阵不变） ----
  const currentRoles = computed(() => auth.user?.roles.map((role) => role.replace(/^ROLE_/, '')) ?? [])
  function hasRole(...roles: string[]) { return roles.some((role) => currentRoles.value.includes(role)) }
  const canManageProject = computed(() => hasRole('ANALYST', 'INVESTMENT_ANALYST', 'PROJECT_MANAGER', 'ADMIN', 'SYSTEM_ADMINISTRATOR'))
  const canEditScenario = computed(() => canManageProject.value)
  const canSaveParameters = computed(() => hasRole('ANALYST', 'INVESTMENT_ANALYST', 'FINANCE_SPECIALIST', 'ADMIN', 'SYSTEM_ADMINISTRATOR') && Boolean(selectedScenario.value))
  const canAddInvestment = computed(() => hasRole('ANALYST', 'INVESTMENT_ANALYST', 'TECHNICAL_ENGINEER', 'PROJECT_MANAGER', 'ADMIN', 'SYSTEM_ADMINISTRATOR') && Boolean(selectedScenario.value))
  const canAddFinancing = computed(() => hasRole('ANALYST', 'INVESTMENT_ANALYST', 'FINANCE_SPECIALIST', 'PROJECT_MANAGER', 'ADMIN', 'SYSTEM_ADMINISTRATOR') && Boolean(selectedScenario.value))
  const canAddCost = computed(() => hasRole('ANALYST', 'INVESTMENT_ANALYST', 'FINANCE_SPECIALIST', 'PROJECT_MANAGER', 'ADMIN', 'SYSTEM_ADMINISTRATOR') && Boolean(selectedScenario.value))
  const canRunCalculation = computed(() => hasRole('ANALYST', 'INVESTMENT_ANALYST', 'PROJECT_MANAGER', 'ADMIN', 'SYSTEM_ADMINISTRATOR') && Boolean(selectedScenario.value))
  const canGenerateReport = computed(() => hasRole('ANALYST', 'INVESTMENT_ANALYST', 'PROJECT_MANAGER', 'ADMIN', 'SYSTEM_ADMINISTRATOR'))
  const canSubmitApproval = computed(() => hasRole('ANALYST', 'INVESTMENT_ANALYST', 'PROJECT_MANAGER', 'ADMIN', 'SYSTEM_ADMINISTRATOR') && Boolean(selectedScenario.value))
  const canReviewApproval = computed(() => hasRole('FINANCE_SPECIALIST', 'ADMIN', 'SYSTEM_ADMINISTRATOR'))
  const canFinalApprove = computed(() => hasRole('PROJECT_MANAGER', 'ADMIN', 'SYSTEM_ADMINISTRATOR'))
  const canRejectApproval = computed(() => hasRole('FINANCE_SPECIALIST', 'PROJECT_MANAGER', 'ADMIN', 'SYSTEM_ADMINISTRATOR'))
  const canUseLock = computed(() => hasRole('ANALYST', 'INVESTMENT_ANALYST', 'FINANCE_SPECIALIST', 'TECHNICAL_ENGINEER', 'PROJECT_MANAGER', 'ADMIN', 'SYSTEM_ADMINISTRATOR') && Boolean(selectedScenario.value))
  const canQueryAudit = computed(() => hasRole('PROJECT_MANAGER', 'ADMIN', 'SYSTEM_ADMINISTRATOR'))

  const hasMetrics = computed(() => Object.keys(calculation.metrics).length > 0)

  // ---- 动作 ----
  async function loadProjects() {
    loading.projects = true
    try { projects.value = await apiGet<Project[]>('/projects') } catch (err) { notifyError(err) } finally { loading.projects = false }
  }

  function selectProject(project: Project | null) {
    selectedProject.value = project
    selectedScenario.value = null
    scenarios.value = []
    if (project) { Object.assign(projectForm, project); loadScenarios() }
  }

  async function loadScenarios() {
    if (!selectedProject.value) return
    loading.scenarios = true
    try { scenarios.value = await apiGet<Scenario[]>(`/projects/${selectedProject.value.id}/scenarios`) } catch (err) { notifyError(err) } finally { loading.scenarios = false }
  }

  function selectScenario(scenario: Scenario | null) {
    selectedScenario.value = scenario
    if (scenario) { Object.assign(scenarioForm, scenario) }
  }

  async function refreshResults() {
    if (!currentTask.value) return
    const response = await apiGet<CalculationRun>(`/calculation-tasks/${currentTask.value.id}/results`)
    currentTask.value = response.task
    calculation.task = response.task
    calculation.metrics = response.metrics ?? {}
    calculation.cashFlowRows = response.cashFlowRows ?? []
    await Promise.all([loadStatements(), loadProfitFlow(), loadLoanSchedule()])
  }

  async function loadStatements() {
    if (!currentTask.value) return
    statementRows.value = await apiGet<CashFlowRow[]>(`/calculation-tasks/${currentTask.value.id}/statements?type=${activeStatementType.value}`)
  }

  async function loadProfitFlow() {
    if (!currentTask.value) return
    try { profitFlow.value = await apiGet<ProfitFlowItem[]>(`/calculation-tasks/${currentTask.value.id}/profit-flow`) } catch { profitFlow.value = [] }
  }

  async function loadLoanSchedule() {
    if (!currentTask.value) return
    try { loanSchedule.value = await apiGet<LoanScheduleRow[]>(`/calculation-tasks/${currentTask.value.id}/loan-schedule`) } catch { loanSchedule.value = [] }
  }

  function notifyForbidden() { ElMessage.warning('当前角色或流程状态不允许执行该操作') }
  function notifyError(err: unknown) { ElMessage.error(err instanceof Error ? err.message : '操作失败') }

  return {
    projects, scenarios, selectedProject, selectedScenario, currentTask, currentReport, requestKey,
    calculation, statementRows, profitFlow, loanSchedule, activeStatementType, loading,
    projectForm, scenarioForm, parameterForm, investmentForm, costForm, financingForm,
    currentRoles, hasRole, canManageProject, canEditScenario, canSaveParameters, canAddInvestment,
    canAddFinancing, canAddCost, canRunCalculation, canGenerateReport, canSubmitApproval,
    canReviewApproval, canFinalApprove, canRejectApproval, canUseLock, canQueryAudit, hasMetrics,
    loadProjects, selectProject, loadScenarios, selectScenario, refreshResults, loadStatements,
    loadProfitFlow, loadLoanSchedule, notifyForbidden, notifyError
  }
})

export interface Project {
  id: number
  code: string
  name: string
  projectType?: string
  status: 'DRAFT' | 'ACTIVE' | 'ARCHIVED'
  department?: string
  ownerId?: number
  tags?: string
  description?: string
  createdAt: string
  updatedAt: string
}

export interface Scenario {
  id: number
  projectId: number
  name: string
  versionNo: number
  status: 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED'
  horizonYears: number
  constructionYears: number
  remarks?: string
  createdAt: string
  updatedAt: string
}

export interface ParameterSet {
  id: number
  scenarioId: number
  wacc: number
  waccSource?: string
  taxRate: number
  depreciationYears: number
  residualRate: number
  loanRatioLimit: number
  pricePerUnit: number
  unitCost: number
  annualOutput: number
  fixedOperatingCost: number
  formulaVersion?: string
  createdAt: string
}

export interface CalculationTask {
  id: number
  scenarioId: number
  taskType: string
  status: 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED'
  progress: number
  errorMessage?: string
  createdAt: string
  startedAt?: string
  finishedAt?: string
}

export interface CashFlowRow {
  id: number
  scenarioId: number
  taskId: number
  statementType: string
  periodNo: number
  inflow: number
  outflow: number
  netCashFlow: number
  discountedCashFlow: number
  cumulativeCashFlow: number
}

export interface CalculationRun {
  task: CalculationTask
  metrics: Record<string, number>
  cashFlowRows: CashFlowRow[]
}

export interface ReportDocument {
  id: number
  scenarioId: number
  taskId: number
  title: string
  fileName: string
  fileType: string
  status: string
  createdAt: string
}

export interface ApprovalInstance {
  id: number
  scenarioId: number
  status: string
  currentNode: string
  createdAt: string
  updatedAt: string
}

export interface AuditEvent {
  id: number
  actorId?: number
  actorName?: string
  action: string
  targetType: string
  targetId: string
  beforeValue?: string
  afterValue?: string
  traceId?: string
  createdAt: string
}

export interface EditLock {
  id: number
  scenarioId: number
  holderId: number
  holderName: string
  expireAt: string
  createdAt: string
}
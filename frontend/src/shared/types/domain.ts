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
  depreciationPolicy?: string
  amortizationYears?: number
  amortizableAmount?: number
  repaymentMethod?: string
  taxSchedule?: string
  rampUp?: string
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
  revenue?: number
  operatingCost?: number
  depreciation?: number
  amortization?: number
  interest?: number
  tax?: number
  netProfit?: number
}

export interface InvestmentItem {
  id: number
  scenarioId: number
  category: string
  name: string
  amount: number
  yearNo: number
  itemCode?: string
  parentId?: number
  sortOrder?: number
}

export interface CostItem {
  id: number
  scenarioId: number
  category: string
  name: string
  yearNo: number
  amount: number
  createdAt?: string
}

export interface InvestmentSummary {
  scenarioId: number
  constructionTotal: number
  interestDuringConstruction: number
  workingCapital: number
  totalInvestment: number
  declaredTotalInvestment?: number
  balanced: boolean
  items: InvestmentItem[]
}

export interface ProfitFlowItem {
  seq: number
  key: string
  label: string
  value: number
}

export interface LoanScheduleRow {
  yearNo: number
  openingBalance: number
  principalPaid: number
  interestPaid: number
  closingBalance: number
}

export interface SensitivityCell {
  factor1: number
  factor2?: number | null
  metricValue: number
}

export interface SensitivityResult {
  runId: number
  scenarioId: number
  targetMetric: string
  variable1: string
  variable2?: string | null
  baseValue: number
  coefficient1?: number | null
  coefficient2?: number | null
  criticalFactor1?: number | null
  criticalFactor2?: number | null
  level1?: string | null
  level2?: string | null
  matrix: SensitivityCell[]
}

export interface ReverseResult {
  runId: number
  scenarioId: number
  targetMetric: string
  targetValue: number
  variable: string
  factor: number
  solvedValue: number
  baseValue: number
  achievedValue: number
  feasible: boolean
  iterations: number
  sensitivityNote?: string | null
  boundaryNote?: string | null
}

export interface BreakEvenCurvePoint {
  output: number
  revenue: number
  totalCost: number
}

export interface BreakEvenResult {
  scenarioId: number
  pricePerUnit: number
  annualOutput: number
  unitVariableCost: number
  annualFixedCost: number
  bepOutput?: number | null
  bepUtilization?: number | null
  bepPrice?: number | null
  contributionMargin: number
  solvable: boolean
  unsolvableReason?: string | null
  curve: BreakEvenCurvePoint[]
  assumptionNote?: string | null
}

export interface MonteCarloVariableSpec {
  variable: string
  type: 'TRIANGULAR' | 'NORMAL'
  min?: number | null
  mode?: number | null
  max?: number | null
  mean?: number | null
  stdDev?: number | null
}

export interface MonteCarloHistogramBucket {
  from: number
  to: number
  count: number
  ratio: number
}

export interface MonteCarloCumulativePoint {
  value: number
  probability: number
}

export interface MonteCarloResultView {
  runId: number
  scenarioId: number
  targetMetric: string
  iterations: number
  seed: number
  variables: MonteCarloVariableSpec[]
  mean: number
  stdDev: number
  probPositive: number
  var95: number
  p5: number
  p50: number
  p95: number
  min: number
  max: number
  histogram: MonteCarloHistogramBucket[]
  cumulative: MonteCarloCumulativePoint[]
}

export interface RiskRule {
  id: number
  metricCode: string
  direction: 'BELOW' | 'ABOVE'
  thresholdValue: number
  level: 'RED' | 'YELLOW'
  strategy?: string | null
  enabled: boolean
  createdBy?: string | null
  createdAt: string
  updatedAt: string
}

export interface RiskAlert {
  id: number
  ruleId: number
  scenarioId: number
  scenarioName?: string | null
  taskId?: number | null
  metricCode: string
  metricValue: number
  thresholdValue: number
  level: 'RED' | 'YELLOW'
  message: string
  status: 'OPEN' | 'ACKED' | 'RECOVERED'
  ackBy?: string | null
  ackAt?: string | null
  createdAt: string
}

export interface RiskEvaluationResult {
  scenarioId: number
  evaluatedRules: number
  triggered: RiskAlert[]
  recovered: RiskAlert[]
}

export interface PortfolioMember {
  scenarioId: number
  scenarioName: string
  projectName?: string | null
  npv: number
  investment: number
  irr?: number | null
  selected: boolean
  rankNo?: number | null
}

export interface PortfolioFrontierPoint {
  budget: number
  npv: number
  investment: number
  count: number
}

export interface PortfolioResultView {
  runId: number
  budget: number
  maxCount?: number | null
  candidateCount: number
  totalNpv: number
  totalInvestment: number
  explanation?: string | null
  members: PortfolioMember[]
  frontier: PortfolioFrontierPoint[]
  engineVersion?: string | null
  createdBy?: string | null
  createdAt: string
}

export interface ScenarioComment {
  id: number
  scenarioId: number
  parentId?: number | null
  content: string
  mentions?: string | null
  authorId?: number | null
  authorName: string
  createdAt: string
}

export interface ScenarioChange {
  id: number
  scenarioId: number
  versionNo: number
  changeType: string
  fieldName?: string | null
  oldValue?: string | null
  newValue?: string | null
  operatorId?: number | null
  operatorName: string
  createdAt: string
}

export interface ScenarioPresence {
  userId: number
  userName: string
  lastSeenAt: string
}

/** R-15 收尾：字段级编辑锁（FR-04-02）。 */
export interface FieldLock {
  id: number
  scenarioId: number
  fieldKey: string
  holderId?: number | null
  holderName: string
  acquiredAt?: string | null
  expireAt?: string | null
  expired?: boolean
}

/** R-15 收尾：协同数据表一行（原型 P8"基础数据协同表"）。 */
export interface CollabFieldItem {
  fieldKey: string
  group: string
  itemName: string
  ownerDept: string
  currentValue: string
  lockHolder?: string | null
  lockExpireAt?: string | null
  lastEditor?: string | null
  lastEditAt?: string | null
}

export interface ProjectLibraryItem {
  id: number
  code: string
  name: string
  projectType?: string | null
  status: string
  department?: string | null
  tags: string[]
  description?: string | null
  latestNpv?: number | null
  latestIrr?: number | null
  hasReview: boolean
}

export interface ProjectReview {
  id: number
  projectId: number
  scenarioId?: number | null
  scenarioName?: string | null
  actualNpv?: number | null
  actualIrr?: number | null
  actualInvestment?: number | null
  actualPaybackYears?: number | null
  plannedNpv?: number | null
  plannedIrr?: number | null
  plannedInvestment?: number | null
  plannedPaybackYears?: number | null
  npvDeviation?: number | null
  irrDeviation?: number | null
  operationStartDate?: string | null
  lessons?: string | null
  createdBy?: string | null
  createdAt: string
  updatedAt: string
}

export interface AiParamItem {
  param: string
  current: number
  recommendedLow: number
  recommendedHigh: number
  basis: string
}

export interface AiParamRecommendation {
  scenarioId: number
  items: AiParamItem[]
  basisSummary: string
}

export interface AiFactorScore {
  factor: string
  name: string
  rawValue: string
  score: number
  weight: number
  weighted: number
  explain: string
}

export interface AiScoreResult {
  scenarioId: number
  modelCode: string
  modelVersion: string
  totalScore: number
  label: 'RECOMMEND' | 'CAUTION' | 'HOLD'
  disclaimer: string
  factors: AiFactorScore[]
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

export interface ApprovalFlowNode {
  id: number
  nodeCode: string
  nodeName: string
  seq: number
  approverRole: string
  conditionExpr?: string | null
}

export interface ApprovalFlow {
  id: number
  code: string
  name: string
  description?: string | null
  isDefault: boolean
  enabled: boolean
  nodes: ApprovalFlowNode[]
  createdAt: string
  updatedAt: string
}

export interface ApprovalTimelineNode {
  nodeCode: string
  nodeName: string
  seq: number
  approverRole: string
  current: boolean
  passed: boolean
}

export interface ApprovalTimelineEvent {
  nodeCode: string
  decision: string
  commentText?: string | null
  operatorId?: number | null
  operatedAt: string
}

export interface ApprovalTimeline {
  instanceId: number
  scenarioId: number
  status: string
  currentNode: string
  flowCode?: string | null
  flowName?: string | null
  flowNodes: ApprovalTimelineNode[]
  events: ApprovalTimelineEvent[]
  createdAt: string
  updatedAt: string
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
  prevHash?: string
  hash?: string
  createdAt: string
}

export interface AuditChainVerification {
  totalEvents: number
  linkedEvents: number
  intact: boolean
  brokenCount: number
  brokenEventIds: number[]
}

export interface EditLock {
  id: number
  scenarioId: number
  holderId: number
  holderName: string
  expireAt: string
  createdAt: string
}

export interface ComparisonScenarioColumn {
  scenarioId: number
  scenarioName: string
  taskId?: number | null
  calculatedAt?: string | null
  calculated: boolean
}

export interface ComparisonMetricRow {
  metricCode: string
  metricName: string
  unit: string
  direction: 'HIGHER' | 'LOWER' | 'NONE'
  values: (number | null)[]
  bestScenarioIds: number[]
}

export interface ComparisonRankingEntry {
  rank: number
  scenarioId: number
  scenarioName: string
  npv: number
  irr?: number | null
}

export interface ComparisonMatrix {
  projectId: number
  projectName: string
  scenarios: ComparisonScenarioColumn[]
  metrics: ComparisonMetricRow[]
  ranking: ComparisonRankingEntry[]
}

export interface DashboardKpis {
  projectCount: number
  weightedIrr?: number | null
  totalNpv?: number | null
  warningCount: number
}

export interface DashboardBubble {
  scenarioId: number
  scenarioName: string
  projectName: string
  npv: number
  irr: number
  investment: number
}

export interface DashboardNameValue {
  name: string
  value: number
}

export interface DashboardRiskSignal {
  variable: string
  currentValue: string
  level: 'RED' | 'YELLOW' | 'GREEN'
  note: string
}

export interface DashboardTodo {
  instanceId: number
  scenarioId: number
  scenarioName: string
  projectName: string
  currentNode: string
  status: string
  updatedAt: string
}

export interface DashboardSummary {
  kpis: DashboardKpis
  bubbles: DashboardBubble[]
  stageCounts: DashboardNameValue[]
  industryAmounts: DashboardNameValue[]
  riskSignals: DashboardRiskSignal[]
  todos: DashboardTodo[]
}
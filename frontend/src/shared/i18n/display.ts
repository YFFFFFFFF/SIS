type DisplayMap = Record<string, string>

export const projectStatusMap: DisplayMap = {
  DRAFT: '草稿',
  ACTIVE: '启用',
  ARCHIVED: '已归档'
}

export const scenarioStatusMap: DisplayMap = {
  DRAFT: '草稿',
  SUBMITTED: '已提交',
  APPROVED: '已通过',
  REJECTED: '已驳回'
}

export const calculationStatusMap: DisplayMap = {
  PENDING: '待处理',
  RUNNING: '计算中',
  SUCCESS: '成功',
  FAILED: '失败'
}

export const approvalStatusMap: DisplayMap = {
  IN_REVIEW: '财务复核中',
  IN_APPROVAL: '项目经理审批中',
  APPROVED: '已通过',
  REJECTED: '已驳回'
}

export const approvalNodeMap: DisplayMap = {
  SUBMIT: '提交',
  REVIEW: '财务复核',
  APPROVAL: '项目经理审批',
  APPROVED: '已通过',
  REJECTED: '已驳回'
}

export const roleMap: DisplayMap = {
  ANALYST: '分析师',
  INVESTMENT_ANALYST: '投资分析师',
  FINANCE_SPECIALIST: '财务专员',
  TECHNICAL_ENGINEER: '技术工程师',
  PROJECT_MANAGER: '项目管理者',
  SYSTEM_ADMINISTRATOR: '系统管理员',
  ADMIN: '管理员'
}

export const metricMap: DisplayMap = {
  TOTAL_INVESTMENT: '总投资',
  CONSTRUCTION_INTEREST: '建设期利息',
  NPV: '净现值',
  ROI: '总投资收益率',
  IRR: '内部收益率',
  CAPITAL_NET_PROFIT_RATE: '资本金净利润率',
  STATIC_PAYBACK_YEARS: '静态回收期',
  DYNAMIC_PAYBACK_YEARS: '动态回收期',
  EQUITY_IRR: '资本金内部收益率',
  EQUITY_NPV: '资本金净现值'
}

export const projectTypeMap: DisplayMap = {
  INDUSTRIAL: '产业项目',
  INFRASTRUCTURE: '基础设施',
  TECHNOLOGY: '科技项目',
  OTHER: '其他'
}

export const investmentCategoryMap: DisplayMap = {
  CONSTRUCTION: '建设投资',
  CONSTRUCTION_BUILDING: '建筑工程费',
  CONSTRUCTION_EQUIPMENT: '设备购置及安装费',
  CONSTRUCTION_OTHER: '工程建设其他费用',
  WORKING_CAPITAL: '流动资金',
  INTEREST_DURING_CONSTRUCTION: '建设期利息'
}

export const costCategoryMap: DisplayMap = {
  RAW_MATERIAL: '外购原材料及燃料动力',
  LABOR_MANUFACTURING: '人工及制造费用',
  OTHER_OPERATING: '其他经营成本'
}

export const depreciationPolicyMap: DisplayMap = {
  STRAIGHT_LINE: '年限平均法',
  DOUBLE_DECLINING: '双倍余额递减法',
  SUM_OF_YEARS_DIGITS: '年数总和法'
}

export const repaymentMethodMap: DisplayMap = {
  EQUAL_PRINCIPAL: '等额本金',
  EQUAL_PAYMENT: '等额本息',
  BULLET: '到期一次还本'
}

export const statementTypeMap: DisplayMap = {
  PROJECT_CASH_FLOW: '项目投资现金流量表',
  EQUITY_CASH_FLOW: '项目资本金现金流量表',
  FINANCIAL_PLAN: '财务计划现金流量表'
}

export const sensitivityVariableMap: DisplayMap = {
  PRICE: '产品售价',
  UNIT_COST: '单位成本',
  INVESTMENT: '建设投资',
  CONSTRUCTION_PERIOD: '建设工期'
}

export const sensitivityLevelMap: DisplayMap = {
  HIGH: '高敏感',
  MEDIUM: '中敏感',
  LOW: '低敏感'
}

export const reverseVariableMap: DisplayMap = {
  PRICE: '产品售价',
  INVESTMENT: '建设投资',
  ANNUAL_OUTPUT: '年产量',
  UNIT_COST: '单位成本'
}

export const monteCarloVariableMap: DisplayMap = {
  PRICE: '产品售价',
  UNIT_COST: '单位成本',
  INVESTMENT: '建设投资',
  ANNUAL_OUTPUT: '年产量'
}

export const riskDirectionMap: DisplayMap = {
  BELOW: '低于阈值触发',
  ABOVE: '高于阈值触发'
}

export const riskLevelMap: DisplayMap = {
  RED: '红灯',
  YELLOW: '黄灯',
  GREEN: '绿灯'
}

export const riskAlertStatusMap: DisplayMap = {
  OPEN: '待处理',
  ACKED: '已确认',
  RECOVERED: '已恢复'
}

export const financingSourceMap: DisplayMap = {
  EQUITY: '资本金',
  LOAN: '银行贷款'
}

export const reportStatusMap: DisplayMap = {
  GENERATED: '已生成'
}

export const targetTypeMap: DisplayMap = {
  PROJECT: '项目',
  SCENARIO: '测算方案',
  CALCULATION_TASK: '测算任务',
  REPORT_DOCUMENT: '报表',
  APPROVAL_INSTANCE: '审批流程',
  EDIT_LOCK: '编辑锁',
  IMPORT_JOB: '导入任务',
  SYS_USER: '用户'
}

export const auditActionMap: DisplayMap = {
  PROJECT_CREATED: '创建项目',
  PROJECT_UPDATED: '更新项目',
  SCENARIO_CREATED: '创建测算方案',
  SCENARIO_UPDATED: '更新测算方案',
  CALCULATION_COMPLETED: '测算完成',
  CALCULATION_FAILED: '测算失败',
  REPORT_GENERATED: '生成报表',
  REPORT_DOWNLOADED: '下载报表',
  APPROVAL_SUBMITTED: '提交审批',
  APPROVAL_REVIEW_APPROVED: '财务复核通过',
  APPROVAL_APPROVED: '审批通过',
  APPROVAL_REJECTED: '审批驳回',
  EDIT_LOCK_ACQUIRED: '获取编辑锁',
  EDIT_LOCK_REPLACED: '替换编辑锁',
  EDIT_LOCK_RELEASED: '释放编辑锁',
  IMPORT_SUCCESS: '导入成功',
  IMPORT_FAILURE: '导入失败',
  LOGIN_SUCCESS: '登录成功',
  LOGIN_FAILURE: '登录失败'
}

export function displayValue(map: DisplayMap, value?: string | null) {
  if (!value) return '-'
  return map[value] ?? value
}

export const projectStatusName = (value?: string | null) => displayValue(projectStatusMap, value)
export const scenarioStatusName = (value?: string | null) => displayValue(scenarioStatusMap, value)
export const calculationStatusName = (value?: string | null) => displayValue(calculationStatusMap, value)
export const approvalStatusName = (value?: string | null) => displayValue(approvalStatusMap, value)
export const approvalNodeName = (value?: string | null) => displayValue(approvalNodeMap, value)
export const roleName = (value?: string | null) => displayValue(roleMap, value)
export const metricName = (value?: string | null) => displayValue(metricMap, value)
export const projectTypeName = (value?: string | null) => displayValue(projectTypeMap, value)
export const investmentCategoryName = (value?: string | null) => displayValue(investmentCategoryMap, value)
export const costCategoryName = (value?: string | null) => displayValue(costCategoryMap, value)
export const depreciationPolicyName = (value?: string | null) => displayValue(depreciationPolicyMap, value)
export const repaymentMethodName = (value?: string | null) => displayValue(repaymentMethodMap, value)
export const statementTypeName = (value?: string | null) => displayValue(statementTypeMap, value)
export const sensitivityVariableName = (value?: string | null) => displayValue(sensitivityVariableMap, value)
export const sensitivityLevelName = (value?: string | null) => displayValue(sensitivityLevelMap, value)
export const reverseVariableName = (value?: string | null) => displayValue(reverseVariableMap, value)
export const monteCarloVariableName = (value?: string | null) => displayValue(monteCarloVariableMap, value)
export const riskDirectionName = (value?: string | null) => displayValue(riskDirectionMap, value)
export const riskLevelName = (value?: string | null) => displayValue(riskLevelMap, value)
export const riskAlertStatusName = (value?: string | null) => displayValue(riskAlertStatusMap, value)
export const financingSourceName = (value?: string | null) => displayValue(financingSourceMap, value)
export const reportStatusName = (value?: string | null) => displayValue(reportStatusMap, value)
export const targetTypeName = (value?: string | null) => displayValue(targetTypeMap, value)
export const auditActionName = (value?: string | null) => displayValue(auditActionMap, value)
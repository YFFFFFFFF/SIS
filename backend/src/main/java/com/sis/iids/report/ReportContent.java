package com.sis.iids.report;

import com.sis.iids.calculation.CalculationResultEntity;
import com.sis.iids.calculation.CalculationTask;
import com.sis.iids.calculation.CashFlowRow;
import com.sis.iids.calculation.InvestmentSummary;
import com.sis.iids.calculation.LoanScheduleResponse;
import com.sis.iids.calculation.ProfitFlowResponse;
import com.sis.iids.project.Project;
import com.sis.iids.scenario.ParameterSet;
import com.sis.iids.scenario.Scenario;
import com.sis.iids.sensitivity.SensitivityRun;

import java.util.List;

/**
 * 结构化报告内容（R-06）：项目概况、指标摘要、投资估算、三类报表数据、
 * 利润流向、还本付息与最近一次敏感性结论，供 Excel/PDF 两种 writer 共用。
 */
public record ReportContent(Project project,
                            Scenario scenario,
                            ParameterSet params,
                            CalculationTask task,
                            List<CalculationResultEntity> metrics,
                            InvestmentSummary investment,
                            List<CashFlowRow> cashFlowRows,
                            List<ProfitFlowResponse> profitFlow,
                            List<LoanScheduleResponse> loanSchedule,
                            SensitivityRun latestSensitivity) {
}

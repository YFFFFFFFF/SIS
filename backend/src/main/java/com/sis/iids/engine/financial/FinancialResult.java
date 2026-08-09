package com.sis.iids.engine.financial;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 财务引擎输出契约 v2（设计文档 §6.1）。
 * metrics 中不含值为 null 的指标（如 IRR 无解时缺省，红线：禁止以 0 占位）。
 */
public class FinancialResult {

    private BigDecimal totalInvestment = BigDecimal.ZERO;
    private BigDecimal constructionInterest = BigDecimal.ZERO;
    /** key = statement_type（MetricCodes.ST_PROJECT / ST_EQUITY / ST_PLAN） */
    private Map<String, List<StatementRow>> statements = new LinkedHashMap<>();
    private List<ProfitFlowItem> profitFlow = new ArrayList<>();
    private List<LoanScheduleRow> loanSchedule = new ArrayList<>();
    private Map<String, BigDecimal> metrics = new LinkedHashMap<>();

    public BigDecimal getTotalInvestment() {
        return totalInvestment;
    }

    public void setTotalInvestment(BigDecimal totalInvestment) {
        this.totalInvestment = totalInvestment;
    }

    public BigDecimal getConstructionInterest() {
        return constructionInterest;
    }

    public void setConstructionInterest(BigDecimal constructionInterest) {
        this.constructionInterest = constructionInterest;
    }

    public Map<String, List<StatementRow>> getStatements() {
        return statements;
    }

    public void setStatements(Map<String, List<StatementRow>> statements) {
        this.statements = statements;
    }

    public List<ProfitFlowItem> getProfitFlow() {
        return profitFlow;
    }

    public void setProfitFlow(List<ProfitFlowItem> profitFlow) {
        this.profitFlow = profitFlow;
    }

    public List<LoanScheduleRow> getLoanSchedule() {
        return loanSchedule;
    }

    public void setLoanSchedule(List<LoanScheduleRow> loanSchedule) {
        this.loanSchedule = loanSchedule;
    }

    public Map<String, BigDecimal> getMetrics() {
        return metrics;
    }

    public void setMetrics(Map<String, BigDecimal> metrics) {
        this.metrics = metrics;
    }

    /** 便捷取值：项目投资现金流量表 */
    public List<StatementRow> projectRows() {
        return statements.getOrDefault(MetricCodes.ST_PROJECT, List.of());
    }
}

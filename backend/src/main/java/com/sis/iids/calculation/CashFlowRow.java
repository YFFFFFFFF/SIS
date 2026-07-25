package com.sis.iids.calculation;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "cash_flow_row")
public class CashFlowRow {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;
    @Column(name = "task_id", nullable = false)
    private Long taskId;
    @Column(name = "statement_type", nullable = false, length = 64)
    private String statementType;
    @Column(name = "period_no", nullable = false)
    private Integer periodNo;
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal inflow;
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal outflow;
    @Column(name = "net_cash_flow", nullable = false, precision = 18, scale = 4)
    private BigDecimal netCashFlow;
    @Column(name = "discounted_cf", nullable = false, precision = 18, scale = 4)
    private BigDecimal discountedCashFlow;
    @Column(name = "cumulative_cf", nullable = false, precision = 18, scale = 4)
    private BigDecimal cumulativeCashFlow;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getScenarioId() { return scenarioId; }
    public void setScenarioId(Long scenarioId) { this.scenarioId = scenarioId; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getStatementType() { return statementType; }
    public void setStatementType(String statementType) { this.statementType = statementType; }
    public Integer getPeriodNo() { return periodNo; }
    public void setPeriodNo(Integer periodNo) { this.periodNo = periodNo; }
    public BigDecimal getInflow() { return inflow; }
    public void setInflow(BigDecimal inflow) { this.inflow = inflow; }
    public BigDecimal getOutflow() { return outflow; }
    public void setOutflow(BigDecimal outflow) { this.outflow = outflow; }
    public BigDecimal getNetCashFlow() { return netCashFlow; }
    public void setNetCashFlow(BigDecimal netCashFlow) { this.netCashFlow = netCashFlow; }
    public BigDecimal getDiscountedCashFlow() { return discountedCashFlow; }
    public void setDiscountedCashFlow(BigDecimal discountedCashFlow) { this.discountedCashFlow = discountedCashFlow; }
    public BigDecimal getCumulativeCashFlow() { return cumulativeCashFlow; }
    public void setCumulativeCashFlow(BigDecimal cumulativeCashFlow) { this.cumulativeCashFlow = cumulativeCashFlow; }
}
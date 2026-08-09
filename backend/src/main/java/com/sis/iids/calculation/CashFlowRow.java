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
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal revenue = BigDecimal.ZERO;
    @Column(name = "operating_cost", nullable = false, precision = 18, scale = 4)
    private BigDecimal operatingCost = BigDecimal.ZERO;
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal depreciation = BigDecimal.ZERO;
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal amortization = BigDecimal.ZERO;
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal interest = BigDecimal.ZERO;
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal tax = BigDecimal.ZERO;
    @Column(name = "net_profit", nullable = false, precision = 18, scale = 4)
    private BigDecimal netProfit = BigDecimal.ZERO;

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
    public BigDecimal getRevenue() { return revenue; }
    public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
    public BigDecimal getOperatingCost() { return operatingCost; }
    public void setOperatingCost(BigDecimal operatingCost) { this.operatingCost = operatingCost; }
    public BigDecimal getDepreciation() { return depreciation; }
    public void setDepreciation(BigDecimal depreciation) { this.depreciation = depreciation; }
    public BigDecimal getAmortization() { return amortization; }
    public void setAmortization(BigDecimal amortization) { this.amortization = amortization; }
    public BigDecimal getInterest() { return interest; }
    public void setInterest(BigDecimal interest) { this.interest = interest; }
    public BigDecimal getTax() { return tax; }
    public void setTax(BigDecimal tax) { this.tax = tax; }
    public BigDecimal getNetProfit() { return netProfit; }
    public void setNetProfit(BigDecimal netProfit) { this.netProfit = netProfit; }
}
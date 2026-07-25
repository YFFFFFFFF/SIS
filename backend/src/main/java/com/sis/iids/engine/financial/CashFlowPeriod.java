package com.sis.iids.engine.financial;

import java.math.BigDecimal;

public class CashFlowPeriod {

    private int periodNo;
    private BigDecimal inflow = BigDecimal.ZERO;
    private BigDecimal outflow = BigDecimal.ZERO;
    private BigDecimal netCashFlow = BigDecimal.ZERO;
    private BigDecimal discountedCashFlow = BigDecimal.ZERO;
    private BigDecimal cumulativeCashFlow = BigDecimal.ZERO;
    private BigDecimal revenue = BigDecimal.ZERO;
    private BigDecimal operatingCost = BigDecimal.ZERO;
    private BigDecimal depreciation = BigDecimal.ZERO;
    private BigDecimal interest = BigDecimal.ZERO;
    private BigDecimal tax = BigDecimal.ZERO;
    private BigDecimal netProfit = BigDecimal.ZERO;

    public int getPeriodNo() {
        return periodNo;
    }

    public void setPeriodNo(int periodNo) {
        this.periodNo = periodNo;
    }

    public BigDecimal getInflow() {
        return inflow;
    }

    public void setInflow(BigDecimal inflow) {
        this.inflow = inflow;
    }

    public BigDecimal getOutflow() {
        return outflow;
    }

    public void setOutflow(BigDecimal outflow) {
        this.outflow = outflow;
    }

    public BigDecimal getNetCashFlow() {
        return netCashFlow;
    }

    public void setNetCashFlow(BigDecimal netCashFlow) {
        this.netCashFlow = netCashFlow;
    }

    public BigDecimal getDiscountedCashFlow() {
        return discountedCashFlow;
    }

    public void setDiscountedCashFlow(BigDecimal discountedCashFlow) {
        this.discountedCashFlow = discountedCashFlow;
    }

    public BigDecimal getCumulativeCashFlow() {
        return cumulativeCashFlow;
    }

    public void setCumulativeCashFlow(BigDecimal cumulativeCashFlow) {
        this.cumulativeCashFlow = cumulativeCashFlow;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public BigDecimal getOperatingCost() {
        return operatingCost;
    }

    public void setOperatingCost(BigDecimal operatingCost) {
        this.operatingCost = operatingCost;
    }

    public BigDecimal getDepreciation() {
        return depreciation;
    }

    public void setDepreciation(BigDecimal depreciation) {
        this.depreciation = depreciation;
    }

    public BigDecimal getInterest() {
        return interest;
    }

    public void setInterest(BigDecimal interest) {
        this.interest = interest;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public void setTax(BigDecimal tax) {
        this.tax = tax;
    }

    public BigDecimal getNetProfit() {
        return netProfit;
    }

    public void setNetProfit(BigDecimal netProfit) {
        this.netProfit = netProfit;
    }
}

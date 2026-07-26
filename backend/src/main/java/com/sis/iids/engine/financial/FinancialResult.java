package com.sis.iids.engine.financial;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Financial calculation result for M1 standard metrics.
 */
public class FinancialResult {

    private BigDecimal totalInvestment = BigDecimal.ZERO;
    private BigDecimal npv = BigDecimal.ZERO;
    private BigDecimal roi = BigDecimal.ZERO;
    private BigDecimal irr = BigDecimal.ZERO;
    private BigDecimal capitalNetProfitRate = BigDecimal.ZERO;
    private BigDecimal staticPaybackYears = BigDecimal.ZERO;
    private BigDecimal dynamicPaybackYears = BigDecimal.ZERO;
    private List<CashFlowPeriod> rows = new ArrayList<>();

    public BigDecimal getTotalInvestment() {
        return totalInvestment;
    }

    public void setTotalInvestment(BigDecimal totalInvestment) {
        this.totalInvestment = totalInvestment;
    }

    public BigDecimal getNpv() {
        return npv;
    }

    public void setNpv(BigDecimal npv) {
        this.npv = npv;
    }

    public BigDecimal getRoi() {
        return roi;
    }

    public void setRoi(BigDecimal roi) {
        this.roi = roi;
    }

    public BigDecimal getIrr() {
        return irr;
    }

    public void setIrr(BigDecimal irr) {
        this.irr = irr;
    }

    public BigDecimal getCapitalNetProfitRate() {
        return capitalNetProfitRate;
    }

    public void setCapitalNetProfitRate(BigDecimal capitalNetProfitRate) {
        this.capitalNetProfitRate = capitalNetProfitRate;
    }

    public BigDecimal getStaticPaybackYears() {
        return staticPaybackYears;
    }

    public void setStaticPaybackYears(BigDecimal staticPaybackYears) {
        this.staticPaybackYears = staticPaybackYears;
    }

    public BigDecimal getDynamicPaybackYears() {
        return dynamicPaybackYears;
    }

    public void setDynamicPaybackYears(BigDecimal dynamicPaybackYears) {
        this.dynamicPaybackYears = dynamicPaybackYears;
    }

    public List<CashFlowPeriod> getRows() {
        return rows;
    }

    public void setRows(List<CashFlowPeriod> rows) {
        this.rows = rows;
    }
}

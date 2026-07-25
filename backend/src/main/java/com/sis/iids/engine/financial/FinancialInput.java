package com.sis.iids.engine.financial;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Simplified M1 financial model input.
 * Cash flow is generated from investment schedule + operating assumptions.
 */
public class FinancialInput {

    private int constructionYears = 2;
    private int horizonYears = 10;
    private BigDecimal wacc = new BigDecimal("0.08");
    private BigDecimal taxRate = new BigDecimal("0.25");
    private int depreciationYears = 10;
    private BigDecimal residualRate = new BigDecimal("0.05");
    private BigDecimal pricePerUnit = BigDecimal.ZERO;
    private BigDecimal unitCost = BigDecimal.ZERO;
    private BigDecimal annualOutput = BigDecimal.ZERO;
    private BigDecimal fixedOperatingCost = BigDecimal.ZERO;
    private BigDecimal constructionInvestment = BigDecimal.ZERO;
    private BigDecimal workingCapital = BigDecimal.ZERO;
    private BigDecimal interestDuringConstruction = BigDecimal.ZERO;
    private BigDecimal equityRatio = new BigDecimal("0.40");
    private BigDecimal loanRatio = new BigDecimal("0.60");
    private BigDecimal loanInterestRate = new BigDecimal("0.045");
    private int loanTermYears = 8;
    private List<BigDecimal> constructionSchedule = new ArrayList<>();

    public int getConstructionYears() {
        return constructionYears;
    }

    public void setConstructionYears(int constructionYears) {
        this.constructionYears = constructionYears;
    }

    public int getHorizonYears() {
        return horizonYears;
    }

    public void setHorizonYears(int horizonYears) {
        this.horizonYears = horizonYears;
    }

    public BigDecimal getWacc() {
        return wacc;
    }

    public void setWacc(BigDecimal wacc) {
        this.wacc = wacc;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public int getDepreciationYears() {
        return depreciationYears;
    }

    public void setDepreciationYears(int depreciationYears) {
        this.depreciationYears = depreciationYears;
    }

    public BigDecimal getResidualRate() {
        return residualRate;
    }

    public void setResidualRate(BigDecimal residualRate) {
        this.residualRate = residualRate;
    }

    public BigDecimal getPricePerUnit() {
        return pricePerUnit;
    }

    public void setPricePerUnit(BigDecimal pricePerUnit) {
        this.pricePerUnit = pricePerUnit;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public BigDecimal getAnnualOutput() {
        return annualOutput;
    }

    public void setAnnualOutput(BigDecimal annualOutput) {
        this.annualOutput = annualOutput;
    }

    public BigDecimal getFixedOperatingCost() {
        return fixedOperatingCost;
    }

    public void setFixedOperatingCost(BigDecimal fixedOperatingCost) {
        this.fixedOperatingCost = fixedOperatingCost;
    }

    public BigDecimal getConstructionInvestment() {
        return constructionInvestment;
    }

    public void setConstructionInvestment(BigDecimal constructionInvestment) {
        this.constructionInvestment = constructionInvestment;
    }

    public BigDecimal getWorkingCapital() {
        return workingCapital;
    }

    public void setWorkingCapital(BigDecimal workingCapital) {
        this.workingCapital = workingCapital;
    }

    public BigDecimal getInterestDuringConstruction() {
        return interestDuringConstruction;
    }

    public void setInterestDuringConstruction(BigDecimal interestDuringConstruction) {
        this.interestDuringConstruction = interestDuringConstruction;
    }

    public BigDecimal getEquityRatio() {
        return equityRatio;
    }

    public void setEquityRatio(BigDecimal equityRatio) {
        this.equityRatio = equityRatio;
    }

    public BigDecimal getLoanRatio() {
        return loanRatio;
    }

    public void setLoanRatio(BigDecimal loanRatio) {
        this.loanRatio = loanRatio;
    }

    public BigDecimal getLoanInterestRate() {
        return loanInterestRate;
    }

    public void setLoanInterestRate(BigDecimal loanInterestRate) {
        this.loanInterestRate = loanInterestRate;
    }

    public int getLoanTermYears() {
        return loanTermYears;
    }

    public void setLoanTermYears(int loanTermYears) {
        this.loanTermYears = loanTermYears;
    }

    public List<BigDecimal> getConstructionSchedule() {
        return constructionSchedule;
    }

    public void setConstructionSchedule(List<BigDecimal> constructionSchedule) {
        this.constructionSchedule = constructionSchedule;
    }

    public BigDecimal totalInvestment() {
        return constructionInvestment
                .add(workingCapital)
                .add(interestDuringConstruction);
    }
}

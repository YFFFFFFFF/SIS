package com.sis.iids.engine.financial;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 财务引擎输入模型 v2（设计文档 §4.1）。
 * 结构化模型：投资分项树平铺 + 成本分项 + 折旧政策 + 税率梯度 + 投产负荷 + 贷款条款。
 * 引擎为无状态纯计算（红线 R1），调用方可克隆修改后批量重算（敏感性/蒙特卡洛预留，§8.5）。
 */
public class FinancialInput {

    // ---- 期间 ----
    private int constructionYears = 2;
    private int operationYears = 10;
    private BigDecimal wacc = new BigDecimal("0.08");
    /** 折现率取值来源（透传留痕，不参与计算） */
    private String waccSource;

    // ---- 收入与负荷 ----
    private BigDecimal pricePerUnit = BigDecimal.ZERO;
    private BigDecimal annualOutput = BigDecimal.ZERO;
    private List<RampUpYear> rampUp = new ArrayList<>();

    // ---- 投资 ----
    private List<InvestmentEntry> constructionEntries = new ArrayList<>();
    /** 分年投资比例，长度 = constructionYears，合计 = 1 */
    private List<BigDecimal> constructionSchedule = new ArrayList<>();
    private BigDecimal workingCapital = BigDecimal.ZERO;
    private BigDecimal amortizableAmount = BigDecimal.ZERO;
    private int amortizationYears = 0;

    // ---- 成本（达产年口径 + 年度覆盖）----
    private List<CostEntry> costEntries = new ArrayList<>();
    /** 达产年单位可变成本（= RAW_MATERIAL 合计 / annualOutput，服务层预计算；可变成本随负荷线性缩放） */
    private BigDecimal unitVariableCost = BigDecimal.ZERO;

    // ---- 折旧与残值 ----
    private DepreciationPolicy depreciationPolicy = DepreciationPolicy.STRAIGHT_LINE;
    private int depreciationYears = 10;
    private BigDecimal residualRate = new BigDecimal("0.05");

    // ---- 税 ----
    private BigDecimal taxRate = new BigDecimal("0.25");
    private List<TaxBracket> taxSchedule = new ArrayList<>();

    // ---- 融资 ----
    private BigDecimal equityRatio = new BigDecimal("0.40");
    /** null 表示无贷款 */
    private LoanTerms loan;

    public int getConstructionYears() {
        return constructionYears;
    }

    public void setConstructionYears(int constructionYears) {
        this.constructionYears = constructionYears;
    }

    public int getOperationYears() {
        return operationYears;
    }

    public void setOperationYears(int operationYears) {
        this.operationYears = operationYears;
    }

    public BigDecimal getWacc() {
        return wacc;
    }

    public void setWacc(BigDecimal wacc) {
        this.wacc = wacc;
    }

    public String getWaccSource() {
        return waccSource;
    }

    public void setWaccSource(String waccSource) {
        this.waccSource = waccSource;
    }

    public BigDecimal getPricePerUnit() {
        return pricePerUnit;
    }

    public void setPricePerUnit(BigDecimal pricePerUnit) {
        this.pricePerUnit = pricePerUnit;
    }

    public BigDecimal getAnnualOutput() {
        return annualOutput;
    }

    public void setAnnualOutput(BigDecimal annualOutput) {
        this.annualOutput = annualOutput;
    }

    public List<RampUpYear> getRampUp() {
        return rampUp;
    }

    public void setRampUp(List<RampUpYear> rampUp) {
        this.rampUp = rampUp == null ? new ArrayList<>() : rampUp;
    }

    public List<InvestmentEntry> getConstructionEntries() {
        return constructionEntries;
    }

    public void setConstructionEntries(List<InvestmentEntry> constructionEntries) {
        this.constructionEntries = constructionEntries == null ? new ArrayList<>() : constructionEntries;
    }

    public List<BigDecimal> getConstructionSchedule() {
        return constructionSchedule;
    }

    public void setConstructionSchedule(List<BigDecimal> constructionSchedule) {
        this.constructionSchedule = constructionSchedule == null ? new ArrayList<>() : constructionSchedule;
    }

    public BigDecimal getWorkingCapital() {
        return workingCapital;
    }

    public void setWorkingCapital(BigDecimal workingCapital) {
        this.workingCapital = workingCapital;
    }

    public BigDecimal getAmortizableAmount() {
        return amortizableAmount;
    }

    public void setAmortizableAmount(BigDecimal amortizableAmount) {
        this.amortizableAmount = amortizableAmount;
    }

    public int getAmortizationYears() {
        return amortizationYears;
    }

    public void setAmortizationYears(int amortizationYears) {
        this.amortizationYears = amortizationYears;
    }

    public List<CostEntry> getCostEntries() {
        return costEntries;
    }

    public void setCostEntries(List<CostEntry> costEntries) {
        this.costEntries = costEntries == null ? new ArrayList<>() : costEntries;
    }

    public BigDecimal getUnitVariableCost() {
        return unitVariableCost;
    }

    public void setUnitVariableCost(BigDecimal unitVariableCost) {
        this.unitVariableCost = unitVariableCost;
    }

    public DepreciationPolicy getDepreciationPolicy() {
        return depreciationPolicy;
    }

    public void setDepreciationPolicy(DepreciationPolicy depreciationPolicy) {
        this.depreciationPolicy = depreciationPolicy;
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

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public List<TaxBracket> getTaxSchedule() {
        return taxSchedule;
    }

    public void setTaxSchedule(List<TaxBracket> taxSchedule) {
        this.taxSchedule = taxSchedule == null ? new ArrayList<>() : taxSchedule;
    }

    public BigDecimal getEquityRatio() {
        return equityRatio;
    }

    public void setEquityRatio(BigDecimal equityRatio) {
        this.equityRatio = equityRatio;
    }

    public LoanTerms getLoan() {
        return loan;
    }

    public void setLoan(LoanTerms loan) {
        this.loan = loan;
    }

    /** 建设投资合计（分项之和） */
    public BigDecimal constructionInvestment() {
        return constructionEntries.stream()
                .map(InvestmentEntry::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

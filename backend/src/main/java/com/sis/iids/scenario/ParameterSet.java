package com.sis.iids.scenario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "parameter_set")
public class ParameterSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scenario_id", nullable = false, unique = true)
    private Long scenarioId;

    @Column(nullable = false, precision = 12, scale = 8)
    private BigDecimal wacc;

    @Column(name = "wacc_source", length = 200)
    private String waccSource;

    @Column(name = "tax_rate", nullable = false, precision = 12, scale = 8)
    private BigDecimal taxRate;

    @Column(name = "depreciation_years", nullable = false)
    private Integer depreciationYears;

    @Column(name = "residual_rate", nullable = false, precision = 12, scale = 8)
    private BigDecimal residualRate;

    @Column(name = "loan_ratio_limit", nullable = false, precision = 12, scale = 8)
    private BigDecimal loanRatioLimit;

    @Column(name = "price_per_unit", nullable = false, precision = 18, scale = 4)
    private BigDecimal pricePerUnit;

    @Column(name = "unit_cost", nullable = false, precision = 18, scale = 4)
    private BigDecimal unitCost;

    @Column(name = "annual_output", nullable = false, precision = 18, scale = 4)
    private BigDecimal annualOutput;

    @Column(name = "fixed_operating_cost", nullable = false, precision = 18, scale = 4)
    private BigDecimal fixedOperatingCost;

    @Column(name = "formula_version", length = 64)
    private String formulaVersion;

    @Column(name = "depreciation_policy", nullable = false, length = 32)
    private String depreciationPolicy = "STRAIGHT_LINE";

    @Column(name = "amortization_years", nullable = false)
    private Integer amortizationYears = 0;

    @Column(name = "amortizable_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal amortizableAmount = BigDecimal.ZERO;

    @Column(name = "repayment_method", nullable = false, length = 32)
    private String repaymentMethod = "EQUAL_PRINCIPAL";

    @Column(name = "tax_schedule")
    private String taxSchedule;

    @Column(name = "ramp_up")
    private String rampUp;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(Long scenarioId) {
        this.scenarioId = scenarioId;
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

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public Integer getDepreciationYears() {
        return depreciationYears;
    }

    public void setDepreciationYears(Integer depreciationYears) {
        this.depreciationYears = depreciationYears;
    }

    public BigDecimal getResidualRate() {
        return residualRate;
    }

    public void setResidualRate(BigDecimal residualRate) {
        this.residualRate = residualRate;
    }

    public BigDecimal getLoanRatioLimit() {
        return loanRatioLimit;
    }

    public void setLoanRatioLimit(BigDecimal loanRatioLimit) {
        this.loanRatioLimit = loanRatioLimit;
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

    public String getFormulaVersion() {
        return formulaVersion;
    }

    public void setFormulaVersion(String formulaVersion) {
        this.formulaVersion = formulaVersion;
    }

    public String getDepreciationPolicy() {
        return depreciationPolicy;
    }

    public void setDepreciationPolicy(String depreciationPolicy) {
        this.depreciationPolicy = depreciationPolicy;
    }

    public Integer getAmortizationYears() {
        return amortizationYears;
    }

    public void setAmortizationYears(Integer amortizationYears) {
        this.amortizationYears = amortizationYears;
    }

    public BigDecimal getAmortizableAmount() {
        return amortizableAmount;
    }

    public void setAmortizableAmount(BigDecimal amortizableAmount) {
        this.amortizableAmount = amortizableAmount;
    }

    public String getRepaymentMethod() {
        return repaymentMethod;
    }

    public void setRepaymentMethod(String repaymentMethod) {
        this.repaymentMethod = repaymentMethod;
    }

    public String getTaxSchedule() {
        return taxSchedule;
    }

    public void setTaxSchedule(String taxSchedule) {
        this.taxSchedule = taxSchedule;
    }

    public String getRampUp() {
        return rampUp;
    }

    public void setRampUp(String rampUp) {
        this.rampUp = rampUp;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
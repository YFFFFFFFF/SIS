package com.sis.iids.calculation;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "financing_plan")
public class FinancingPlan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;
    @Column(name = "source_type", nullable = false, length = 64)
    private String sourceType;
    @Column(nullable = false, precision = 12, scale = 8)
    private BigDecimal ratio;
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal amount;
    @Column(name = "interest_rate", nullable = false, precision = 12, scale = 8)
    private BigDecimal interestRate;
    @Column(name = "term_years", nullable = false)
    private Integer termYears;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getScenarioId() { return scenarioId; }
    public void setScenarioId(Long scenarioId) { this.scenarioId = scenarioId; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public BigDecimal getRatio() { return ratio; }
    public void setRatio(BigDecimal ratio) { this.ratio = ratio; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
    public Integer getTermYears() { return termYears; }
    public void setTermYears(Integer termYears) { this.termYears = termYears; }
}
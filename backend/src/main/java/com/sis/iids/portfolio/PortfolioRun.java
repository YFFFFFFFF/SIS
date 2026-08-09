package com.sis.iids.portfolio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * R-13 组合优化运行记录（FR-03-02）。
 */
@Entity
@Table(name = "portfolio_run")
public class PortfolioRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, precision = 24, scale = 8)
    private BigDecimal budget;
    @Column(name = "max_count")
    private Integer maxCount;
    @Column(name = "candidate_count", nullable = false)
    private Integer candidateCount;
    @Column(name = "total_npv", precision = 24, scale = 8)
    private BigDecimal totalNpv;
    @Column(name = "total_investment", precision = 24, scale = 8)
    private BigDecimal totalInvestment;
    @Column(length = 2000)
    private String explanation;
    @Column(name = "frontier_json", length = 8000)
    private String frontierJson;
    @Column(name = "engine_version", nullable = false, length = 64)
    private String engineVersion;
    @Column(name = "created_by", length = 64)
    private String createdBy;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BigDecimal getBudget() { return budget; }
    public void setBudget(BigDecimal budget) { this.budget = budget; }
    public Integer getMaxCount() { return maxCount; }
    public void setMaxCount(Integer maxCount) { this.maxCount = maxCount; }
    public Integer getCandidateCount() { return candidateCount; }
    public void setCandidateCount(Integer candidateCount) { this.candidateCount = candidateCount; }
    public BigDecimal getTotalNpv() { return totalNpv; }
    public void setTotalNpv(BigDecimal totalNpv) { this.totalNpv = totalNpv; }
    public BigDecimal getTotalInvestment() { return totalInvestment; }
    public void setTotalInvestment(BigDecimal totalInvestment) { this.totalInvestment = totalInvestment; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public String getFrontierJson() { return frontierJson; }
    public void setFrontierJson(String frontierJson) { this.frontierJson = frontierJson; }
    public String getEngineVersion() { return engineVersion; }
    public void setEngineVersion(String engineVersion) { this.engineVersion = engineVersion; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

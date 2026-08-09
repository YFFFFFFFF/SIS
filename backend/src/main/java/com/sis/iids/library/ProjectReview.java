package com.sis.iids.library;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * R-16 项目复盘（FR-03-03）：已投运项目的实际指标 vs 计划指标对照。
 */
@Entity
@Table(name = "project_review")
public class ProjectReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "project_id", nullable = false, unique = true)
    private Long projectId;
    @Column(name = "scenario_id")
    private Long scenarioId;
    @Column(name = "actual_npv", precision = 24, scale = 8)
    private BigDecimal actualNpv;
    @Column(name = "actual_irr", precision = 24, scale = 8)
    private BigDecimal actualIrr;
    @Column(name = "actual_investment", precision = 24, scale = 8)
    private BigDecimal actualInvestment;
    @Column(name = "actual_payback_years", precision = 10, scale = 4)
    private BigDecimal actualPaybackYears;
    @Column(name = "operation_start_date")
    private LocalDate operationStartDate;
    @Column(length = 2000)
    private String lessons;
    @Column(name = "created_by", length = 64)
    private String createdBy;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getScenarioId() { return scenarioId; }
    public void setScenarioId(Long scenarioId) { this.scenarioId = scenarioId; }
    public BigDecimal getActualNpv() { return actualNpv; }
    public void setActualNpv(BigDecimal actualNpv) { this.actualNpv = actualNpv; }
    public BigDecimal getActualIrr() { return actualIrr; }
    public void setActualIrr(BigDecimal actualIrr) { this.actualIrr = actualIrr; }
    public BigDecimal getActualInvestment() { return actualInvestment; }
    public void setActualInvestment(BigDecimal actualInvestment) { this.actualInvestment = actualInvestment; }
    public BigDecimal getActualPaybackYears() { return actualPaybackYears; }
    public void setActualPaybackYears(BigDecimal actualPaybackYears) { this.actualPaybackYears = actualPaybackYears; }
    public LocalDate getOperationStartDate() { return operationStartDate; }
    public void setOperationStartDate(LocalDate operationStartDate) { this.operationStartDate = operationStartDate; }
    public String getLessons() { return lessons; }
    public void setLessons(String lessons) { this.lessons = lessons; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

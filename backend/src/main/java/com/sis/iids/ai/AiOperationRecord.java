package com.sis.iids.ai;

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
 * R-17 历史运营数据（FR-05）：已投项目实际运营数据，校验（verified）后纳入训练库。
 */
@Entity
@Table(name = "ai_operation_record")
public class AiOperationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "project_id", nullable = false)
    private Long projectId;
    @Column(nullable = false, length = 16)
    private String period;
    @Column(name = "actual_revenue", precision = 24, scale = 8)
    private BigDecimal actualRevenue;
    @Column(name = "actual_cost", precision = 24, scale = 8)
    private BigDecimal actualCost;
    @Column(name = "actual_output", precision = 24, scale = 8)
    private BigDecimal actualOutput;
    @Column(name = "actual_npv", precision = 24, scale = 8)
    private BigDecimal actualNpv;
    @Column(name = "actual_irr", precision = 24, scale = 8)
    private BigDecimal actualIrr;
    @Column(name = "deviation_ratio", precision = 10, scale = 6)
    private BigDecimal deviationRatio;
    @Column(nullable = false)
    private Boolean verified = false;
    @Column(length = 1000)
    private String note;
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
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public BigDecimal getActualRevenue() { return actualRevenue; }
    public void setActualRevenue(BigDecimal actualRevenue) { this.actualRevenue = actualRevenue; }
    public BigDecimal getActualCost() { return actualCost; }
    public void setActualCost(BigDecimal actualCost) { this.actualCost = actualCost; }
    public BigDecimal getActualOutput() { return actualOutput; }
    public void setActualOutput(BigDecimal actualOutput) { this.actualOutput = actualOutput; }
    public BigDecimal getActualNpv() { return actualNpv; }
    public void setActualNpv(BigDecimal actualNpv) { this.actualNpv = actualNpv; }
    public BigDecimal getActualIrr() { return actualIrr; }
    public void setActualIrr(BigDecimal actualIrr) { this.actualIrr = actualIrr; }
    public BigDecimal getDeviationRatio() { return deviationRatio; }
    public void setDeviationRatio(BigDecimal deviationRatio) { this.deviationRatio = deviationRatio; }
    public Boolean getVerified() { return verified; }
    public void setVerified(Boolean verified) { this.verified = verified; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

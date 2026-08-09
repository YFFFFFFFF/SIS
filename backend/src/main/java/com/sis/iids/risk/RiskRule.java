package com.sis.iids.risk;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * R-12 风险阈值规则（FR-02-04）：变量/方向/阈值/级别/策略建议，管理员可配置。
 */
@Entity
@Table(name = "risk_rule")
public class RiskRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "metric_code", nullable = false, length = 32)
    private String metricCode;
    /** BELOW：指标低于阈值触发；ABOVE：高于阈值触发 */
    @Column(nullable = false, length = 8)
    private String direction;
    @Column(name = "threshold_value", nullable = false, precision = 24, scale = 8)
    private BigDecimal thresholdValue;
    /** RED / YELLOW */
    @Column(nullable = false, length = 8)
    private String level;
    @Column(length = 1000)
    private String strategy;
    @Column(nullable = false)
    private Boolean enabled = true;
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
    public String getMetricCode() { return metricCode; }
    public void setMetricCode(String metricCode) { this.metricCode = metricCode; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public BigDecimal getThresholdValue() { return thresholdValue; }
    public void setThresholdValue(BigDecimal thresholdValue) { this.thresholdValue = thresholdValue; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

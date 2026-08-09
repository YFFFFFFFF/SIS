package com.sis.iids.risk;

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
 * R-12 风险预警事件（FR-02-04）：规则触发留痕，支持确认（ACK）与恢复（RECOVERED）。
 */
@Entity
@Table(name = "risk_alert_event")
public class RiskAlertEvent {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_ACKED = "ACKED";
    public static final String STATUS_RECOVERED = "RECOVERED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "rule_id", nullable = false)
    private Long ruleId;
    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;
    @Column(name = "task_id")
    private Long taskId;
    @Column(name = "metric_code", nullable = false, length = 32)
    private String metricCode;
    @Column(name = "metric_value", nullable = false, precision = 24, scale = 8)
    private BigDecimal metricValue;
    @Column(name = "threshold_value", nullable = false, precision = 24, scale = 8)
    private BigDecimal thresholdValue;
    @Column(nullable = false, length = 8)
    private String level;
    @Column(nullable = false, length = 1000)
    private String message;
    @Column(nullable = false, length = 16)
    private String status = STATUS_OPEN;
    @Column(name = "ack_by", length = 64)
    private String ackBy;
    @Column(name = "ack_at")
    private LocalDateTime ackAt;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRuleId() { return ruleId; }
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }
    public Long getScenarioId() { return scenarioId; }
    public void setScenarioId(Long scenarioId) { this.scenarioId = scenarioId; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getMetricCode() { return metricCode; }
    public void setMetricCode(String metricCode) { this.metricCode = metricCode; }
    public BigDecimal getMetricValue() { return metricValue; }
    public void setMetricValue(BigDecimal metricValue) { this.metricValue = metricValue; }
    public BigDecimal getThresholdValue() { return thresholdValue; }
    public void setThresholdValue(BigDecimal thresholdValue) { this.thresholdValue = thresholdValue; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAckBy() { return ackBy; }
    public void setAckBy(String ackBy) { this.ackBy = ackBy; }
    public LocalDateTime getAckAt() { return ackAt; }
    public void setAckAt(LocalDateTime ackAt) { this.ackAt = ackAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

package com.sis.iids.sensitivity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sensitivity_run")
public class SensitivityRun {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;
    @Column(name = "task_id")
    private Long taskId;
    @Column(name = "target_metric", nullable = false, length = 64)
    private String targetMetric;
    @Column(nullable = false, length = 32)
    private String variable1;
    @Column(nullable = false, precision = 8, scale = 4)
    private BigDecimal range1;
    @Column(nullable = false)
    private Integer steps1;
    @Column(length = 32)
    private String variable2;
    @Column(precision = 8, scale = 4)
    private BigDecimal range2;
    @Column
    private Integer steps2;
    @Column(name = "base_value", precision = 24, scale = 8)
    private BigDecimal baseValue;
    @Column(nullable = false, length = 32)
    private String status;
    @Column(name = "error_message", length = 2000)
    private String errorMessage;
    @Column(name = "engine_version", nullable = false, length = 64)
    private String engineVersion;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getScenarioId() { return scenarioId; }
    public void setScenarioId(Long scenarioId) { this.scenarioId = scenarioId; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getTargetMetric() { return targetMetric; }
    public void setTargetMetric(String targetMetric) { this.targetMetric = targetMetric; }
    public String getVariable1() { return variable1; }
    public void setVariable1(String variable1) { this.variable1 = variable1; }
    public BigDecimal getRange1() { return range1; }
    public void setRange1(BigDecimal range1) { this.range1 = range1; }
    public Integer getSteps1() { return steps1; }
    public void setSteps1(Integer steps1) { this.steps1 = steps1; }
    public String getVariable2() { return variable2; }
    public void setVariable2(String variable2) { this.variable2 = variable2; }
    public BigDecimal getRange2() { return range2; }
    public void setRange2(BigDecimal range2) { this.range2 = range2; }
    public Integer getSteps2() { return steps2; }
    public void setSteps2(Integer steps2) { this.steps2 = steps2; }
    public BigDecimal getBaseValue() { return baseValue; }
    public void setBaseValue(BigDecimal baseValue) { this.baseValue = baseValue; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getEngineVersion() { return engineVersion; }
    public void setEngineVersion(String engineVersion) { this.engineVersion = engineVersion; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

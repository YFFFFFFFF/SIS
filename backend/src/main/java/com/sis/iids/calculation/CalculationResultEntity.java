package com.sis.iids.calculation;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "calculation_result")
public class CalculationResultEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;
    @Column(name = "task_id", nullable = false)
    private Long taskId;
    @Column(name = "metric_code", nullable = false, length = 64)
    private String metricCode;
    @Column(name = "metric_value", nullable = false, precision = 24, scale = 8)
    private BigDecimal metricValue;
    @Column(name = "formula_version", nullable = false, length = 64)
    private String formulaVersion;
    @Column(name = "engine_version", nullable = false, length = 64)
    private String engineVersion;
    @Column(name = "parameter_set_id")
    private Long parameterSetId;
    @Column(name = "input_hash", length = 128)
    private String inputHash;
    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;
    @PrePersist void onCreate() { calculatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getScenarioId() { return scenarioId; }
    public void setScenarioId(Long scenarioId) { this.scenarioId = scenarioId; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getMetricCode() { return metricCode; }
    public void setMetricCode(String metricCode) { this.metricCode = metricCode; }
    public BigDecimal getMetricValue() { return metricValue; }
    public void setMetricValue(BigDecimal metricValue) { this.metricValue = metricValue; }
    public String getFormulaVersion() { return formulaVersion; }
    public void setFormulaVersion(String formulaVersion) { this.formulaVersion = formulaVersion; }
    public String getEngineVersion() { return engineVersion; }
    public void setEngineVersion(String engineVersion) { this.engineVersion = engineVersion; }
    public Long getParameterSetId() { return parameterSetId; }
    public void setParameterSetId(Long parameterSetId) { this.parameterSetId = parameterSetId; }
    public String getInputHash() { return inputHash; }
    public void setInputHash(String inputHash) { this.inputHash = inputHash; }
    public LocalDateTime getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(LocalDateTime calculatedAt) { this.calculatedAt = calculatedAt; }
}
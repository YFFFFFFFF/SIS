package com.sis.iids.reverse;

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
@Table(name = "reverse_run")
public class ReverseRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;
    @Column(name = "task_id")
    private Long taskId;
    @Column(name = "target_metric", nullable = false, length = 32)
    private String targetMetric;
    @Column(name = "target_value", nullable = false, precision = 24, scale = 8)
    private BigDecimal targetValue;
    @Column(nullable = false, length = 32)
    private String variable;
    @Column(precision = 24, scale = 8)
    private BigDecimal factor;
    @Column(name = "solved_value", precision = 24, scale = 8)
    private BigDecimal solvedValue;
    @Column(name = "base_value", precision = 24, scale = 8)
    private BigDecimal baseValue;
    @Column(name = "achieved_value", precision = 24, scale = 8)
    private BigDecimal achievedValue;
    @Column(nullable = false)
    private Boolean feasible = false;
    @Column(nullable = false)
    private Integer iterations = 0;
    @Column(name = "sensitivity_note", length = 1000)
    private String sensitivityNote;
    @Column(name = "boundary_note", length = 2000)
    private String boundaryNote;
    @Column(name = "engine_version", nullable = false, length = 64)
    private String engineVersion;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getScenarioId() { return scenarioId; }
    public void setScenarioId(Long scenarioId) { this.scenarioId = scenarioId; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getTargetMetric() { return targetMetric; }
    public void setTargetMetric(String targetMetric) { this.targetMetric = targetMetric; }
    public BigDecimal getTargetValue() { return targetValue; }
    public void setTargetValue(BigDecimal targetValue) { this.targetValue = targetValue; }
    public String getVariable() { return variable; }
    public void setVariable(String variable) { this.variable = variable; }
    public BigDecimal getFactor() { return factor; }
    public void setFactor(BigDecimal factor) { this.factor = factor; }
    public BigDecimal getSolvedValue() { return solvedValue; }
    public void setSolvedValue(BigDecimal solvedValue) { this.solvedValue = solvedValue; }
    public BigDecimal getBaseValue() { return baseValue; }
    public void setBaseValue(BigDecimal baseValue) { this.baseValue = baseValue; }
    public BigDecimal getAchievedValue() { return achievedValue; }
    public void setAchievedValue(BigDecimal achievedValue) { this.achievedValue = achievedValue; }
    public Boolean getFeasible() { return feasible; }
    public void setFeasible(Boolean feasible) { this.feasible = feasible; }
    public Integer getIterations() { return iterations; }
    public void setIterations(Integer iterations) { this.iterations = iterations; }
    public String getSensitivityNote() { return sensitivityNote; }
    public void setSensitivityNote(String sensitivityNote) { this.sensitivityNote = sensitivityNote; }
    public String getBoundaryNote() { return boundaryNote; }
    public void setBoundaryNote(String boundaryNote) { this.boundaryNote = boundaryNote; }
    public String getEngineVersion() { return engineVersion; }
    public void setEngineVersion(String engineVersion) { this.engineVersion = engineVersion; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

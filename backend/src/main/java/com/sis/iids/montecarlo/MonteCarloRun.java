package com.sis.iids.montecarlo;

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
 * R-11 蒙特卡洛运行记录（FR-02-03，红线 R11：种子入库可复现）。
 */
@Entity
@Table(name = "monte_carlo_run")
public class MonteCarloRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;
    @Column(name = "task_id")
    private Long taskId;
    @Column(name = "target_metric", nullable = false, length = 32)
    private String targetMetric;
    @Column(nullable = false)
    private Integer iterations;
    @Column(nullable = false)
    private Long seed;
    @Column(name = "variables_json", nullable = false, length = 4000)
    private String variablesJson;
    @Column(name = "mean_value", precision = 24, scale = 8)
    private BigDecimal meanValue;
    @Column(name = "std_dev", precision = 24, scale = 8)
    private BigDecimal stdDev;
    @Column(name = "prob_positive", precision = 10, scale = 6)
    private BigDecimal probPositive;
    @Column(name = "var95", precision = 24, scale = 8)
    private BigDecimal var95;
    @Column(name = "p5", precision = 24, scale = 8)
    private BigDecimal p5;
    @Column(name = "p50", precision = 24, scale = 8)
    private BigDecimal p50;
    @Column(name = "p95", precision = 24, scale = 8)
    private BigDecimal p95;
    @Column(name = "min_value", precision = 24, scale = 8)
    private BigDecimal minValue;
    @Column(name = "max_value", precision = 24, scale = 8)
    private BigDecimal maxValue;
    @Column(name = "histogram_json", length = 8000)
    private String histogramJson;
    @Column(name = "cumulative_json", length = 8000)
    private String cumulativeJson;
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
    public Integer getIterations() { return iterations; }
    public void setIterations(Integer iterations) { this.iterations = iterations; }
    public Long getSeed() { return seed; }
    public void setSeed(Long seed) { this.seed = seed; }
    public String getVariablesJson() { return variablesJson; }
    public void setVariablesJson(String variablesJson) { this.variablesJson = variablesJson; }
    public BigDecimal getMeanValue() { return meanValue; }
    public void setMeanValue(BigDecimal meanValue) { this.meanValue = meanValue; }
    public BigDecimal getStdDev() { return stdDev; }
    public void setStdDev(BigDecimal stdDev) { this.stdDev = stdDev; }
    public BigDecimal getProbPositive() { return probPositive; }
    public void setProbPositive(BigDecimal probPositive) { this.probPositive = probPositive; }
    public BigDecimal getVar95() { return var95; }
    public void setVar95(BigDecimal var95) { this.var95 = var95; }
    public BigDecimal getP5() { return p5; }
    public void setP5(BigDecimal p5) { this.p5 = p5; }
    public BigDecimal getP50() { return p50; }
    public void setP50(BigDecimal p50) { this.p50 = p50; }
    public BigDecimal getP95() { return p95; }
    public void setP95(BigDecimal p95) { this.p95 = p95; }
    public BigDecimal getMinValue() { return minValue; }
    public void setMinValue(BigDecimal minValue) { this.minValue = minValue; }
    public BigDecimal getMaxValue() { return maxValue; }
    public void setMaxValue(BigDecimal maxValue) { this.maxValue = maxValue; }
    public String getHistogramJson() { return histogramJson; }
    public void setHistogramJson(String histogramJson) { this.histogramJson = histogramJson; }
    public String getCumulativeJson() { return cumulativeJson; }
    public void setCumulativeJson(String cumulativeJson) { this.cumulativeJson = cumulativeJson; }
    public String getEngineVersion() { return engineVersion; }
    public void setEngineVersion(String engineVersion) { this.engineVersion = engineVersion; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

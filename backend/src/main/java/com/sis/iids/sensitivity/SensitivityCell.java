package com.sis.iids.sensitivity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "sensitivity_cell")
public class SensitivityCell {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "run_id", nullable = false)
    private Long runId;
    @Column(nullable = false, precision = 12, scale = 8)
    private BigDecimal factor1;
    @Column(precision = 12, scale = 8)
    private BigDecimal factor2;
    @Column(name = "metric_value", nullable = false, precision = 24, scale = 8)
    private BigDecimal metricValue;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRunId() { return runId; }
    public void setRunId(Long runId) { this.runId = runId; }
    public BigDecimal getFactor1() { return factor1; }
    public void setFactor1(BigDecimal factor1) { this.factor1 = factor1; }
    public BigDecimal getFactor2() { return factor2; }
    public void setFactor2(BigDecimal factor2) { this.factor2 = factor2; }
    public BigDecimal getMetricValue() { return metricValue; }
    public void setMetricValue(BigDecimal metricValue) { this.metricValue = metricValue; }
}

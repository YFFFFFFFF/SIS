package com.sis.iids.sensitivity;

import java.math.BigDecimal;
import java.util.List;

/**
 * 敏感性分析结果（FR-02-01）：基准值 + 矩阵 + 各因素敏感系数/临界值/等级。
 */
public record SensitivityResponse(
        Long runId,
        Long scenarioId,
        String targetMetric,
        String variable1,
        String variable2,
        BigDecimal baseValue,
        BigDecimal coefficient1,
        BigDecimal coefficient2,
        BigDecimal criticalFactor1,
        BigDecimal criticalFactor2,
        String level1,
        String level2,
        List<Cell> matrix
) {
    public record Cell(BigDecimal factor1, BigDecimal factor2, BigDecimal metricValue) {
    }
}

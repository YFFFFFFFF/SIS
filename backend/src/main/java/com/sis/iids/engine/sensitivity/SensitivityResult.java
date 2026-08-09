package com.sis.iids.engine.sensitivity;

import java.math.BigDecimal;
import java.util.List;

/**
 * 敏感性分析输出（FR-02-01）。
 * 单因素时 factor2/criticalFactor2 为 null，matrix 为一维（每行一个点）。
 */
public record SensitivityResult(
        String targetMetric,
        FactorSpec factor1,
        FactorSpec factor2,
        BigDecimal baseValue,
        List<SensitivityCell> matrix,
        BigDecimal coefficient1,
        BigDecimal coefficient2,
        BigDecimal criticalFactor1,
        BigDecimal criticalFactor2,
        String level1,
        String level2
) {
    /** 矩阵单元格：因素1/因素2 波动比例 → 目标指标值。 */
    public record SensitivityCell(BigDecimal factor1, BigDecimal factor2, BigDecimal metricValue) {
    }
}

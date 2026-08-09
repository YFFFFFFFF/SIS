package com.sis.iids.comparison;

import java.util.List;

/**
 * 多方案横向对比矩阵（PRD FR-03-01）。
 *
 * @param scenarios 方案列（含未测算方案，未测算时 calculated=false、各指标值为 null）
 * @param metrics   指标行（values 与 scenarios 顺序对齐；bestScenarioIds 为该指标最优方案，支持并列）
 * @param ranking   排序建议（按 NPV 降序，NPV 并列时按 IRR 降序；未测算方案不参与排序）
 */
public record ComparisonMatrix(
        Long projectId,
        String projectName,
        List<ScenarioColumn> scenarios,
        List<MetricRow> metrics,
        List<RankingEntry> ranking
) {

    public record ScenarioColumn(
            Long scenarioId,
            String scenarioName,
            Long taskId,
            String calculatedAt,
            boolean calculated
    ) {
    }

    /**
     * @param direction HIGHER=越大越好 / LOWER=越小越好 / NONE=不参与最优标记
     */
    public record MetricRow(
            String metricCode,
            String metricName,
            String unit,
            String direction,
            List<java.math.BigDecimal> values,
            List<Long> bestScenarioIds
    ) {
    }

    public record RankingEntry(
            int rank,
            Long scenarioId,
            String scenarioName,
            java.math.BigDecimal npv,
            java.math.BigDecimal irr
    ) {
    }
}

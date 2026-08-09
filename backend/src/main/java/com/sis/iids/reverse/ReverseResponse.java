package com.sis.iids.reverse;

import java.math.BigDecimal;

/**
 * 目标反算响应（FR-01-05）：临界值 + 敏感性说明 + 适用边界与假设声明。
 */
public record ReverseResponse(Long runId,
                              Long scenarioId,
                              String targetMetric,
                              BigDecimal targetValue,
                              String variable,
                              BigDecimal factor,
                              BigDecimal solvedValue,
                              BigDecimal baseValue,
                              BigDecimal achievedValue,
                              boolean feasible,
                              int iterations,
                              String sensitivityNote,
                              String boundaryNote) {
}

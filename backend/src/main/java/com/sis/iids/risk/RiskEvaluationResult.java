package com.sis.iids.risk;

import java.util.List;

/**
 * R-12 风险评估结果（FR-02-04）：触发的新事件 + 恢复的旧事件。
 */
public record RiskEvaluationResult(Long scenarioId,
                                   int evaluatedRules,
                                   List<RiskAlertResponse> triggered,
                                   List<RiskAlertResponse> recovered) {
}

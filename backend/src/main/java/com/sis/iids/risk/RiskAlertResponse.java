package com.sis.iids.risk;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * R-12 风险预警事件响应（FR-02-04）。
 */
public record RiskAlertResponse(Long id,
                                Long ruleId,
                                Long scenarioId,
                                String scenarioName,
                                Long taskId,
                                String metricCode,
                                BigDecimal metricValue,
                                BigDecimal thresholdValue,
                                String level,
                                String message,
                                String status,
                                String ackBy,
                                LocalDateTime ackAt,
                                LocalDateTime createdAt) {
}

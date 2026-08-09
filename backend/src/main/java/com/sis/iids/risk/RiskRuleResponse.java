package com.sis.iids.risk;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * R-12 风险规则响应（FR-02-04）。
 */
public record RiskRuleResponse(Long id,
                               String metricCode,
                               String direction,
                               BigDecimal thresholdValue,
                               String level,
                               String strategy,
                               boolean enabled,
                               String createdBy,
                               LocalDateTime createdAt,
                               LocalDateTime updatedAt) {
}

package com.sis.iids.risk;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * R-12 风险规则创建/更新请求（FR-02-04，管理员配置）。
 */
public record RiskRuleRequest(@NotBlank String metricCode,
                              @NotBlank String direction,
                              @NotNull BigDecimal thresholdValue,
                              @NotBlank String level,
                              String strategy,
                              Boolean enabled) {
}

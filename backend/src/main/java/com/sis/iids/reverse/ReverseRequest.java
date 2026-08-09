package com.sis.iids.reverse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 目标反算请求（FR-01-05）。
 */
public record ReverseRequest(@NotBlank String targetMetric,
                             @NotNull BigDecimal targetValue,
                             @NotBlank String variable,
                             Long taskId) {
}

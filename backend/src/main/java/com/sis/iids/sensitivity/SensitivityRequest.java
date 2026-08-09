package com.sis.iids.sensitivity;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 敏感性分析请求（FR-02-01）。variable2 可空 = 单因素。
 */
public record SensitivityRequest(
        Long taskId,
        @Size(max = 64) String targetMetric,
        @NotBlank @Size(max = 32) String variable1,
        @NotNull @DecimalMin("0.0001") BigDecimal range1,
        @NotNull @Min(3) @Max(21) Integer steps1,
        @Size(max = 32) String variable2,
        @DecimalMin("0.0001") BigDecimal range2,
        @Min(3) @Max(21) Integer steps2
) {
}

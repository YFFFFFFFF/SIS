package com.sis.iids.calculation;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record FinancingPlanRequest(
        @NotBlank @Size(max = 64) String sourceType,
        @NotNull @DecimalMin("0.0") BigDecimal ratio,
        @NotNull @DecimalMin("0.0") BigDecimal amount,
        @NotNull @DecimalMin("0.0") BigDecimal interestRate,
        @NotNull @Min(0) Integer termYears
) {
}
package com.sis.iids.scenario;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ParameterSetRequest(
        @NotNull @DecimalMin("0.0") BigDecimal wacc,
        @Size(max = 200) String waccSource,
        @NotNull @DecimalMin("0.0") BigDecimal taxRate,
        @NotNull @Min(1) Integer depreciationYears,
        @NotNull @DecimalMin("0.0") BigDecimal residualRate,
        @NotNull @DecimalMin("0.0") BigDecimal loanRatioLimit,
        @NotNull @DecimalMin("0.0") BigDecimal pricePerUnit,
        @NotNull @DecimalMin("0.0") BigDecimal unitCost,
        @NotNull @DecimalMin("0.0") BigDecimal annualOutput,
        @NotNull @DecimalMin("0.0") BigDecimal fixedOperatingCost,
        @Size(max = 64) String formulaVersion
) {
}
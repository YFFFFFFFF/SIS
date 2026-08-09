package com.sis.iids.calculation;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record InvestmentItemRequest(
        @NotBlank @Size(max = 64) String category,
        @NotBlank @Size(max = 200) String name,
        @NotNull @DecimalMin("0.0") BigDecimal amount,
        @NotNull @Min(0) Integer yearNo,
        @Size(max = 32) String itemCode,
        Long parentId,
        @Min(0) Integer sortOrder
) {
}

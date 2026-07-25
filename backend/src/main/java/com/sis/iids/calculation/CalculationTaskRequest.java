package com.sis.iids.calculation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CalculationTaskRequest(
        @NotBlank @Size(max = 32) String taskType
) {
}
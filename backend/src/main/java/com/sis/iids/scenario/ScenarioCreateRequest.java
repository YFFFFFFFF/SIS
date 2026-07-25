package com.sis.iids.scenario;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ScenarioCreateRequest(
        @NotBlank @Size(max = 200) String name,
        @NotNull @Min(1) Integer horizonYears,
        @NotNull @Min(1) Integer constructionYears,
        @Size(max = 1000) String remarks
) {
}
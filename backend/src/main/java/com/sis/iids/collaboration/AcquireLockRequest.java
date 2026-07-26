package com.sis.iids.collaboration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AcquireLockRequest(
        @NotNull Long holderId,
        @NotBlank String holderName,
        @NotNull @Min(1) Integer ttlMinutes
) {
}
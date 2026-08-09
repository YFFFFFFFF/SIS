package com.sis.iids.collab;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FieldLockAcquireRequest(
        @NotBlank String fieldKey,
        Long holderId,
        @NotBlank String holderName,
        @NotNull @Positive Integer ttlMinutes
) {
}

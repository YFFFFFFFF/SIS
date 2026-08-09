package com.sis.iids.collab;

import jakarta.validation.constraints.NotBlank;

public record FieldLockReleaseRequest(
        @NotBlank String fieldKey,
        Long holderId,
        String holderName
) {
}

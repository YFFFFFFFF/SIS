package com.sis.iids.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProjectUpdateRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 64) String projectType,
        @NotNull ProjectStatus status,
        @Size(max = 100) String department,
        Long ownerId,
        @Size(max = 500) String tags,
        @Size(max = 1000) String description
) {
}

package com.sis.iids.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectCreateRequest(
        @NotBlank @Size(max = 64) String code,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 64) String projectType,
        @Size(max = 100) String department,
        Long ownerId,
        @Size(max = 500) String tags,
        @Size(max = 1000) String description
) {
}
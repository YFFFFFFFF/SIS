package com.sis.iids.project;

import java.time.LocalDateTime;

public record ProjectResponse(
        Long id,
        String code,
        String name,
        String projectType,
        ProjectStatus status,
        String department,
        Long ownerId,
        String tags,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getCode(),
                project.getName(),
                project.getProjectType(),
                project.getStatus(),
                project.getDepartment(),
                project.getOwnerId(),
                project.getTags(),
                project.getDescription(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
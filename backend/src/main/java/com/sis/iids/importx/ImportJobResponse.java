package com.sis.iids.importx;

import java.time.LocalDateTime;

public record ImportJobResponse(
        Long id,
        Long scenarioId,
        String fileName,
        ImportJobStatus status,
        String message,
        LocalDateTime createdAt,
        LocalDateTime finishedAt
) {
    public static ImportJobResponse from(ImportJob job) {
        return new ImportJobResponse(
                job.getId(),
                job.getScenarioId(),
                job.getFileName(),
                job.getStatus(),
                job.getMessage(),
                job.getCreatedAt(),
                job.getFinishedAt());
    }
}

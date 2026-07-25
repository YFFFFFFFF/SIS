package com.sis.iids.calculation;

import java.time.LocalDateTime;

public record CalculationTaskResponse(Long id, Long scenarioId, String taskType, CalculationStatus status, Integer progress,
                                      String errorMessage, LocalDateTime createdAt, LocalDateTime startedAt,
                                      LocalDateTime finishedAt) {
    static CalculationTaskResponse from(CalculationTask task) {
        return new CalculationTaskResponse(task.getId(), task.getScenarioId(), task.getTaskType(), task.getStatus(),
                task.getProgress(), task.getErrorMessage(), task.getCreatedAt(), task.getStartedAt(), task.getFinishedAt());
    }
}
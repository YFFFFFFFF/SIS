package com.sis.iids.approval;

import java.time.LocalDateTime;

public record ApprovalInstanceResponse(
        Long id,
        Long scenarioId,
        ApprovalStatus status,
        String currentNode,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ApprovalInstanceResponse from(ApprovalInstance instance) {
        return new ApprovalInstanceResponse(
                instance.getId(),
                instance.getScenarioId(),
                instance.getStatus(),
                instance.getCurrentNode(),
                instance.getCreatedAt(),
                instance.getUpdatedAt());
    }
}
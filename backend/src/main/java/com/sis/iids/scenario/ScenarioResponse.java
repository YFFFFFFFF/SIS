package com.sis.iids.scenario;

import java.time.LocalDateTime;

public record ScenarioResponse(
        Long id,
        Long projectId,
        String name,
        Integer versionNo,
        ScenarioStatus status,
        Integer horizonYears,
        Integer constructionYears,
        String remarks,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    static ScenarioResponse from(Scenario scenario) {
        return new ScenarioResponse(
                scenario.getId(),
                scenario.getProjectId(),
                scenario.getName(),
                scenario.getVersionNo(),
                scenario.getStatus(),
                scenario.getHorizonYears(),
                scenario.getConstructionYears(),
                scenario.getRemarks(),
                scenario.getCreatedAt(),
                scenario.getUpdatedAt()
        );
    }
}
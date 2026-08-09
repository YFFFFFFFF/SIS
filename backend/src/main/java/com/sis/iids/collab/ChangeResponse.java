package com.sis.iids.collab;

import java.time.LocalDateTime;

/**
 * R-15 变更时间线条目（FR-04-02）。
 */
public record ChangeResponse(Long id,
                             Long scenarioId,
                             Integer versionNo,
                             String changeType,
                             String fieldName,
                             String oldValue,
                             String newValue,
                             Long operatorId,
                             String operatorName,
                             LocalDateTime createdAt) {
}

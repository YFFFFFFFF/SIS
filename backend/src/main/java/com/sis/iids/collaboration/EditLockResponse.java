package com.sis.iids.collaboration;

import java.time.LocalDateTime;

public record EditLockResponse(
        Long id,
        Long scenarioId,
        Long holderId,
        String holderName,
        LocalDateTime expireAt,
        LocalDateTime createdAt
) {
    public static EditLockResponse from(EditLock lock) {
        return new EditLockResponse(
                lock.getId(),
                lock.getScenarioId(),
                lock.getHolderId(),
                lock.getHolderName(),
                lock.getExpireAt(),
                lock.getCreatedAt());
    }
}
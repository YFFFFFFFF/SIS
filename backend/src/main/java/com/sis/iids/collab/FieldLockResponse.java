package com.sis.iids.collab;

import java.time.LocalDateTime;

public record FieldLockResponse(
        Long id,
        Long scenarioId,
        String fieldKey,
        Long holderId,
        String holderName,
        LocalDateTime acquiredAt,
        LocalDateTime expireAt,
        boolean expired
) {
    static FieldLockResponse from(ScenarioFieldLock lock) {
        return new FieldLockResponse(lock.getId(), lock.getScenarioId(), lock.getFieldKey(),
                lock.getHolderId(), lock.getHolderName(), lock.getAcquiredAt(), lock.getExpireAt(),
                lock.getExpireAt().isBefore(LocalDateTime.now()));
    }
}

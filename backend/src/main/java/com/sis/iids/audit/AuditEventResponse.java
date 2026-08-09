package com.sis.iids.audit;

import java.time.LocalDateTime;

public record AuditEventResponse(Long id, Long actorId, String actorName, String action, String targetType,
                                 String targetId, String beforeValue, String afterValue, String traceId,
                                 String prevHash, String hash, LocalDateTime createdAt) {
    static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(event.getId(), event.getActorId(), event.getActorName(), event.getAction(),
                event.getTargetType(), event.getTargetId(), event.getBeforeValue(), event.getAfterValue(),
                event.getTraceId(), event.getPrevHash(), event.getHash(), event.getCreatedAt());
    }
}
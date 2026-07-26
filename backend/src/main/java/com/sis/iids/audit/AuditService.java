package com.sis.iids.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public void record(String action, String targetType, String targetId, String beforeValue, String afterValue) {
        AuditEvent event = new AuditEvent();
        event.setAction(action);
        event.setTargetType(targetType);
        event.setTargetId(targetId);
        event.setBeforeValue(beforeValue);
        event.setAfterValue(afterValue);
        auditEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> list(String targetType, String targetId) {
        return auditEventRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(targetType, targetId).stream()
                .map(AuditEventResponse::from)
                .toList();
    }
}
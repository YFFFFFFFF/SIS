package com.sis.iids.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    /**
     * 写入审计事件并挂到哈希链上（R-08）：prev_hash 取上一条已链接事件的 hash，
     * 首条为 GENESIS；hash 为包含 prev_hash 的内容 SHA-256。
     * synchronized 防止并发写入时两条事件读到同一 prev_hash 造成链分叉。
     */
    @Transactional
    public synchronized void record(String action, String targetType, String targetId,
                                    String beforeValue, String afterValue) {
        AuditEvent event = new AuditEvent();
        event.setAction(action);
        event.setTargetType(targetType);
        event.setTargetId(targetId);
        event.setBeforeValue(beforeValue);
        event.setAfterValue(afterValue);
        String prevHash = auditEventRepository.findFirstByHashIsNotNullOrderByIdDesc()
                .map(AuditEvent::getHash)
                .orElse(AuditHasher.GENESIS);
        event.setPrevHash(prevHash);
        event.setHash(AuditHasher.hash(event));
        auditEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> list(String targetType, String targetId) {
        return auditEventRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(targetType, targetId).stream()
                .map(AuditEventResponse::from)
                .toList();
    }

    /**
     * 链完整性校验（R-08）：逐条重算 hash 并核对 prev_hash 链接。
     * V6 之前的历史事件 hash 为 NULL，按"未纳入链"跳过，不计入篡改。
     */
    @Transactional(readOnly = true)
    public AuditChainVerification verifyChain() {
        List<AuditEvent> events = auditEventRepository.findAllByOrderByIdAsc();
        int linked = 0;
        List<Long> brokenIds = new ArrayList<>();
        String expectedPrev = AuditHasher.GENESIS;
        for (AuditEvent event : events) {
            if (event.getHash() == null) {
                continue; // 历史遗留事件，未纳入链
            }
            linked++;
            boolean prevOk = expectedPrev.equals(event.getPrevHash());
            boolean hashOk = AuditHasher.hash(event).equals(event.getHash());
            if (!prevOk || !hashOk) {
                brokenIds.add(event.getId());
            }
            expectedPrev = event.getHash();
        }
        return new AuditChainVerification(events.size(), linked, brokenIds.isEmpty(),
                brokenIds.size(), brokenIds);
    }
}

package com.sis.iids.collab;

import com.sis.iids.audit.AuditService;
import com.sis.iids.common.error.BusinessException;
import com.sis.iids.common.error.ErrorCode;
import com.sis.iids.scenario.ScenarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * R-15 收尾：字段级编辑锁服务（FR-04-02 约束"编辑冲突需有锁定机制"）。
 * 与方案级 EditLock 并存、粒度更细：同一方案的不同字段可被多人同时锁定编辑；
 * 同一字段被他人持有时获取 → 409 冲突并提示持有人（冲突检测）；
 * 锁到期自动释放（定时清理）；管理员可强制释放（冲突合并的人工兜底）。
 */
@Service
public class FieldLockService {

    private static final int MAX_TTL_MINUTES = 120;

    private final ScenarioRepository scenarioRepository;
    private final ScenarioFieldLockRepository fieldLockRepository;
    private final CollabService collabService;
    private final AuditService auditService;
    private final CollabEventBus eventBus;

    public FieldLockService(ScenarioRepository scenarioRepository,
                            ScenarioFieldLockRepository fieldLockRepository,
                            CollabService collabService,
                            AuditService auditService,
                            CollabEventBus eventBus) {
        this.scenarioRepository = scenarioRepository;
        this.fieldLockRepository = fieldLockRepository;
        this.collabService = collabService;
        this.auditService = auditService;
        this.eventBus = eventBus;
    }

    @Transactional(readOnly = true)
    public List<FieldLockResponse> list(Long scenarioId) {
        if (!scenarioRepository.existsById(scenarioId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "测算方案不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        return fieldLockRepository.findByScenarioId(scenarioId).stream()
                .filter(l -> l.getExpireAt().isAfter(now))
                .map(FieldLockResponse::from)
                .toList();
    }

    /** 获取/续期字段锁：他人持有未过期 → 409 冲突；本人持有 → 续期；过期 → 接管。 */
    @Transactional
    public FieldLockResponse acquire(Long scenarioId, FieldLockAcquireRequest request) {
        if (!scenarioRepository.existsById(scenarioId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "测算方案不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        ScenarioFieldLock lock = fieldLockRepository.findByScenarioIdAndFieldKey(scenarioId, request.fieldKey())
                .map(existing -> reuseOrReject(existing, request, now))
                .orElseGet(ScenarioFieldLock::new);

        boolean isNew = lock.getId() == null;
        boolean replacingExpired = !isNew && lock.getExpireAt().isBefore(now)
                && !request.holderName().equals(lock.getHolderName());
        int ttl = Math.min(request.ttlMinutes(), MAX_TTL_MINUTES);
        lock.setScenarioId(scenarioId);
        lock.setFieldKey(request.fieldKey());
        lock.setHolderId(request.holderId());
        lock.setHolderName(request.holderName().trim());
        lock.setExpireAt(now.plusMinutes(ttl));
        ScenarioFieldLock saved = fieldLockRepository.save(lock);

        String action = isNew ? "FIELD_LOCK_ACQUIRED" : (replacingExpired ? "FIELD_LOCK_TAKEN_OVER" : "FIELD_LOCK_RENEWED");
        auditService.record(action, "FIELD_LOCK", saved.getId().toString(), null,
                "scenarioId=%s;field=%s;holder=%s".formatted(scenarioId, saved.getFieldKey(), saved.getHolderName()));
        collabService.recordChange(scenarioId, "LOCK_ACQUIRED", saved.getFieldKey(), null,
                (isNew ? "锁定字段 " : "续期/接管字段 ") + saved.getFieldKey(),
                request.holderId(), saved.getHolderName());
        FieldLockResponse response = FieldLockResponse.from(saved);
        eventBus.publish(scenarioId, "fieldlock", response);
        return response;
    }

    /** 释放字段锁：仅持有人本人可释放。 */
    @Transactional
    public void release(Long scenarioId, FieldLockReleaseRequest request) {
        ScenarioFieldLock lock = fieldLockRepository.findByScenarioIdAndFieldKey(scenarioId, request.fieldKey())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "该字段无有效编辑锁"));
        if (lock.getExpireAt().isBefore(LocalDateTime.now())) {
            fieldLockRepository.delete(lock);
            throw new BusinessException(ErrorCode.NOT_FOUND, "该字段编辑锁已过期自动释放");
        }
        boolean sameHolder = request.holderId() != null
                ? request.holderId().equals(lock.getHolderId())
                : request.holderName() != null && request.holderName().equals(lock.getHolderName());
        if (!sameHolder) {
            throw new BusinessException(ErrorCode.CONFLICT, "只有锁持有人（" + lock.getHolderName() + "）可以释放该字段锁");
        }
        doRelease(scenarioId, lock, "FIELD_LOCK_RELEASED", "释放字段 ");
    }

    /** 管理员强制释放（冲突合并的人工兜底）：无需持有人身份。 */
    @Transactional
    public void forceRelease(Long scenarioId, String fieldKey, String operatorName) {
        ScenarioFieldLock lock = fieldLockRepository.findByScenarioIdAndFieldKey(scenarioId, fieldKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "该字段无有效编辑锁"));
        doRelease(scenarioId, lock, "FIELD_LOCK_FORCE_RELEASED", "管理员强制释放字段 ");
    }

    private void doRelease(Long scenarioId, ScenarioFieldLock lock, String action, String prefix) {
        Long lockId = lock.getId();
        String fieldKey = lock.getFieldKey();
        String holderName = lock.getHolderName();
        fieldLockRepository.delete(lock);
        auditService.record(action, "FIELD_LOCK", lockId.toString(), null,
                "scenarioId=%s;field=%s;holder=%s".formatted(scenarioId, fieldKey, holderName));
        collabService.recordChange(scenarioId, "LOCK_RELEASED", fieldKey, holderName,
                prefix + fieldKey, null, holderName);
        eventBus.publish(scenarioId, "fieldlock",
                new FieldLockResponse(lockId, scenarioId, fieldKey, null, null, null, null, true));
    }

    /**
     * 写入强制拦截（R-15c）：校验给定字段是否被他人锁定。
     * 任一字段被非 holderName 的用户持有且未过期 → 409，消息列出被锁字段与持有人。
     * 供参数保存 / 分项更新等写入路径在变更前调用。
     */
    @Transactional(readOnly = true)
    public void assertFieldsEditable(Long scenarioId, java.util.Collection<String> fieldKeys, String holderName) {
        if (fieldKeys == null || fieldKeys.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<String> blocked = fieldLockRepository.findByScenarioId(scenarioId).stream()
                .filter(l -> fieldKeys.contains(l.getFieldKey()))
                .filter(l -> l.getExpireAt().isAfter(now))
                .filter(l -> !l.getHolderName().equals(holderName))
                .map(l -> l.getFieldKey() + "（" + l.getHolderName() + " 编辑中）")
                .toList();
        if (!blocked.isEmpty()) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "以下字段正被他人锁定编辑，无法保存：" + String.join("、", blocked));
        }
    }

    /** 惰性清理过期锁：项目未启用 @Scheduled，过期锁在 list/acquire 时过滤或接管。 */
    @Transactional
    public int purgeExpiredLocks() {
        LocalDateTime now = LocalDateTime.now();
        List<ScenarioFieldLock> expired = fieldLockRepository.findAll().stream()
                .filter(l -> l.getExpireAt().isBefore(now))
                .toList();
        expired.forEach(l -> {
            fieldLockRepository.delete(l);
            eventBus.publish(l.getScenarioId(), "fieldlock",
                    new FieldLockResponse(l.getId(), l.getScenarioId(), l.getFieldKey(), null, null, null, null, true));
        });
        return expired.size();
    }

    private ScenarioFieldLock reuseOrReject(ScenarioFieldLock existing, FieldLockAcquireRequest request, LocalDateTime now) {
        boolean sameHolder = request.holderId() != null
                ? request.holderId().equals(existing.getHolderId())
                : request.holderName() != null && request.holderName().equals(existing.getHolderName());
        if (sameHolder || existing.getExpireAt().isBefore(now)) {
            return existing;
        }
        throw new BusinessException(ErrorCode.CONFLICT,
                "字段正被 " + existing.getHolderName() + " 编辑（" + existing.getFieldKey() + "），请稍后或联系其释放");
    }
}

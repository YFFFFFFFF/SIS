package com.sis.iids.collaboration;

import com.sis.iids.audit.AuditService;
import com.sis.iids.common.error.BusinessException;
import com.sis.iids.common.error.ErrorCode;
import com.sis.iids.scenario.ScenarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class EditLockService {

    private final ScenarioRepository scenarioRepository;
    private final EditLockRepository editLockRepository;
    private final AuditService auditService;

    public EditLockService(ScenarioRepository scenarioRepository,
                           EditLockRepository editLockRepository,
                           AuditService auditService) {
        this.scenarioRepository = scenarioRepository;
        this.editLockRepository = editLockRepository;
        this.auditService = auditService;
    }

    @Transactional
    public EditLockResponse acquire(Long scenarioId, AcquireLockRequest request) {
        if (!scenarioRepository.existsById(scenarioId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Scenario not found");
        }
        LocalDateTime now = LocalDateTime.now();
        EditLock lock = editLockRepository.findByScenarioId(scenarioId)
                .map(existing -> reuseOrReject(existing, request, now))
                .orElseGet(EditLock::new);

        boolean replacingExpired = lock.getId() != null && lock.getExpireAt().isBefore(now);
        lock.setScenarioId(scenarioId);
        lock.setHolderId(request.holderId());
        lock.setHolderName(request.holderName().trim());
        lock.setExpireAt(now.plusMinutes(request.ttlMinutes()));
        EditLock saved = editLockRepository.save(lock);

        auditService.record(replacingExpired ? "EDIT_LOCK_REPLACED" : "EDIT_LOCK_ACQUIRED",
                "EDIT_LOCK", saved.getId().toString(), null,
                "scenarioId=%s;holderId=%s;holderName=%s".formatted(scenarioId, saved.getHolderId(), saved.getHolderName()));
        return EditLockResponse.from(saved);
    }

    @Transactional
    public ReleaseLockResponse release(Long scenarioId, ReleaseLockRequest request) {
        EditLock lock = editLockRepository.findByScenarioId(scenarioId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Edit lock not found"));
        if (!lock.getHolderId().equals(request.holderId())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Only lock holder can release the scenario lock");
        }
        String lockId = lock.getId().toString();
        editLockRepository.delete(lock);
        auditService.record("EDIT_LOCK_RELEASED", "EDIT_LOCK", lockId, null,
                "scenarioId=%s;holderId=%s".formatted(scenarioId, request.holderId()));
        return new ReleaseLockResponse(scenarioId, true);
    }

    private EditLock reuseOrReject(EditLock existing, AcquireLockRequest request, LocalDateTime now) {
        if (existing.getHolderId().equals(request.holderId()) || existing.getExpireAt().isBefore(now)) {
            return existing;
        }
        throw new BusinessException(ErrorCode.CONFLICT, "Scenario is locked by " + existing.getHolderName());
    }
}
package com.sis.iids.approval;

import com.sis.iids.audit.AuditService;
import com.sis.iids.common.error.BusinessException;
import com.sis.iids.common.error.ErrorCode;
import com.sis.iids.scenario.ScenarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovalService {

    private static final String NODE_SUBMIT = "SUBMIT";
    private static final String NODE_REVIEW = "REVIEW";
    private static final String NODE_APPROVAL = "APPROVAL";
    private static final String NODE_APPROVED = "APPROVED";
    private static final String NODE_REJECTED = "REJECTED";

    private final ScenarioRepository scenarioRepository;
    private final ApprovalInstanceRepository approvalInstanceRepository;
    private final ApprovalRecordRepository approvalRecordRepository;
    private final AuditService auditService;

    public ApprovalService(ScenarioRepository scenarioRepository,
                           ApprovalInstanceRepository approvalInstanceRepository,
                           ApprovalRecordRepository approvalRecordRepository,
                           AuditService auditService) {
        this.scenarioRepository = scenarioRepository;
        this.approvalInstanceRepository = approvalInstanceRepository;
        this.approvalRecordRepository = approvalRecordRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ApprovalInstanceResponse submit(Long scenarioId, ApprovalActionRequest request) {
        if (!scenarioRepository.existsById(scenarioId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Scenario not found");
        }
        approvalInstanceRepository.findFirstByScenarioIdOrderByCreatedAtDesc(scenarioId)
                .filter(this::active)
                .ifPresent(instance -> {
                    throw new BusinessException(ErrorCode.CONFLICT, "Scenario already has an active approval instance");
                });

        ApprovalInstance instance = new ApprovalInstance();
        instance.setScenarioId(scenarioId);
        instance.setStatus(ApprovalStatus.IN_REVIEW);
        instance.setCurrentNode(NODE_REVIEW);
        ApprovalInstance saved = approvalInstanceRepository.save(instance);
        record(saved.getId(), NODE_SUBMIT, ApprovalDecision.SUBMIT, comment(request));
        auditService.record("APPROVAL_SUBMITTED", "APPROVAL_INSTANCE", saved.getId().toString(), null,
                "scenarioId=%s;currentNode=%s".formatted(scenarioId, NODE_REVIEW));
        return ApprovalInstanceResponse.from(saved);
    }

    @Transactional
    public ApprovalInstanceResponse reviewApprove(Long instanceId, ApprovalActionRequest request) {
        ApprovalInstance instance = findInstance(instanceId);
        requireState(instance, ApprovalStatus.IN_REVIEW, NODE_REVIEW);
        instance.setStatus(ApprovalStatus.IN_APPROVAL);
        instance.setCurrentNode(NODE_APPROVAL);
        ApprovalInstance saved = approvalInstanceRepository.save(instance);
        record(saved.getId(), NODE_REVIEW, ApprovalDecision.APPROVE, comment(request));
        auditService.record("APPROVAL_REVIEW_APPROVED", "APPROVAL_INSTANCE", saved.getId().toString(), NODE_REVIEW, NODE_APPROVAL);
        return ApprovalInstanceResponse.from(saved);
    }

    @Transactional
    public ApprovalInstanceResponse approve(Long instanceId, ApprovalActionRequest request) {
        ApprovalInstance instance = findInstance(instanceId);
        requireState(instance, ApprovalStatus.IN_APPROVAL, NODE_APPROVAL);
        instance.setStatus(ApprovalStatus.APPROVED);
        instance.setCurrentNode(NODE_APPROVED);
        ApprovalInstance saved = approvalInstanceRepository.save(instance);
        record(saved.getId(), NODE_APPROVAL, ApprovalDecision.APPROVE, comment(request));
        auditService.record("APPROVAL_APPROVED", "APPROVAL_INSTANCE", saved.getId().toString(), NODE_APPROVAL, NODE_APPROVED);
        return ApprovalInstanceResponse.from(saved);
    }

    @Transactional
    public ApprovalInstanceResponse reject(Long instanceId, ApprovalActionRequest request) {
        ApprovalInstance instance = findInstance(instanceId);
        if (instance.getStatus() == ApprovalStatus.APPROVED || instance.getStatus() == ApprovalStatus.REJECTED) {
            throw new BusinessException(ErrorCode.CONFLICT, "Terminal approval instance cannot be rejected");
        }
        String node = instance.getCurrentNode();
        instance.setStatus(ApprovalStatus.REJECTED);
        instance.setCurrentNode(NODE_REJECTED);
        ApprovalInstance saved = approvalInstanceRepository.save(instance);
        record(saved.getId(), node, ApprovalDecision.REJECT, comment(request));
        auditService.record("APPROVAL_REJECTED", "APPROVAL_INSTANCE", saved.getId().toString(), node, NODE_REJECTED);
        return ApprovalInstanceResponse.from(saved);
    }

    private ApprovalInstance findInstance(Long instanceId) {
        return approvalInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Approval instance not found"));
    }

    private void requireState(ApprovalInstance instance, ApprovalStatus status, String node) {
        if (instance.getStatus() != status || !node.equals(instance.getCurrentNode())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Approval instance is not at required node");
        }
    }

    private boolean active(ApprovalInstance instance) {
        return instance.getStatus() == ApprovalStatus.IN_REVIEW || instance.getStatus() == ApprovalStatus.IN_APPROVAL;
    }

    private void record(Long instanceId, String nodeCode, ApprovalDecision decision, String comment) {
        ApprovalRecord record = new ApprovalRecord();
        record.setInstanceId(instanceId);
        record.setNodeCode(nodeCode);
        record.setDecision(decision);
        record.setCommentText(comment);
        approvalRecordRepository.save(record);
    }

    private String comment(ApprovalActionRequest request) {
        if (request == null || request.comment() == null || request.comment().isBlank()) {
            return null;
        }
        return request.comment().trim();
    }
}
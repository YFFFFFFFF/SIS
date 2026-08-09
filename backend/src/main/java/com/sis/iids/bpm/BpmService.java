package com.sis.iids.bpm;

import com.sis.iids.approval.ApprovalInstance;
import com.sis.iids.approval.ApprovalInstanceRepository;
import com.sis.iids.approval.ApprovalRecord;
import com.sis.iids.approval.ApprovalRecordRepository;
import com.sis.iids.audit.AuditService;
import com.sis.iids.common.error.BusinessException;
import com.sis.iids.common.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * R-14 BPM 可配置审批流服务（FR-04-03）。
 * 流程定义 CRUD（管理员）+ 默认模板（M1 固定三段链迁移入库）+ 流程追踪时间线。
 * 说明：节点条件规则 condition_expr 本期落库预留，实例推进仍走现有固定链（P0 壳/P1 实）。
 */
@Service
public class BpmService {

    private final ApprovalFlowDefRepository flowRepository;
    private final ApprovalNodeDefRepository nodeRepository;
    private final ApprovalInstanceRepository instanceRepository;
    private final ApprovalRecordRepository recordRepository;
    private final AuditService auditService;

    public BpmService(ApprovalFlowDefRepository flowRepository,
                      ApprovalNodeDefRepository nodeRepository,
                      ApprovalInstanceRepository instanceRepository,
                      ApprovalRecordRepository recordRepository,
                      AuditService auditService) {
        this.flowRepository = flowRepository;
        this.nodeRepository = nodeRepository;
        this.instanceRepository = instanceRepository;
        this.recordRepository = recordRepository;
        this.auditService = auditService;
    }

    // ============================================================
    // 流程定义 CRUD
    // ============================================================
    @Transactional(readOnly = true)
    public List<ApprovalFlowResponse> listFlows() {
        return flowRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ApprovalFlowResponse getFlow(Long id) {
        return toResponse(findFlow(id));
    }

    @Transactional
    public ApprovalFlowResponse createFlow(ApprovalFlowRequest request) {
        validate(request);
        flowRepository.findByCode(request.code().trim()).ifPresent(f -> {
            throw new BusinessException(ErrorCode.CONFLICT, "流程编码已存在: " + request.code());
        });
        ApprovalFlowDef flow = new ApprovalFlowDef();
        apply(flow, request);
        if (Boolean.TRUE.equals(flow.getIsDefault())) {
            clearDefault();
        }
        flow = flowRepository.save(flow);
        saveNodes(flow.getId(), request.nodes());
        auditService.record("BPM_FLOW_CREATED", "APPROVAL_FLOW_DEF", flow.getId().toString(), null,
                "code=%s;nodes=%d".formatted(flow.getCode(), request.nodes().size()));
        return toResponse(flow);
    }

    @Transactional
    public ApprovalFlowResponse updateFlow(Long id, ApprovalFlowRequest request) {
        validate(request);
        ApprovalFlowDef flow = findFlow(id);
        flowRepository.findByCode(request.code().trim())
                .filter(f -> !f.getId().equals(id))
                .ifPresent(f -> {
                    throw new BusinessException(ErrorCode.CONFLICT, "流程编码已存在: " + request.code());
                });
        apply(flow, request);
        if (Boolean.TRUE.equals(flow.getIsDefault())) {
            clearDefaultExcept(id);
        }
        flow = flowRepository.save(flow);
        nodeRepository.deleteByFlowDefId(id);
        saveNodes(id, request.nodes());
        auditService.record("BPM_FLOW_UPDATED", "APPROVAL_FLOW_DEF", id.toString(), null,
                "code=%s;nodes=%d".formatted(flow.getCode(), request.nodes().size()));
        return toResponse(flow);
    }

    @Transactional
    public void deleteFlow(Long id) {
        ApprovalFlowDef flow = findFlow(id);
        if (Boolean.TRUE.equals(flow.getIsDefault())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "默认审批流模板不可删除");
        }
        nodeRepository.deleteByFlowDefId(id);
        flowRepository.delete(flow);
        auditService.record("BPM_FLOW_DELETED", "APPROVAL_FLOW_DEF", id.toString(), flow.getCode(), null);
    }

    // ============================================================
    // 流程追踪时间线
    // ============================================================
    @Transactional(readOnly = true)
    public ApprovalTimelineResponse timeline(Long instanceId) {
        ApprovalInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "审批流程不存在"));

        // 实例绑定的流定义；未绑定（历史实例）回退默认模板
        ApprovalFlowDef flow = null;
        if (instance.getFlowDefId() != null) {
            flow = flowRepository.findById(instance.getFlowDefId()).orElse(null);
        }
        if (flow == null) {
            flow = flowRepository.findFirstByIsDefaultTrueAndEnabledTrue().orElse(null);
        }
        List<ApprovalNodeDef> nodeDefs = flow == null ? List.of() : nodeRepository.findByFlowDefIdOrderBySeqAsc(flow.getId());

        // 已经过的节点 = 出现过 APPROVE 决策的节点编码
        List<ApprovalRecord> records = recordRepository.findByInstanceIdOrderByOperatedAtAsc(instanceId);
        Map<String, Long> approvedNodes = records.stream()
                .filter(r -> "APPROVE".equals(r.getDecision().name()))
                .collect(Collectors.groupingBy(ApprovalRecord::getNodeCode, Collectors.counting()));

        List<ApprovalTimelineResponse.TimelineNode> nodes = new ArrayList<>();
        for (ApprovalNodeDef def : nodeDefs) {
            boolean current = def.getNodeCode().equals(instance.getCurrentNode());
            boolean passed = approvedNodes.containsKey(def.getNodeCode());
            nodes.add(new ApprovalTimelineResponse.TimelineNode(def.getNodeCode(), def.getNodeName(),
                    def.getSeq(), def.getApproverRole(), current, passed));
        }
        List<ApprovalTimelineResponse.TimelineEvent> events = records.stream()
                .map(r -> new ApprovalTimelineResponse.TimelineEvent(r.getNodeCode(), r.getDecision().name(),
                        r.getCommentText(), r.getOperatorId(), r.getOperatedAt()))
                .toList();

        return new ApprovalTimelineResponse(instance.getId(), instance.getScenarioId(),
                instance.getStatus().name(), instance.getCurrentNode(),
                flow == null ? null : flow.getCode(), flow == null ? null : flow.getName(),
                nodes, events, instance.getCreatedAt(), instance.getUpdatedAt());
    }

    // ============================================================
    // 内部
    // ============================================================
    private ApprovalFlowDef findFlow(Long id) {
        return flowRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "审批流定义不存在"));
    }

    private void validate(ApprovalFlowRequest request) {
        List<Integer> seqs = request.nodes().stream().map(ApprovalFlowRequest.NodeSpec::seq).sorted().toList();
        for (int i = 0; i < seqs.size(); i++) {
            if (seqs.get(i) != i + 1) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "节点顺序必须从 1 连续递增");
            }
        }
        long distinct = request.nodes().stream().map(ApprovalFlowRequest.NodeSpec::nodeCode).distinct().count();
        if (distinct != request.nodes().size()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "节点编码不可重复");
        }
    }

    private void apply(ApprovalFlowDef flow, ApprovalFlowRequest request) {
        flow.setCode(request.code().trim());
        flow.setName(request.name().trim());
        flow.setDescription(request.description());
        flow.setIsDefault(Boolean.TRUE.equals(request.isDefault()));
        flow.setEnabled(request.enabled() == null || request.enabled());
    }

    private void saveNodes(Long flowId, List<ApprovalFlowRequest.NodeSpec> nodes) {
        for (ApprovalFlowRequest.NodeSpec spec : nodes) {
            ApprovalNodeDef node = new ApprovalNodeDef();
            node.setFlowDefId(flowId);
            node.setNodeCode(spec.nodeCode().trim());
            node.setNodeName(spec.nodeName().trim());
            node.setSeq(spec.seq());
            node.setApproverRole(spec.approverRole().trim());
            node.setConditionExpr(spec.conditionExpr());
            nodeRepository.save(node);
        }
    }

    private void clearDefault() {
        flowRepository.findAll().forEach(f -> {
            if (Boolean.TRUE.equals(f.getIsDefault())) {
                f.setIsDefault(false);
                flowRepository.save(f);
            }
        });
    }

    private void clearDefaultExcept(Long id) {
        flowRepository.findAll().forEach(f -> {
            if (!f.getId().equals(id) && Boolean.TRUE.equals(f.getIsDefault())) {
                f.setIsDefault(false);
                flowRepository.save(f);
            }
        });
    }

    private ApprovalFlowResponse toResponse(ApprovalFlowDef flow) {
        List<ApprovalFlowResponse.NodeView> nodes = nodeRepository.findByFlowDefIdOrderBySeqAsc(flow.getId())
                .stream()
                .map(n -> new ApprovalFlowResponse.NodeView(n.getId(), n.getNodeCode(), n.getNodeName(),
                        n.getSeq(), n.getApproverRole(), n.getConditionExpr()))
                .toList();
        return new ApprovalFlowResponse(flow.getId(), flow.getCode(), flow.getName(), flow.getDescription(),
                Boolean.TRUE.equals(flow.getIsDefault()), Boolean.TRUE.equals(flow.getEnabled()),
                nodes, flow.getCreatedAt(), flow.getUpdatedAt());
    }
}

package com.sis.iids.bpm;

import java.time.LocalDateTime;
import java.util.List;

/**
 * R-14 审批流程追踪时间线（FR-04-03）：实例状态 + 逐节点操作记录 + 当前节点在流定义中的位置。
 */
public record ApprovalTimelineResponse(Long instanceId,
                                       Long scenarioId,
                                       String status,
                                       String currentNode,
                                       String flowCode,
                                       String flowName,
                                       List<TimelineNode> flowNodes,
                                       List<TimelineEvent> events,
                                       LocalDateTime createdAt,
                                       LocalDateTime updatedAt) {

    /** 流定义节点（含当前位置标记） */
    public record TimelineNode(String nodeCode, String nodeName, Integer seq,
                               String approverRole, boolean current, boolean passed) {
    }

    /** 操作事件（提交/通过/驳回 + 时间 + 意见） */
    public record TimelineEvent(String nodeCode, String decision, String commentText,
                                Long operatorId, LocalDateTime operatedAt) {
    }
}

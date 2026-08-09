package com.sis.iids.bpm;

import java.time.LocalDateTime;
import java.util.List;

/**
 * R-14 审批流定义响应（FR-04-03）。
 */
public record ApprovalFlowResponse(Long id,
                                   String code,
                                   String name,
                                   String description,
                                   boolean isDefault,
                                   boolean enabled,
                                   List<NodeView> nodes,
                                   LocalDateTime createdAt,
                                   LocalDateTime updatedAt) {

    public record NodeView(Long id, String nodeCode, String nodeName, Integer seq,
                           String approverRole, String conditionExpr) {
    }
}

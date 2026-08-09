package com.sis.iids.bpm;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * R-14 审批流定义请求（FR-04-03，管理员配置）。
 */
public record ApprovalFlowRequest(@NotBlank String code,
                                  @NotBlank String name,
                                  String description,
                                  Boolean isDefault,
                                  Boolean enabled,
                                  @NotEmpty @Valid List<NodeSpec> nodes) {

    /**
     * @param nodeCode      节点编码（如 REVIEW / APPROVAL / COMMITTEE）
     * @param nodeName      节点名称
     * @param seq           顺序（从 1 递增）
     * @param approverRole  审批角色编码
     * @param conditionExpr 条件规则（预留，如"参数调整 >±5% 升级投委会"）
     */
    public record NodeSpec(@NotBlank String nodeCode,
                           @NotBlank String nodeName,
                           @NotNull Integer seq,
                           @NotBlank String approverRole,
                           String conditionExpr) {
    }
}

package com.sis.iids.bpm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * R-14 审批节点定义（FR-04-03）：节点角色、顺序与条件规则（condition_expr 预留，如"参数调整 >±5% 升级投委会"）。
 */
@Entity
@Table(name = "approval_node_def")
public class ApprovalNodeDef {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "flow_def_id", nullable = false)
    private Long flowDefId;
    @Column(name = "node_code", nullable = false, length = 64)
    private String nodeCode;
    @Column(name = "node_name", nullable = false)
    private String nodeName;
    @Column(nullable = false)
    private Integer seq;
    @Column(name = "approver_role", nullable = false, length = 64)
    private String approverRole;
    @Column(name = "condition_expr", length = 1000)
    private String conditionExpr;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFlowDefId() { return flowDefId; }
    public void setFlowDefId(Long flowDefId) { this.flowDefId = flowDefId; }
    public String getNodeCode() { return nodeCode; }
    public void setNodeCode(String nodeCode) { this.nodeCode = nodeCode; }
    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }
    public Integer getSeq() { return seq; }
    public void setSeq(Integer seq) { this.seq = seq; }
    public String getApproverRole() { return approverRole; }
    public void setApproverRole(String approverRole) { this.approverRole = approverRole; }
    public String getConditionExpr() { return conditionExpr; }
    public void setConditionExpr(String conditionExpr) { this.conditionExpr = conditionExpr; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

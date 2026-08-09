package com.sis.iids.dashboard;

import java.math.BigDecimal;
import java.util.List;

/**
 * BI 仪表盘聚合视图（FR-04-01）。只读聚合，不落库。
 *
 * @param kpis            组合级 KPI（项目数/加权 IRR/总 NPV/预警计数）
 * @param bubbles         NPV-IRR 气泡（大小 = 总投资额）
 * @param stageCounts     项目阶段分布
 * @param industryAmounts 行业分布（按总投资额）
 * @param riskSignals     风险信号灯（R-12 落地前为占位演示规则）
 * @param todos           待办与审批（在途审批实例）
 */
public record DashboardSummary(Kpis kpis,
                               List<BubblePoint> bubbles,
                               List<NameValue> stageCounts,
                               List<NameValue> industryAmounts,
                               List<RiskSignal> riskSignals,
                               List<TodoItem> todos) {

    public record Kpis(long projectCount,
                       BigDecimal weightedIrr,
                       BigDecimal totalNpv,
                       long warningCount) {
    }

    public record BubblePoint(Long scenarioId, String scenarioName, String projectName,
                              BigDecimal npv, BigDecimal irr, BigDecimal investment) {
    }

    public record NameValue(String name, BigDecimal value) {
    }

    public record RiskSignal(String variable, String currentValue, String level, String note) {
    }

    public record TodoItem(Long instanceId, Long scenarioId, String scenarioName, String projectName,
                           String currentNode, String status, String updatedAt) {
    }
}

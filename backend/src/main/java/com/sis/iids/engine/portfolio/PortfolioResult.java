package com.sis.iids.engine.portfolio;

import java.math.BigDecimal;
import java.util.List;

/**
 * 组合优化结果（FR-03-02）。
 *
 * @param selectedScenarioIds 入选方案 ID（按 NPV 降序）
 * @param totalNpv            组合总 NPV
 * @param totalInvestment     组合总投资
 * @param budget              资金池约束
 * @param maxCount            数量上限约束（null = 不限）
 * @param explanation         求解解释（约束松紧与边际价值说明）
 * @param frontier            帕累托前沿点（按预算档位扫描：预算 → 该预算下最优组合 NPV）
 */
public record PortfolioResult(List<Long> selectedScenarioIds,
                              BigDecimal totalNpv,
                              BigDecimal totalInvestment,
                              BigDecimal budget,
                              Integer maxCount,
                              String explanation,
                              List<FrontierPoint> frontier) {

    /** 帕累托前沿点：预算上限 → 最优组合 NPV/投资 */
    public record FrontierPoint(BigDecimal budget, BigDecimal npv, BigDecimal investment, int count) {
    }
}

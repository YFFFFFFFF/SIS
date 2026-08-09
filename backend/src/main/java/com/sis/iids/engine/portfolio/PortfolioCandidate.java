package com.sis.iids.engine.portfolio;

import java.math.BigDecimal;

/**
 * 组合优化候选方案（FR-03-02）。
 *
 * @param scenarioId   方案 ID
 * @param scenarioName 方案名称
 * @param projectName  所属项目名
 * @param npv          NPV（目标函数价值系数）
 * @param investment   总投资（资金约束消耗系数）
 * @param irr          IRR（组合报告展示用，可空）
 */
public record PortfolioCandidate(Long scenarioId,
                                 String scenarioName,
                                 String projectName,
                                 BigDecimal npv,
                                 BigDecimal investment,
                                 BigDecimal irr) {
}

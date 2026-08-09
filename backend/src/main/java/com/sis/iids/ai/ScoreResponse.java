package com.sis.iids.ai;

import java.math.BigDecimal;
import java.util.List;

/**
 * R-17 智能打分响应（FR-05-03）：总分 + 标签 + 逐因子明细（可解释，不替代人工决策）。
 */
public record ScoreResponse(Long scenarioId,
                            String modelCode,
                            String modelVersion,
                            BigDecimal totalScore,
                            String label,
                            String disclaimer,
                            List<FactorScore> factors) {

    /**
     * @param factor     因子编码
     * @param name       因子名
     * @param rawValue   原始取值（展示用）
     * @param score      因子得分（0~100）
     * @param weight     权重
     * @param weighted   加权得分
     * @param explain    打分依据
     */
    public record FactorScore(String factor,
                              String name,
                              String rawValue,
                              BigDecimal score,
                              BigDecimal weight,
                              BigDecimal weighted,
                              String explain) {
    }
}

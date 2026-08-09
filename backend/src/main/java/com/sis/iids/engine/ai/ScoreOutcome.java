package com.sis.iids.engine.ai;

import java.math.BigDecimal;
import java.util.List;

/**
 * R-17 打分结果（FR-05-03）。
 */
public record ScoreOutcome(BigDecimal totalScore,
                           String label,
                           List<FactorScore> factors) {

    public record FactorScore(String factor,
                              String name,
                              String rawValue,
                              BigDecimal score,
                              BigDecimal weight,
                              BigDecimal weighted,
                              String explain) {
    }
}

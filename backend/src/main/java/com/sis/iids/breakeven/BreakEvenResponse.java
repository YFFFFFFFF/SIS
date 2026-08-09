package com.sis.iids.breakeven;

import java.math.BigDecimal;
import java.util.List;

/**
 * 盈亏平衡分析响应（FR-02-02）。
 */
public record BreakEvenResponse(Long scenarioId,
                                BigDecimal pricePerUnit,
                                BigDecimal annualOutput,
                                BigDecimal unitVariableCost,
                                BigDecimal annualFixedCost,
                                BigDecimal bepOutput,
                                BigDecimal bepUtilization,
                                BigDecimal bepPrice,
                                BigDecimal contributionMargin,
                                boolean solvable,
                                String unsolvableReason,
                                List<CurvePointView> curve,
                                String assumptionNote) {

    public record CurvePointView(BigDecimal output, BigDecimal revenue, BigDecimal totalCost) {
    }
}

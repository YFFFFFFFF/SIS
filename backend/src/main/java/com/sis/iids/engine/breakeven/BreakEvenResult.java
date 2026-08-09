package com.sis.iids.engine.breakeven;

import java.math.BigDecimal;
import java.util.List;

/**
 * 盈亏平衡分析结果（FR-02-02）。
 * 三口径：BEP 产量 / 产能利用率 / 盈亏平衡售价；另附盈亏平衡图数据点。
 *
 * @param pricePerUnit         达产年单价
 * @param annualOutput         达产年设计产量
 * @param unitVariableCost     单位可变成本
 * @param annualFixedCost      达产年固定成本（固定经营成本 + 折旧 + 摊销，税前口径）
 * @param bepOutput            盈亏平衡产量（固定成本 ÷ 单位边际贡献）
 * @param bepUtilization       盈亏平衡产能利用率（bepOutput ÷ annualOutput）
 * @param bepPrice             盈亏平衡售价（unitVariableCost + 固定成本 ÷ annualOutput）
 * @param contributionMargin   单位边际贡献（price − unitVariableCost）
 * @param solvable             是否可解（单位边际贡献 > 0 且产量 > 0）
 * @param unsolvableReason     不可解原因（solvable=false 时填充）
 * @param curve                盈亏平衡图数据点（产量 → 收入/总成本）
 * @param assumptionNote       适用边界与假设声明
 */
public record BreakEvenResult(BigDecimal pricePerUnit,
                              BigDecimal annualOutput,
                              BigDecimal unitVariableCost,
                              BigDecimal annualFixedCost,
                              BigDecimal bepOutput,
                              BigDecimal bepUtilization,
                              BigDecimal bepPrice,
                              BigDecimal contributionMargin,
                              boolean solvable,
                              String unsolvableReason,
                              List<CurvePoint> curve,
                              String assumptionNote) {

    /** 盈亏平衡图数据点：某一产量水平下的收入与总成本 */
    public record CurvePoint(BigDecimal output, BigDecimal revenue, BigDecimal totalCost) {
    }
}

package com.sis.iids.engine.breakeven;

import com.sis.iids.engine.financial.FinancialInput;
import com.sis.iids.engine.financial.InvestmentEntry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * R-10 盈亏平衡引擎测试（FR-02-02）。
 */
class BreakEvenEngineTest {

    private final BreakEvenEngine engine = new BreakEvenEngine();

    /** 与 R-02 基准 fixture 同构：建设 20 万 + 流动资金 2 万，全资本金，price=140/unit=40/output=1000/fixed=10000 */
    private FinancialInput baseInput() {
        FinancialInput input = new FinancialInput();
        input.setConstructionYears(1);
        input.setOperationYears(5);
        input.setWacc(new BigDecimal("0.10"));
        input.setTaxRate(new BigDecimal("0.25"));
        input.setDepreciationYears(5);
        input.setResidualRate(BigDecimal.ZERO);
        input.setPricePerUnit(new BigDecimal("140"));
        input.setAnnualOutput(new BigDecimal("1000"));
        input.setConstructionEntries(List.of(new InvestmentEntry("CONSTRUCTION", "建设投资", new BigDecimal("200000"))));
        input.setConstructionSchedule(List.of(BigDecimal.ONE));
        input.setWorkingCapital(new BigDecimal("20000"));
        input.setCostEntries(List.of(
                new com.sis.iids.engine.financial.CostEntry("RAW_MATERIAL", "外购原材料", 0, new BigDecimal("40000")),
                new com.sis.iids.engine.financial.CostEntry("LABOR_MANUFACTURING", "人工及制造费用", 0, new BigDecimal("10000"))));
        input.setUnitVariableCost(new BigDecimal("40"));
        input.setEquityRatio(BigDecimal.ONE);
        return input;
    }

    @Test
    void solvesThreeBreakEvenMeasures() {
        BreakEvenResult r = engine.analyze(baseInput());

        assertThat(r.solvable()).isTrue();
        // 单位边际贡献 = 140 − 40 = 100
        assertThat(r.contributionMargin()).isEqualByComparingTo("100");
        // 年固定成本 = 固定经营 10000 + 折旧 200000/5 = 40000（直线法，残值 0）→ 50000
        assertThat(r.annualFixedCost()).isEqualByComparingTo("50000");
        // BEP 产量 = 50000 / 100 = 500
        assertThat(r.bepOutput()).isEqualByComparingTo("500");
        // 产能利用率 = 500 / 1000 = 0.5
        assertThat(r.bepUtilization()).isEqualByComparingTo("0.5");
        // 盈亏平衡售价 = 40 + 50000/1000 = 90
        assertThat(r.bepPrice()).isEqualByComparingTo("90");
        assertThat(r.curve()).hasSize(11);
        assertThat(r.assumptionNote()).isNotBlank();
    }

    @Test
    void curveCrossesAtBreakEven() {
        BreakEvenResult r = engine.analyze(baseInput());
        // 在 BEP 产量 500 处：收入 = 140×500=70000，总成本 = 50000 + 40×500 = 70000
        BreakEvenResult.CurvePoint atBep = null;
        for (BreakEvenResult.CurvePoint p : r.curve()) {
            if (p.output().compareTo(new BigDecimal("500.0000")) == 0) {
                atBep = p;
            }
        }
        // 曲线点产量为 0/150/300/.../1500，500 不在网格上；改为验证收入线与成本线在 BEP 两侧大小关系翻转
        assertThat(atBep).isNull();
        BigDecimal below = r.curve().get(3).output(); // 450
        BigDecimal above = r.curve().get(4).output(); // 600
        assertThat(below).isLessThan(new BigDecimal("500"));
        assertThat(above).isGreaterThan(new BigDecimal("500"));
        assertThat(r.curve().get(3).revenue()).isLessThan(r.curve().get(3).totalCost());
        assertThat(r.curve().get(4).revenue()).isGreaterThan(r.curve().get(4).totalCost());
    }

    @Test
    void unsolvableWhenContributionNonPositive() {
        FinancialInput input = baseInput();
        input.setPricePerUnit(new BigDecimal("30")); // 低于单位可变成本 40
        BreakEvenResult r = engine.analyze(input);
        assertThat(r.solvable()).isFalse();
        assertThat(r.unsolvableReason()).contains("边际贡献");
        assertThat(r.bepOutput()).isNull();
        assertThat(r.bepUtilization()).isNull();
        assertThat(r.bepPrice()).isNull();
    }

    @Test
    void unsolvableWhenOutputZero() {
        FinancialInput input = baseInput();
        input.setAnnualOutput(BigDecimal.ZERO);
        input.setUnitVariableCost(BigDecimal.ZERO);
        BreakEvenResult r = engine.analyze(input);
        assertThat(r.solvable()).isFalse();
        assertThat(r.unsolvableReason()).contains("产量为 0");
    }

    @Test
    void fixedCostIncludesDepreciationAndAmortization() {
        FinancialInput input = baseInput();
        input.setAmortizableAmount(new BigDecimal("10000"));
        input.setAmortizationYears(5);
        BreakEvenResult r = engine.analyze(input);
        // 固定成本 = 10000 + 折旧 40000 + 摊销 2000 = 52000
        assertThat(r.annualFixedCost()).isEqualByComparingTo("52000");
        assertThat(r.bepOutput()).isEqualByComparingTo("520");
        assertThat(r.bepPrice()).isCloseTo(new BigDecimal("92"), within(new BigDecimal("0.01")));
    }
}

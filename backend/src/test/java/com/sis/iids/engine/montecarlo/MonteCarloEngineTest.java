package com.sis.iids.engine.montecarlo;

import com.sis.iids.engine.financial.FinancialInput;
import com.sis.iids.engine.financial.InvestmentEntry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * R-11 蒙特卡洛引擎测试（FR-02-03）。
 */
class MonteCarloEngineTest {

    private final MonteCarloEngine engine = new MonteCarloEngine();

    /** R-02 基准 fixture 同构 */
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

    private List<DistributionSpec> priceSpec() {
        return List.of(new DistributionSpec(MonteCarloVariable.PRICE, DistributionSpec.TRIANGULAR,
                new BigDecimal("-0.20"), BigDecimal.ZERO, new BigDecimal("0.20"), null, null));
    }

    @Test
    void seedReproducibility() {
        MonteCarloResult a = engine.run(baseInput(), "NPV", priceSpec(), 2000, 42L);
        MonteCarloResult b = engine.run(baseInput(), "NPV", priceSpec(), 2000, 42L);
        assertThat(a.mean()).isEqualTo(b.mean());
        assertThat(a.p5()).isEqualTo(b.p5());
        assertThat(a.histogram()).isEqualTo(b.histogram());
    }

    @Test
    void differentSeedsDiffer() {
        MonteCarloResult a = engine.run(baseInput(), "NPV", priceSpec(), 2000, 42L);
        MonteCarloResult b = engine.run(baseInput(), "NPV", priceSpec(), 2000, 7L);
        assertThat(a.mean()).isNotEqualTo(b.mean());
    }

    @Test
    void statisticsAreCoherent() {
        MonteCarloResult r = engine.run(baseInput(), "NPV", priceSpec(), 10000, 42L);
        // 基准 NPV = 88022.58，±20% 售价扰动下期望应接近基准、P(>0) 高
        assertThat(r.mean().doubleValue()).isCloseTo(88022.58, within(20000.0));
        assertThat(r.probPositive().doubleValue()).isGreaterThan(0.9);
        assertThat(r.min()).isLessThan(r.p5());
        assertThat(r.p5()).isLessThanOrEqualTo(r.p50());
        assertThat(r.p50()).isLessThanOrEqualTo(r.p95());
        assertThat(r.p95()).isLessThan(r.max());
        assertThat(r.var95()).isEqualTo(r.p5());
        assertThat(r.histogram()).hasSize(20);
        assertThat(r.cumulative()).hasSize(21);
        int histogramTotal = r.histogram().stream().mapToInt(MonteCarloResult.HistogramBucket::count).sum();
        assertThat(histogramTotal).isEqualTo(10000);
    }

    @Test
    void normalDistributionWithinThreeSigma() {
        DistributionSpec spec = new DistributionSpec(MonteCarloVariable.UNIT_COST, DistributionSpec.NORMAL,
                null, null, null, BigDecimal.ZERO, new BigDecimal("0.10"));
        MonteCarloResult r = engine.run(baseInput(), "NPV", List.of(spec), 5000, 42L);
        // 单位成本 ±30%（3σ）截断内 NPV 应保持有限区间
        assertThat(r.min().doubleValue()).isGreaterThan(0);
        assertThat(r.max().doubleValue()).isLessThan(200000);
    }

    @Test
    void rejectsInvalidConfig() {
        assertThatThrownBy(() -> engine.run(baseInput(), "NPV", List.of(), 10000, 1L))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("抽样变量");
        assertThatThrownBy(() -> engine.run(baseInput(), "NPV", priceSpec(), 100, 1L))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("抽样次数");
        assertThatThrownBy(() -> new DistributionSpec(MonteCarloVariable.PRICE, "GAMMA",
                null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("分布类型");
        assertThatThrownBy(() -> new DistributionSpec(MonteCarloVariable.PRICE, DistributionSpec.TRIANGULAR,
                new BigDecimal("0.3"), BigDecimal.ZERO, new BigDecimal("0.2"), null, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("min ≤ mode ≤ max");
    }

    @Test
    void performanceWithinBudget() {
        // PRD 性能约束：≥10000 次抽样 ≤ 1 分钟。引擎单次重算约亚毫秒级，10000 次应在数秒内完成。
        long start = System.currentTimeMillis();
        engine.run(baseInput(), "NPV", priceSpec(), 10000, 42L);
        long elapsed = System.currentTimeMillis() - start;
        assertThat(elapsed).isLessThan(60000);
    }
}

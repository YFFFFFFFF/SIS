package com.sis.iids.engine.ai;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * R-17 打分引擎测试（FR-05-03）。
 */
class ScoringEngineTest {

    private final ScoringEngine engine = new ScoringEngine();

    private ScoreFeatures strongFeatures() {
        return new ScoreFeatures(
                new BigDecimal("88022.58"), new BigDecimal("0.2477"), new BigDecimal("0.10"),
                new BigDecimal("2.84"), 5, "MEDIUM", new BigDecimal("0.5"), null);
    }

    @Test
    void strongScenarioScoresHigh() {
        ScoreOutcome outcome = engine.score(strongFeatures(), null);
        // NPV 88022 → 70×0.25=17.5；IRR 溢价 14.77pct → 100×0.25=25；回收期 2.84/5=56.8% → 72×0.15≈10.8；
        // MEDIUM → 60×0.15=9；BEP 50% → 100×0.10=10；无复盘 → 60×0.10=6；合计 ≈ 78.3
        assertThat(outcome.totalScore()).isGreaterThan(new BigDecimal("70"));
        assertThat(outcome.label()).isEqualTo(ScoringEngine.LABEL_RECOMMEND);
        assertThat(outcome.factors()).hasSize(6);
    }

    @Test
    void negativeNpvScoresLow() {
        ScoreFeatures weak = new ScoreFeatures(
                new BigDecimal("-50000"), null, new BigDecimal("0.10"),
                null, 5, "HIGH", new BigDecimal("1.2"), new BigDecimal("-0.35"));
        ScoreOutcome outcome = engine.score(weak, null);
        assertThat(outcome.totalScore()).isLessThan(new BigDecimal("50"));
        assertThat(outcome.label()).isEqualTo(ScoringEngine.LABEL_HOLD);
    }

    @Test
    void midRangeIsCaution() {
        ScoreFeatures mid = new ScoreFeatures(
                new BigDecimal("30000"), new BigDecimal("0.11"), new BigDecimal("0.10"),
                new BigDecimal("3.5"), 5, "MEDIUM", new BigDecimal("0.7"), new BigDecimal("0.10"));
        ScoreOutcome outcome = engine.score(mid, null);
        assertThat(outcome.totalScore()).isBetween(new BigDecimal("30"), new BigDecimal("80"));
    }

    @Test
    void factorWeightsRespected() {
        ScoreOutcome outcome = engine.score(strongFeatures(),
                Map.of("npv", new BigDecimal("1.0"), "irrSpread", BigDecimal.ZERO, "payback", BigDecimal.ZERO,
                        "sensitivity", BigDecimal.ZERO, "bepUtilization", BigDecimal.ZERO, "reviewDeviation", BigDecimal.ZERO));
        // 全部权重给 NPV（70 分）→ 总分 70
        assertThat(outcome.totalScore()).isEqualByComparingTo("70.00");
        assertThat(outcome.label()).isEqualTo(ScoringEngine.LABEL_RECOMMEND);
    }

    @Test
    void everyFactorHasExplanation() {
        ScoreOutcome outcome = engine.score(strongFeatures(), null);
        for (ScoreOutcome.FactorScore f : outcome.factors()) {
            assertThat(f.explain()).isNotBlank();
            assertThat(f.score()).isBetween(BigDecimal.ZERO, new BigDecimal("100"));
            assertThat(f.weighted()).isNotNull();
        }
    }
}

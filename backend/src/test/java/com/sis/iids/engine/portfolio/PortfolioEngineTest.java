package com.sis.iids.engine.portfolio;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * R-13 组合优化引擎测试（FR-03-02）。
 */
class PortfolioEngineTest {

    private final PortfolioEngine engine = new PortfolioEngine();

    private List<PortfolioCandidate> candidates() {
        return List.of(
                new PortfolioCandidate(1L, "方案A", "项目甲", new BigDecimal("80000"), new BigDecimal("100000"), new BigDecimal("0.20")),
                new PortfolioCandidate(2L, "方案B", "项目乙", new BigDecimal("120000"), new BigDecimal("200000"), new BigDecimal("0.18")),
                new PortfolioCandidate(3L, "方案C", "项目丙", new BigDecimal("50000"), new BigDecimal("60000"), new BigDecimal("0.15")),
                new PortfolioCandidate(4L, "方案D", "项目丁", new BigDecimal("90000"), new BigDecimal("150000"), new BigDecimal("0.12")));
    }

    @Test
    void selectsBestSubsetWithinBudget() {
        // 预算 260000：A+B=300000 超支；B+C=260000 NPV=170000；A+C+D=310000 超支；A+B? 不行。最优 = B+C (170000)？A+D=250000 NPV=170000 并列——oj! 取其一，均为 170000
        PortfolioResult r = engine.optimize(candidates(), new BigDecimal("260000"), null);
        assertThat(r.totalNpv()).isEqualByComparingTo("170000");
        assertThat(r.totalInvestment().doubleValue()).isLessThanOrEqualTo(260000.0);
        assertThat(r.selectedScenarioIds()).hasSize(2);
        assertThat(r.explanation()).contains("资金利用率");
        assertThat(r.frontier()).hasSize(21);
    }

    @Test
    void respectsMaxCount() {
        // 预算充裕 400000 但限 1 个 → 选 NPV 最大的 B（120000）
        PortfolioResult r = engine.optimize(candidates(), new BigDecimal("400000"), 1);
        assertThat(r.selectedScenarioIds()).containsExactly(2L);
        assertThat(r.totalNpv()).isEqualByComparingTo("120000");
    }

    @Test
    void fullBudgetSelectsAll() {
        PortfolioResult r = engine.optimize(candidates(), new BigDecimal("1000000"), null);
        assertThat(r.selectedScenarioIds()).containsExactlyInAnyOrder(1L, 2L, 3L, 4L);
        assertThat(r.totalNpv()).isEqualByComparingTo("340000");
    }

    @Test
    void frontierIsMonotoneNonDecreasing() {
        PortfolioResult r = engine.optimize(candidates(), new BigDecimal("400000"), null);
        BigDecimal prev = null;
        for (PortfolioResult.FrontierPoint p : r.frontier()) {
            if (prev != null) {
                assertThat(p.npv()).isGreaterThanOrEqualTo(prev);
            }
            prev = p.npv();
        }
        // 满预算档 NPV 应等于最优解
        assertThat(r.frontier().get(20).npv()).isEqualByComparingTo(r.totalNpv());
    }

    @Test
    void rejectsInvalidInput() {
        assertThatThrownBy(() -> engine.optimize(List.of(), new BigDecimal("100"), null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("候选方案");
        assertThatThrownBy(() -> engine.optimize(candidates(), BigDecimal.ZERO, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("资金池");
    }
}

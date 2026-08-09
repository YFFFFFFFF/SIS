package com.sis.iids.engine.reverse;

import com.sis.iids.engine.financial.CostEntry;
import com.sis.iids.engine.financial.DepreciationPolicy;
import com.sis.iids.engine.financial.FinancialInput;
import com.sis.iids.engine.financial.InvestmentEntry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 目标反算引擎（FR-01-05）单元测试。基准：全资本金工业项目（复用 R-02 CASE 结构）。
 */
class ReverseEngineTest {

    private FinancialInput base() {
        FinancialInput input = new FinancialInput();
        input.setConstructionYears(1);
        input.setOperationYears(5);
        input.setWacc(new BigDecimal("0.10"));
        input.setTaxRate(new BigDecimal("0.25"));
        input.setDepreciationPolicy(DepreciationPolicy.STRAIGHT_LINE);
        input.setDepreciationYears(5);
        input.setResidualRate(BigDecimal.ZERO);
        input.setEquityRatio(BigDecimal.ONE);
        input.setConstructionEntries(List.of(new InvestmentEntry("CONSTRUCTION", "建设投资", new BigDecimal("200000"))));
        input.setConstructionSchedule(List.of(BigDecimal.ONE));
        input.setWorkingCapital(new BigDecimal("20000"));
        input.setPricePerUnit(new BigDecimal("140"));
        input.setAnnualOutput(new BigDecimal("1000"));
        input.setUnitVariableCost(new BigDecimal("40"));
        input.setCostEntries(List.of(
                new CostEntry("RAW_MATERIAL", "外购原材料及燃料动力", 0, new BigDecimal("40000")),
                new CostEntry("LABOR_MANUFACTURING", "人工及制造费用", 0, new BigDecimal("10000"))));
        return input;
    }

    @Test
    void solvesPriceForTargetNpv() {
        // 目标 NPV=0 → 反算盈亏平衡售价，应低于基准售价 140
        ReverseResult r = new ReverseEngine().solve(base(), ReverseTarget.NPV, BigDecimal.ZERO, ReverseVariable.PRICE);

        assertThat(r.feasible()).isTrue();
        assertThat(r.solvedValue()).isLessThan(new BigDecimal("140"));
        // 达成值应收敛到目标值附近
        assertThat(r.achievedValue()).isNotNull();
        assertThat(r.achievedValue().subtract(BigDecimal.ZERO).abs())
                .isLessThan(new BigDecimal("1"));
        assertThat(r.sensitivityNote()).contains("产品售价");
        assertThat(r.boundaryNote()).contains("适用边界与假设");
        assertThat(r.iterations()).isGreaterThan(3);
    }

    @Test
    void solvesInvestmentForTargetIrr() {
        // 目标 IRR=0.10（=WACC）→ 反算可承受的最高建设投资，应高于基准 200000
        ReverseResult r = new ReverseEngine().solve(base(), ReverseTarget.IRR,
                new BigDecimal("0.10"), ReverseVariable.INVESTMENT);

        assertThat(r.feasible()).isTrue();
        assertThat(r.solvedValue()).isGreaterThan(new BigDecimal("200000"));
        assertThat(r.achievedValue()).isNotNull();
        assertThat(r.achievedValue().subtract(new BigDecimal("0.10")).abs())
                .isLessThanOrEqualTo(new BigDecimal("0.01"));
    }

    @Test
    void infeasibleWhenTargetOutOfReach() {
        // 目标 IRR=0.99 极端高，靠降成本（下限 1%）也达不到 → 不可行并给边界说明
        ReverseResult r = new ReverseEngine().solve(base(), ReverseTarget.IRR,
                new BigDecimal("0.99"), ReverseVariable.UNIT_COST);

        assertThat(r.feasible()).isFalse();
        assertThat(r.solvedValue()).isNull();
        assertThat(r.boundaryNote()).contains("适用边界与假设");
    }

    @Test
    void rejectsBlankVariable() {
        assertThrows(IllegalArgumentException.class, () -> ReverseVariable.from("NOPE"));
        assertThrows(IllegalArgumentException.class, () -> ReverseTarget.from(""));
    }

    @Test
    void solvedPaybackTarget() {
        // 目标回收期 3 年 → 反算所需售价（应高于基准 140 以加速回收）
        ReverseResult r = new ReverseEngine().solve(base(), ReverseTarget.STATIC_PAYBACK_YEARS,
                new BigDecimal("3"), ReverseVariable.PRICE);

        assertThat(r.feasible()).isTrue();
        assertThat(r.solvedValue()).isGreaterThan(new BigDecimal("140"));
        assertThat(r.achievedValue()).isNotNull();
    }
}

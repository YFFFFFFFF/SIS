package com.sis.iids.engine.sensitivity;

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
 * 敏感性引擎（FR-02-01）单元测试。
 * 基准：全资本金工业项目（复用 R-02 CASE 结构）。
 */
class SensitivityEngineTest {

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
    void singleFactorPriceSensitivity() {
        SensitivityResult r = new SensitivityEngine().analyze(base(), "NPV",
                new FactorSpec(SensitivityVariable.PRICE, new BigDecimal("0.60"), 13), null);

        assertThat(r.factor2()).isNull();
        assertThat(r.matrix()).hasSize(13);
        assertThat(r.baseValue()).isGreaterThan(BigDecimal.ZERO);
        // 售价是首要敏感因素，敏感系数显著 > 1
        assertThat(r.coefficient1()).isGreaterThan(BigDecimal.ONE);
        assertThat(r.level1()).isEqualTo("HIGH");
        // 售价下浮存在 NPV=0 的临界值
        assertThat(r.criticalFactor1()).isNotNull();
        assertThat(r.criticalFactor1()).isBetween(new BigDecimal("-0.60"), BigDecimal.ZERO);
        // 基准点（factor=0）指标值 = 基准值
        SensitivityResult.SensitivityCell baseCell = r.matrix().stream()
                .filter(c -> c.factor1().signum() == 0).findFirst().orElseThrow();
        assertThat(baseCell.metricValue()).isEqualByComparingTo(r.baseValue());
    }

    @Test
    void twoFactorHeatmapMatrix() {
        SensitivityResult r = new SensitivityEngine().analyze(base(), "NPV",
                new FactorSpec(SensitivityVariable.PRICE, new BigDecimal("0.20"), 5),
                new FactorSpec(SensitivityVariable.UNIT_COST, new BigDecimal("0.20"), 5));

        assertThat(r.matrix()).hasSize(25);
        assertThat(r.coefficient1()).isNotNull();
        assertThat(r.coefficient2()).isNotNull();
        // 售价敏感度高于单位成本
        assertThat(r.coefficient1().abs()).isGreaterThan(r.coefficient2().abs());
        // 左上角（售价 -20% × 成本 -20%）
        SensitivityResult.SensitivityCell corner = r.matrix().stream()
                .filter(c -> c.factor1().compareTo(new BigDecimal("-0.2")) == 0
                        && c.factor2().compareTo(new BigDecimal("-0.2")) == 0)
                .findFirst().orElseThrow();
        assertThat(corner.metricValue()).isNotNull();
    }

    @Test
    void rejectsEvenSteps() {
        assertThrows(IllegalArgumentException.class,
                () -> new FactorSpec(SensitivityVariable.PRICE, new BigDecimal("0.2"), 4));
    }

    @Test
    void rejectsUnknownVariable() {
        assertThrows(IllegalArgumentException.class, () -> SensitivityVariable.from("NOPE"));
    }

    @Test
    void costUpReducesNpv() {
        // 单位成本上浮 → NPV 下降（单调性）
        SensitivityResult r = new SensitivityEngine().analyze(base(), "NPV",
                new FactorSpec(SensitivityVariable.UNIT_COST, new BigDecimal("0.20"), 5), null);
        BigDecimal down = r.matrix().get(0).metricValue();          // -20%
        BigDecimal up = r.matrix().get(4).metricValue();            // +20%
        assertThat(up).isLessThan(down);
    }
}

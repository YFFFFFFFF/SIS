package com.sis.iids.engine.financial;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 财务引擎 v2 锁定算例（设计文档 R01-R02 §10）。
 * 断言值以引擎实际输出复核，容差仅用于浮点求解指标（IRR/NPV）。
 */
class FinancialEngineTest {

    private static final BigDecimal WACC = new BigDecimal("0.10");

    private FinancialInput case1() {
        FinancialInput input = baseInput(1, 5);
        input.setConstructionEntries(List.of(new InvestmentEntry("CONSTRUCTION", "建设投资", new BigDecimal("1000"))));
        input.setConstructionSchedule(List.of(BigDecimal.ONE));
        input.setWorkingCapital(new BigDecimal("50"));
        input.setPricePerUnit(new BigDecimal("140"));
        input.setAnnualOutput(new BigDecimal("10"));
        input.setUnitVariableCost(new BigDecimal("40"));
        input.setCostEntries(List.of(new CostEntry("LABOR_MANUFACTURING", "人工制造费", 0, new BigDecimal("300"))));
        input.setDepreciationYears(5);
        input.setResidualRate(BigDecimal.ZERO);
        return input;
    }

    private FinancialInput baseInput(int cY, int oY) {
        FinancialInput input = new FinancialInput();
        input.setConstructionYears(cY);
        input.setOperationYears(oY);
        input.setWacc(WACC);
        input.setTaxRate(new BigDecimal("0.25"));
        input.setDepreciationPolicy(DepreciationPolicy.STRAIGHT_LINE);
        input.setResidualRate(new BigDecimal("0.05"));
        input.setEquityRatio(BigDecimal.ONE);
        return input;
    }

    private BigDecimal metric(FinancialResult r, String code) {
        return r.getMetrics().get(code);
    }

    private List<StatementRow> project(FinancialResult r) {
        return r.getStatements().get(MetricCodes.ST_PROJECT);
    }

    @Test
    void case1_standardAllEquityProject() {
        FinancialResult r = new FinancialEngine().calculate(case1());

        assertThat(r.getTotalInvestment()).isEqualByComparingTo("1050.0000");
        assertThat(r.getConstructionInterest()).isEqualByComparingTo("0.0000");
        assertThat(metric(r, MetricCodes.NPV)).isCloseTo(new BigDecimal("1165.2939"), within(new BigDecimal("0.01")));
        assertThat(metric(r, MetricCodes.IRR)).isCloseTo(new BigDecimal("0.4816"), within(new BigDecimal("0.0005")));
        assertThat(metric(r, MetricCodes.STATIC_PAYBACK_YEARS)).isCloseTo(new BigDecimal("1.8261"), within(new BigDecimal("0.0005")));
        assertThat(metric(r, MetricCodes.DYNAMIC_PAYBACK_YEARS)).isCloseTo(new BigDecimal("2.1100"), within(new BigDecimal("0.0005")));
        assertThat(metric(r, MetricCodes.ROI)).isCloseTo(new BigDecimal("0.4762"), within(new BigDecimal("0.0005")));
        assertThat(metric(r, MetricCodes.CAPITAL_NET_PROFIT_RATE)).isCloseTo(new BigDecimal("0.3571"), within(new BigDecimal("0.0005")));
        assertThat(metric(r, MetricCodes.EQUITY_NPV)).isCloseTo(new BigDecimal("1165.2939"), within(new BigDecimal("0.01")));

        List<StatementRow> rows = project(r);
        assertThat(rows).hasSize(6);
        assertThat(rows.get(0).getNetCashFlow()).isEqualByComparingTo("-1000.0000");
        assertThat(rows.get(1).getNetCashFlow()).isEqualByComparingTo("525.0000");   // 投产年扣流动资金 50
        for (int k = 2; k <= 4; k++) {
            assertThat(rows.get(k).getNetCashFlow()).isEqualByComparingTo("575.0000");
        }
        assertThat(rows.get(5).getNetCashFlow()).isEqualByComparingTo("625.0000");   // 末年回收流动资金 50
        assertThat(rows.get(1).getTax()).isEqualByComparingTo("125.0000");
        assertThat(rows.get(1).getDepreciation()).isEqualByComparingTo("200.0000");
    }

    @Test
    void case2_loanFinancedWithConstructionInterest() {
        FinancialResult r = new FinancialEngine().calculate(case2Input());

        assertThat(r.getTotalInvestment()).isCloseTo(new BigDecimal("2307.9040"), within(new BigDecimal("0.001")));
        assertThat(r.getConstructionInterest()).isCloseTo(new BigDecimal("107.9040"), within(new BigDecimal("0.001")));
        assertThat(metric(r, MetricCodes.NPV)).isCloseTo(new BigDecimal("2514.0534"), within(new BigDecimal("0.05")));
        assertThat(metric(r, MetricCodes.EQUITY_NPV)).isCloseTo(new BigDecimal("2611.6897"), within(new BigDecimal("0.05")));

        List<StatementRow> rows = project(r);
        assertThat(rows).hasSize(8);
        assertThat(rows.get(0).getNetCashFlow()).isEqualByComparingTo("-1200.0000");
        assertThat(rows.get(1).getNetCashFlow()).isEqualByComparingTo("-800.0000");
        assertThat(rows.get(2).getNetCashFlow()).isCloseTo(new BigDecimal("937.5627"), within(new BigDecimal("0.01")));
        // 运营期贷款利息逐年递减（等额本金）
        assertThat(rows.get(2).getInterest()).isCloseTo(new BigDecimal("104.6323"), within(new BigDecimal("0.01")));
        assertThat(rows.get(6).getInterest()).isCloseTo(new BigDecimal("26.1581"), within(new BigDecimal("0.01")));
        assertThat(rows.get(7).getInterest()).isEqualByComparingTo("0.0000");

        // 资本金现金流量表：建设期按资本金比例流出
        List<StatementRow> equity = r.getStatements().get(MetricCodes.ST_EQUITY);
        assertThat(equity.get(0).getNetCashFlow()).isCloseTo(new BigDecimal("-491.5200"), within(new BigDecimal("0.01")));
    }

    @Test
    void case3_doubleDecliningWithTaxGradientAndRampUp() {
        FinancialResult r = new FinancialEngine().calculate(case3Input());

        List<StatementRow> rows = project(r);
        assertThat(rows).hasSize(7);
        assertThat(rows.get(0).getNetCashFlow()).isEqualByComparingTo("-1000.0000");
        assertThat(rows.get(1).getNetCashFlow()).isCloseTo(new BigDecimal("1000.0000"), within(new BigDecimal("0.01")));   // 投产年 60% 负荷
        assertThat(rows.get(2).getNetCashFlow()).isCloseTo(new BigDecimal("1800.0000"), within(new BigDecimal("0.01")));
        assertThat(rows.get(3).getNetCashFlow()).isCloseTo(new BigDecimal("1800.0000"), within(new BigDecimal("0.01")));
        // 税收梯度：1~3 年免税（税=0），4~6 年减半
        assertThat(rows.get(1).getTax()).isEqualByComparingTo("0.0000");
        assertThat(rows.get(3).getTax()).isEqualByComparingTo("0.0000");
        assertThat(rows.get(4).getTax()).isCloseTo(new BigDecimal("212.6543"), within(new BigDecimal("0.01")));
        // DDB 折旧逐年递减
        assertThat(rows.get(1).getDepreciation()).isCloseTo(new BigDecimal("333.3333"), within(new BigDecimal("0.001")));
        assertThat(rows.get(2).getDepreciation()).isCloseTo(new BigDecimal("222.2222"), within(new BigDecimal("0.001")));

        assertThat(metric(r, MetricCodes.NPV)).isCloseTo(new BigDecimal("5709.1290"), within(new BigDecimal("0.05")));
    }

    private FinancialInput case2Input() {
        FinancialInput input = baseInput(2, 6);
        input.setConstructionEntries(List.of(new InvestmentEntry("CONSTRUCTION", "建设投资", new BigDecimal("2000"))));
        input.setConstructionSchedule(List.of(new BigDecimal("0.6"), new BigDecimal("0.4")));
        input.setWorkingCapital(new BigDecimal("200"));
        input.setPricePerUnit(new BigDecimal("500"));
        input.setAnnualOutput(new BigDecimal("5"));
        input.setUnitVariableCost(new BigDecimal("150"));
        input.setCostEntries(List.of(new CostEntry("LABOR_MANUFACTURING", "人工制造费", 0, new BigDecimal("300"))));
        input.setDepreciationYears(10);
        input.setResidualRate(new BigDecimal("0.05"));
        LoanTerms loan = new LoanTerms();
        loan.setPrincipalRatioOfConstruction(new BigDecimal("0.6"));
        loan.setInterestRate(new BigDecimal("0.08"));
        loan.setRepaymentYears(5);
        loan.setGraceYears(1);
        loan.setRepaymentMethod(RepaymentMethod.EQUAL_PRINCIPAL);
        input.setLoan(loan);
        input.setEquityRatio(new BigDecimal("0.4"));
        return input;
    }

    private FinancialInput case3Input() {
        FinancialInput input = baseInput(1, 6);
        input.setConstructionEntries(List.of(new InvestmentEntry("CONSTRUCTION", "建设投资", new BigDecimal("1000"))));
        input.setConstructionSchedule(List.of(BigDecimal.ONE));
        input.setWorkingCapital(BigDecimal.ZERO);
        input.setPricePerUnit(new BigDecimal("500"));
        input.setAnnualOutput(new BigDecimal("5"));
        input.setUnitVariableCost(new BigDecimal("100"));
        input.setCostEntries(List.of(new CostEntry("LABOR_MANUFACTURING", "人工制造费", 0, new BigDecimal("200"))));
        input.setDepreciationPolicy(DepreciationPolicy.DOUBLE_DECLINING);
        input.setDepreciationYears(6);
        input.setResidualRate(new BigDecimal("0.04"));
        input.setTaxRate(new BigDecimal("0.25"));
        input.setTaxSchedule(List.of(new TaxBracket(1, 3, BigDecimal.ZERO), new TaxBracket(4, 6, new BigDecimal("0.125"))));
        input.setRampUp(List.of(new RampUpYear(1, new BigDecimal("0.6"))));
        return input;
    }

    @Test
    void sumOfYearsDigitsDepreciation() {
        List<BigDecimal> seq = DepreciationCalculator.schedule(
                DepreciationPolicy.SUM_OF_YEARS_DIGITS, new BigDecimal("1000"),
                new BigDecimal("0.05"), 5, 5);
        // 950 × (5-k+1)/15，scale=12，HALF_UP
        assertThat(seq.get(0)).isEqualByComparingTo("316.666666666667");
        assertThat(seq.get(4)).isEqualByComparingTo("63.333333333333");
    }

    @Test
    void rejectsScheduleSumNotOne() {
        FinancialInput input = case1();
        input.setConstructionYears(2);
        input.setConstructionSchedule(List.of(new BigDecimal("0.5"), new BigDecimal("0.6")));
        assertThrows(IllegalArgumentException.class, () -> new FinancialEngine().calculate(input));
    }

    @Test
    void irrAbsentWhenAllCashFlowsNegative() {
        FinancialInput input = case1();
        input.setPricePerUnit(new BigDecimal("1"));     // 收入远低于成本，运营期持续净负
        input.setUnitVariableCost(new BigDecimal("500"));
        FinancialResult r = new FinancialEngine().calculate(input);
        assertThat(r.getMetrics()).doesNotContainKey(MetricCodes.IRR);
    }

    @Test
    void rejectsGraceYearsNotLessThanRepayment() {
        FinancialInput input = case1();
        LoanTerms loan = new LoanTerms();
        loan.setPrincipalRatioOfConstruction(new BigDecimal("0.5"));
        loan.setInterestRate(new BigDecimal("0.08"));
        loan.setRepaymentYears(5);
        loan.setGraceYears(5);
        input.setLoan(loan);
        assertThrows(IllegalArgumentException.class, () -> new FinancialEngine().calculate(input));
    }

    @Test
    void rejectsOverlappingTaxBrackets() {
        FinancialInput input = case1();
        input.setTaxSchedule(List.of(new TaxBracket(1, 3, BigDecimal.ZERO), new TaxBracket(3, 5, new BigDecimal("0.1"))));
        assertThrows(IllegalArgumentException.class, () -> new FinancialEngine().calculate(input));
    }

    @Test
    void taxIsZeroInLossYear() {
        FinancialInput input = case1();
        input.setPricePerUnit(new BigDecimal("10"));    // 收入不足以覆盖成本
        FinancialResult r = new FinancialEngine().calculate(input);
        for (StatementRow row : project(r)) {
            assertThat(row.getTax()).isEqualByComparingTo("0");
        }
    }

    @Test
    void doubleDecliningSwitchesToStraightLineInFinalYears() {
        List<BigDecimal> seq = DepreciationCalculator.schedule(
                DepreciationPolicy.DOUBLE_DECLINING, new BigDecimal("1000"),
                new BigDecimal("0.04"), 6, 6);
        assertThat(seq.get(0)).isEqualByComparingTo("333.333333333000");
        assertThat(seq.get(1)).isCloseTo(new BigDecimal("222.222222222111"), within(new BigDecimal("0.000001")));
        assertThat(seq.get(2)).isCloseTo(new BigDecimal("148.148148148074"), within(new BigDecimal("0.000001")));
        // 最后两年直线化：(剩余净值-残值)/2
        assertThat(seq.get(4)).isCloseTo(new BigDecimal("78.765432098963"), within(new BigDecimal("0.000001")));
        assertThat(seq.get(5)).isCloseTo(new BigDecimal("39.382716049481"), within(new BigDecimal("0.000001")));
    }
}

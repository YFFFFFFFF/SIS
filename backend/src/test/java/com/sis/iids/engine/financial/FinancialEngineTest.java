package com.sis.iids.engine.financial;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialEngineTest {

    @Test
    void calculatesCoreMetricsForStandardIndustrialSample() {
        FinancialInput input = new FinancialInput();
        input.setConstructionYears(1);
        input.setHorizonYears(5);
        input.setWacc(new BigDecimal("0.10"));
        input.setTaxRate(new BigDecimal("0.25"));
        input.setDepreciationYears(5);
        input.setResidualRate(BigDecimal.ZERO);
        input.setPricePerUnit(new BigDecimal("140"));
        input.setUnitCost(new BigDecimal("40"));
        input.setAnnualOutput(new BigDecimal("1000"));
        input.setFixedOperatingCost(new BigDecimal("10000"));
        input.setConstructionInvestment(new BigDecimal("200000"));
        input.setWorkingCapital(new BigDecimal("20000"));
        input.setInterestDuringConstruction(BigDecimal.ZERO);
        input.setLoanRatio(BigDecimal.ZERO);
        input.setEquityRatio(BigDecimal.ONE);
        input.setConstructionSchedule(List.of(BigDecimal.ONE));

        FinancialResult result = new FinancialEngine().calculate(input);

        assertThat(result.getTotalInvestment()).isEqualByComparingTo("220000.0000");
        assertThat(result.getRows()).hasSize(6);
        assertThat(result.getNpv()).isEqualByComparingTo("86204.4011");
        assertThat(result.getRoi()).isEqualByComparingTo("0.1875");
        assertThat(result.getIrr()).isEqualByComparingTo("0.2391");
        assertThat(result.getCapitalNetProfitRate()).isEqualByComparingTo("0.1705");
        assertThat(result.getStaticPaybackYears()).isEqualByComparingTo("2.8387");
        assertThat(result.getDynamicPaybackYears()).isEqualByComparingTo("3.5152");

        CashFlowPeriod firstOperationYear = result.getRows().get(1);
        assertThat(firstOperationYear.getRevenue()).isEqualByComparingTo("140000.0000");
        assertThat(firstOperationYear.getOperatingCost()).isEqualByComparingTo("50000.0000");
        assertThat(firstOperationYear.getTax()).isEqualByComparingTo("12500.0000");
        assertThat(firstOperationYear.getNetCashFlow()).isEqualByComparingTo("77500.0000");
    }

    @Test
    void rejectsInvalidInvestmentSchedule() {
        FinancialInput input = new FinancialInput();
        input.setConstructionYears(2);
        input.setConstructionInvestment(new BigDecimal("100000"));
        input.setConstructionSchedule(List.of(BigDecimal.ONE));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new FinancialEngine().calculate(input));
    }
}

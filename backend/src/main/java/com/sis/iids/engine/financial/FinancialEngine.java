package com.sis.iids.engine.financial;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * M1 financial engine for cash flow and core investment metrics.
 */
public class FinancialEngine {

    private static final int SCALE = 4;
    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);

    public FinancialResult calculate(FinancialInput input) {
        validate(input);

        BigDecimal totalInvestment = input.totalInvestment();
        BigDecimal depreciation = depreciation(input);
        BigDecimal revenue = input.getPricePerUnit().multiply(input.getAnnualOutput(), MC);
        BigDecimal operatingCost = input.getUnitCost()
                .multiply(input.getAnnualOutput(), MC)
                .add(input.getFixedOperatingCost(), MC);
        BigDecimal interest = annualInterest(input);
        BigDecimal taxableProfit = revenue.subtract(operatingCost, MC)
                .subtract(depreciation, MC)
                .subtract(interest, MC);
        BigDecimal tax = positive(taxableProfit).multiply(input.getTaxRate(), MC);
        BigDecimal netProfit = taxableProfit.subtract(tax, MC);
        BigDecimal operationCashFlow = netProfit.add(depreciation, MC);

        List<CashFlowPeriod> rows = new ArrayList<>();
        BigDecimal cumulative = BigDecimal.ZERO;
        BigDecimal discountedCumulative = BigDecimal.ZERO;
        BigDecimal npv = BigDecimal.ZERO;

        BigDecimal constructionOutflow = totalInvestment;
        CashFlowPeriod construction = row(0, BigDecimal.ZERO, constructionOutflow, input.getWacc());
        construction.setCumulativeCashFlow(scale(construction.getNetCashFlow()));
        rows.add(construction);
        cumulative = cumulative.add(construction.getNetCashFlow(), MC);
        BigDecimal constructionDiscounted = discount(construction.getNetCashFlow(), input.getWacc(), 0);
        npv = npv.add(constructionDiscounted, MC);

        for (int year = 1; year <= input.getHorizonYears(); year++) {
            BigDecimal inflow = operationCashFlow;
            if (year == input.getHorizonYears()) {
                inflow = inflow.add(input.getWorkingCapital(), MC);
            }
            CashFlowPeriod period = row(year, inflow, BigDecimal.ZERO, input.getWacc());
            period.setRevenue(scale(revenue));
            period.setOperatingCost(scale(operatingCost));
            period.setDepreciation(scale(depreciation));
            period.setInterest(scale(interest));
            period.setTax(scale(tax));
            period.setNetProfit(scale(netProfit));
            cumulative = cumulative.add(period.getNetCashFlow(), MC);
            BigDecimal discountedCashFlow = discount(period.getNetCashFlow(), input.getWacc(), year);
            period.setCumulativeCashFlow(scale(cumulative));
            rows.add(period);
            npv = npv.add(discountedCashFlow, MC);
        }

        FinancialResult result = new FinancialResult();
        result.setTotalInvestment(scale(totalInvestment));
        result.setRows(rows);
        result.setNpv(scale(npv));
        result.setRoi(scale(safeDivide(netProfit, input.getConstructionInvestment())));
        result.setStaticPaybackYears(scale(payback(rows, false, input.getWacc())));
        result.setDynamicPaybackYears(scale(payback(rows, true, input.getWacc())));
        return result;
    }

    private void validate(FinancialInput input) {
        if (input.getConstructionYears() <= 0 || input.getHorizonYears() <= 0) {
            throw new IllegalArgumentException("constructionYears and horizonYears must be positive");
        }
        if (input.getConstructionSchedule() != null && !input.getConstructionSchedule().isEmpty()
                && input.getConstructionSchedule().size() != input.getConstructionYears()) {
            throw new IllegalArgumentException("construction schedule size must match construction years");
        }
    }

    private CashFlowPeriod row(int periodNo, BigDecimal inflow, BigDecimal outflow, BigDecimal discountRate) {
        CashFlowPeriod period = new CashFlowPeriod();
        period.setPeriodNo(periodNo);
        period.setInflow(scale(inflow));
        period.setOutflow(scale(outflow));
        BigDecimal net = inflow.subtract(outflow, MC);
        period.setNetCashFlow(scale(net));
        period.setDiscountedCashFlow(scale(discount(net, discountRate, periodNo)));
        return period;
    }

    private BigDecimal depreciation(FinancialInput input) {
        if (input.getDepreciationYears() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal depreciableBase = input.getConstructionInvestment()
                .multiply(BigDecimal.ONE.subtract(input.getResidualRate(), MC), MC);
        return depreciableBase.divide(BigDecimal.valueOf(input.getDepreciationYears()), 12, RoundingMode.HALF_UP);
    }

    private BigDecimal annualInterest(FinancialInput input) {
        BigDecimal loanPrincipal = input.totalInvestment().multiply(input.getLoanRatio(), MC);
        if (input.getLoanTermYears() <= 0) {
            return BigDecimal.ZERO;
        }
        return loanPrincipal.multiply(input.getLoanInterestRate(), MC);
    }

    private BigDecimal payback(List<CashFlowPeriod> rows, boolean discounted, BigDecimal discountRate) {
        BigDecimal cumulative = BigDecimal.ZERO;
        for (int i = 0; i < rows.size(); i++) {
            CashFlowPeriod row = rows.get(i);
            BigDecimal value = discounted
                    ? discount(row.getNetCashFlow(), discountRate, row.getPeriodNo())
                    : row.getNetCashFlow();
            BigDecimal next = cumulative.add(value, MC);
            if (cumulative.signum() < 0 && next.signum() >= 0) {
                BigDecimal deficit = cumulative.abs();
                return BigDecimal.valueOf(i - 1).add(safeDivide(deficit, value), MC);
            }
            cumulative = next;
        }
        return BigDecimal.valueOf(rows.size() - 1L);
    }

    private BigDecimal discount(BigDecimal value, BigDecimal rate, int periodNo) {
        if (periodNo == 0 || rate == null || rate.signum() == 0) {
            return value;
        }
        BigDecimal factor = BigDecimal.ONE.add(rate, MC).pow(periodNo, MC);
        return value.divide(factor, 20, RoundingMode.HALF_UP);
    }

    private BigDecimal safeDivide(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.divide(denominator, 12, RoundingMode.HALF_UP);
    }

    private BigDecimal positive(BigDecimal value) {
        return value.signum() > 0 ? value : BigDecimal.ZERO;
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }
}

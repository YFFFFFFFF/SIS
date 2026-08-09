package com.sis.iids.engine.financial;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 贷款摊还计算器（设计文档 §5.5）。
 * 返回运营年 k=1..repaymentYears 的还本付息计划；超过还款期后利息与还本均为 0。
 * 约定：repaymentYears 含宽限期，还本期 n = repaymentYears − graceYears（设计文档 V1.1 勘误后口径）。
 */
final class LoanAmortizationCalculator {

    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);

    private LoanAmortizationCalculator() {
    }

    static List<LoanScheduleRow> schedule(BigDecimal principalAtOperationStart, LoanTerms loan) {
        List<LoanScheduleRow> rows = new ArrayList<>();
        if (principalAtOperationStart == null || principalAtOperationStart.signum() <= 0 || loan == null) {
            return rows;
        }
        BigDecimal rate = loan.getInterestRate();
        int repayYears = loan.getRepaymentYears();
        int grace = loan.getGraceYears();
        int amortizingYears = repayYears - grace;
        if (amortizingYears <= 0) {
            throw new IllegalArgumentException("宽限期必须小于还款年限");
        }

        BigDecimal balance = principalAtOperationStart;
        // 等额本息年供（还本期 n 年）：A = P0 × r(1+r)^n / ((1+r)^n − 1)
        BigDecimal annuityPayment = null;
        if (loan.getRepaymentMethod() == RepaymentMethod.EQUAL_PAYMENT) {
            if (rate.signum() == 0) {
                annuityPayment = principalAtOperationStart
                        .divide(BigDecimal.valueOf(amortizingYears), 12, RoundingMode.HALF_UP);
            } else {
                BigDecimal factor = BigDecimal.ONE.add(rate, MC).pow(amortizingYears, MC);
                annuityPayment = principalAtOperationStart.multiply(rate, MC).multiply(factor, MC)
                        .divide(factor.subtract(BigDecimal.ONE, MC), 12, RoundingMode.HALF_UP);
            }
        }

        for (int k = 1; k <= repayYears; k++) {
            LoanScheduleRow row = new LoanScheduleRow();
            row.setYearNo(k);
            row.setOpeningBalance(balance);
            BigDecimal interest = balance.multiply(rate, MC);
            BigDecimal principalPaid;
            if (k <= grace) {
                principalPaid = BigDecimal.ZERO;
            } else {
                int remainingPayments = repayYears - k + 1;
                switch (loan.getRepaymentMethod()) {
                    case EQUAL_PRINCIPAL -> principalPaid = principalAtOperationStart
                            .divide(BigDecimal.valueOf(amortizingYears), 12, RoundingMode.HALF_UP);
                    case EQUAL_PAYMENT -> principalPaid = annuityPayment.subtract(interest, MC);
                    case BULLET -> principalPaid = (k == repayYears) ? balance : BigDecimal.ZERO;
                    default -> throw new IllegalArgumentException("不支持的还款方式: " + loan.getRepaymentMethod());
                }
                // 末期/尾差收口：还本不超过当前余额，保证期末余额归零
                if (principalPaid.compareTo(balance) > 0 || remainingPayments == 1) {
                    principalPaid = balance;
                }
            }
            principalPaid = principalPaid.max(BigDecimal.ZERO);
            balance = balance.subtract(principalPaid, MC);
            row.setPrincipalPaid(principalPaid);
            row.setInterestPaid(interest);
            row.setClosingBalance(balance);
            rows.add(row);
        }
        return rows;
    }
}

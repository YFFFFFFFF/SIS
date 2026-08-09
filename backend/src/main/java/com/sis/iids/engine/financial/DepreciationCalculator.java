package com.sis.iids.engine.financial;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 折旧计算器（设计文档 §5.3）。
 * 返回按运营年序号 k=1..operationYears 的折旧序列（下标 0 对应 k=1）。
 */
final class DepreciationCalculator {

    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);

    private DepreciationCalculator() {
    }

    static List<BigDecimal> schedule(DepreciationPolicy policy, BigDecimal depreciableBase,
                                     BigDecimal residualRate, int depreciationYears, int operationYears) {
        List<BigDecimal> result = new ArrayList<>();
        for (int k = 1; k <= operationYears; k++) {
            result.add(BigDecimal.ZERO);
        }
        if (depreciableBase == null || depreciableBase.signum() <= 0 || depreciationYears <= 0) {
            return result;
        }
        switch (policy) {
            case STRAIGHT_LINE -> straightLine(result, depreciableBase, residualRate, depreciationYears);
            case DOUBLE_DECLINING -> doubleDeclining(result, depreciableBase, residualRate, depreciationYears);
            case SUM_OF_YEARS_DIGITS -> sumOfYearsDigits(result, depreciableBase, residualRate, depreciationYears);
        }
        return result;
    }

    private static void straightLine(List<BigDecimal> result, BigDecimal base, BigDecimal residualRate, int years) {
        BigDecimal annual = base.multiply(BigDecimal.ONE.subtract(residualRate, MC), MC)
                .divide(BigDecimal.valueOf(years), 12, RoundingMode.HALF_UP);
        for (int k = 1; k <= Math.min(years, result.size()); k++) {
            result.set(k - 1, annual);
        }
    }

    private static void doubleDeclining(List<BigDecimal> result, BigDecimal base, BigDecimal residualRate, int years) {
        BigDecimal rate = BigDecimal.valueOf(2).divide(BigDecimal.valueOf(years), 12, RoundingMode.HALF_UP);
        BigDecimal bookValue = base;
        BigDecimal residual = base.multiply(residualRate, MC);
        int n = Math.min(years, result.size());
        for (int k = 1; k <= n; k++) {
            BigDecimal dep;
            if (k >= years - 1) {
                // 最后两年改直线：(剩余净值 − 残值) / 2
                dep = bookValue.subtract(residual, MC)
                        .divide(BigDecimal.valueOf(2), 12, RoundingMode.HALF_UP);
            } else {
                dep = bookValue.multiply(rate, MC);
            }
            dep = dep.max(BigDecimal.ZERO);
            result.set(k - 1, dep);
            bookValue = bookValue.subtract(dep, MC);
        }
    }

    private static void sumOfYearsDigits(List<BigDecimal> result, BigDecimal base, BigDecimal residualRate, int years) {
        BigDecimal depreciable = base.multiply(BigDecimal.ONE.subtract(residualRate, MC), MC);
        BigDecimal sumOfYears = BigDecimal.valueOf((long) years * (years + 1) / 2);
        int n = Math.min(years, result.size());
        for (int k = 1; k <= n; k++) {
            BigDecimal dep = depreciable
                    .multiply(BigDecimal.valueOf(years - k + 1), MC)
                    .divide(sumOfYears, 12, RoundingMode.HALF_UP);
            result.set(k - 1, dep);
        }
    }
}

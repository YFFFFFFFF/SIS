package com.sis.iids.engine.sensitivity;

import com.sis.iids.engine.financial.CostEntry;
import com.sis.iids.engine.financial.FinancialEngine;
import com.sis.iids.engine.financial.FinancialInput;
import com.sis.iids.engine.financial.FinancialResult;
import com.sis.iids.engine.financial.InvestmentEntry;
import com.sis.iids.engine.financial.MetricCodes;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 敏感性分析引擎（FR-02-01）。
 * 复用无状态 {@link FinancialEngine} 进行网格化批量重算（红线 R1/R9）：
 * 单因素或多因素（≤2）按波动区间与步长生成因子网格，逐点克隆基准输入并重算目标指标，
 * 输出敏感系数、临界值（目标指标 = 0 的线性插值）与等级。
 *
 * <p>数值规范（红线 R2）：内部 MC(20, HALF_UP)，输出 scale=4。</p>
 */
public class SensitivityEngine {

    private static final int SCALE = 4;
    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);

    private final FinancialEngine financialEngine = new FinancialEngine();

    /**
     * @param baseInput    基准方案输入（已由服务层按 ParameterSet/投资/成本/融资装配）
     * @param targetMetric 目标指标编码（默认 NPV）
     * @param factor1      因素1
     * @param factor2      因素2（可空 = 单因素）
     */
    public SensitivityResult analyze(FinancialInput baseInput, String targetMetric,
                                     FactorSpec factor1, FactorSpec factor2) {
        String metric = (targetMetric == null || targetMetric.isBlank()) ? MetricCodes.NPV : targetMetric;
        BigDecimal baseValue = metricValue(financialEngine.calculate(baseInput), metric);
        if (baseValue == null) {
            throw new IllegalArgumentException("基准方案目标指标 " + metric + " 无解，无法进行敏感性分析");
        }

        BigDecimal[] f1 = factor1.factors();
        BigDecimal[] f2 = factor2 == null ? new BigDecimal[]{null} : factor2.factors();

        List<SensitivityResult.SensitivityCell> matrix = new ArrayList<>();
        for (BigDecimal a : f1) {
            for (BigDecimal b : f2) {
                FinancialInput varied = vary(baseInput, factor1.variable(), a, factor2 == null ? null : factor2.variable(), b);
                BigDecimal value = metricValue(financialEngine.calculate(varied), metric);
                matrix.add(new SensitivityResult.SensitivityCell(a, b, value));
            }
        }

        // 单因素结论（对每个因素独立做单因素扫描，与网格解耦）
        SingleFactor s1 = singleFactor(baseInput, metric, factor1, baseValue);
        SingleFactor s2 = factor2 == null ? SingleFactor.absent()
                : singleFactor(baseInput, metric, factor2, baseValue);

        return new SensitivityResult(metric, factor1, factor2, baseValue, matrix,
                s1.coefficient, s2.coefficient, s1.critical, s2.critical, s1.level, s2.level);
    }

    // ============================================================
    // 单因素扫描：敏感系数 + 临界值 + 等级
    // ============================================================
    private SingleFactor singleFactor(FinancialInput base, String metric, FactorSpec spec, BigDecimal baseValue) {
        BigDecimal[] factors = spec.factors();
        BigDecimal[] values = new BigDecimal[factors.length];
        for (int i = 0; i < factors.length; i++) {
            FinancialInput varied = vary(base, spec.variable(), factors[i], null, null);
            values[i] = metricValue(financialEngine.calculate(varied), metric);
        }
        BigDecimal coefficient = coefficient(factors, values, baseValue);
        BigDecimal critical = critical(factors, values);
        return new SingleFactor(coefficient, critical, level(coefficient));
    }

    /** 敏感系数 = 端点 (Δ指标/基准指标) / (Δ因素) 的均值（取 ±range 两端）。 */
    private BigDecimal coefficient(BigDecimal[] factors, BigDecimal[] values, BigDecimal baseValue) {
        if (baseValue.signum() == 0) {
            return null;
        }
        int n = factors.length;
        BigDecimal lo = sensitivityRatio(values[0], baseValue, factors[0]);
        BigDecimal hi = sensitivityRatio(values[n - 1], baseValue, factors[n - 1]);
        if (lo == null || hi == null) {
            return null;
        }
        return scale(lo.add(hi, MC).divide(BigDecimal.valueOf(2), MC).abs());
    }

    private BigDecimal sensitivityRatio(BigDecimal value, BigDecimal baseValue, BigDecimal factor) {
        if (value == null || factor.signum() == 0) {
            return null;
        }
        BigDecimal metricChange = value.subtract(baseValue, MC).divide(baseValue, MC);
        return metricChange.divide(factor, MC);
    }

    /** 临界值：目标指标 = 0 的因素波动比例，相邻两点线性插值；无符号穿越返回 null。 */
    private BigDecimal critical(BigDecimal[] factors, BigDecimal[] values) {
        for (int i = 1; i < factors.length; i++) {
            BigDecimal v0 = values[i - 1];
            BigDecimal v1 = values[i];
            if (v0 == null || v1 == null) {
                continue;
            }
            if (v0.signum() == 0) {
                return scale(factors[i - 1]);
            }
            if (v0.signum() != v1.signum()) {
                // factor = f0 - v0*(f1-f0)/(v1-v0)
                BigDecimal fraction = v0.multiply(factors[i].subtract(factors[i - 1], MC), MC)
                        .divide(v1.subtract(v0, MC), MC);
                return scale(factors[i - 1].subtract(fraction, MC));
            }
        }
        return null;
    }

    private String level(BigDecimal coefficient) {
        if (coefficient == null) {
            return "LOW";
        }
        BigDecimal abs = coefficient.abs();
        if (abs.compareTo(new BigDecimal("3")) >= 0) {
            return "HIGH";
        }
        if (abs.compareTo(BigDecimal.ONE) >= 0) {
            return "MEDIUM";
        }
        return "LOW";
    }

    // ============================================================
    // 输入变异与取数
    // ============================================================
    private FinancialInput vary(FinancialInput base, SensitivityVariable v1, BigDecimal f1,
                                SensitivityVariable v2, BigDecimal f2) {
        FinancialInput copy = copyOf(base);
        apply(copy, v1, f1);
        if (v2 != null && f2 != null) {
            apply(copy, v2, f2);
        }
        return copy;
    }

    private void apply(FinancialInput input, SensitivityVariable variable, BigDecimal factor) {
        BigDecimal multiplier = BigDecimal.ONE.add(factor, MC);
        switch (variable) {
            case PRICE -> input.setPricePerUnit(input.getPricePerUnit().multiply(multiplier, MC));
            case UNIT_COST -> {
                input.setUnitVariableCost(input.getUnitVariableCost().multiply(multiplier, MC));
                List<CostEntry> scaled = new ArrayList<>();
                for (CostEntry e : input.getCostEntries()) {
                    if ("RAW_MATERIAL".equals(e.category())) {
                        scaled.add(new CostEntry(e.category(), e.name(), e.yearNo(), e.amount().multiply(multiplier, MC)));
                    } else {
                        scaled.add(e);
                    }
                }
                input.setCostEntries(scaled);
            }
            case INVESTMENT -> {
                List<InvestmentEntry> scaled = new ArrayList<>();
                for (InvestmentEntry e : input.getConstructionEntries()) {
                    scaled.add(new InvestmentEntry(e.category(), e.name(), e.amount().multiply(multiplier, MC)));
                }
                input.setConstructionEntries(scaled);
            }
            case CONSTRUCTION_PERIOD -> {
                int base = input.getConstructionYears();
                int delta = factor.multiply(BigDecimal.valueOf(base), MC)
                        .setScale(0, RoundingMode.HALF_UP).intValue();
                int years = Math.max(1, base + delta);
                input.setConstructionYears(years);
                input.setConstructionSchedule(evenSchedule(years));
            }
        }
    }

    private List<BigDecimal> evenSchedule(int years) {
        List<BigDecimal> schedule = new ArrayList<>();
        BigDecimal share = BigDecimal.ONE.divide(BigDecimal.valueOf(years), 12, RoundingMode.HALF_UP);
        for (int i = 0; i < years; i++) {
            schedule.add(share);
        }
        return schedule;
    }

    private BigDecimal metricValue(FinancialResult result, String metric) {
        BigDecimal value = result.getMetrics().get(metric);
        return value;
    }

    private BigDecimal scale(BigDecimal value) {
        return value == null ? null : value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /** 浅拷贝 + 集合深拷贝：保证克隆修改不影响基准（红线 R1 可重算性）。 */
    private FinancialInput copyOf(FinancialInput s) {
        FinancialInput c = new FinancialInput();
        c.setConstructionYears(s.getConstructionYears());
        c.setOperationYears(s.getOperationYears());
        c.setWacc(s.getWacc());
        c.setWaccSource(s.getWaccSource());
        c.setPricePerUnit(s.getPricePerUnit());
        c.setAnnualOutput(s.getAnnualOutput());
        c.setRampUp(new ArrayList<>(s.getRampUp()));
        c.setConstructionEntries(new ArrayList<>(s.getConstructionEntries()));
        c.setConstructionSchedule(new ArrayList<>(s.getConstructionSchedule()));
        c.setWorkingCapital(s.getWorkingCapital());
        c.setAmortizableAmount(s.getAmortizableAmount());
        c.setAmortizationYears(s.getAmortizationYears());
        c.setCostEntries(new ArrayList<>(s.getCostEntries()));
        c.setUnitVariableCost(s.getUnitVariableCost());
        c.setDepreciationPolicy(s.getDepreciationPolicy());
        c.setDepreciationYears(s.getDepreciationYears());
        c.setResidualRate(s.getResidualRate());
        c.setTaxRate(s.getTaxRate());
        c.setTaxSchedule(new ArrayList<>(s.getTaxSchedule()));
        c.setEquityRatio(s.getEquityRatio());
        c.setLoan(s.getLoan());
        return c;
    }

    private record SingleFactor(BigDecimal coefficient, BigDecimal critical, String level) {
        static SingleFactor absent() {
            return new SingleFactor(null, null, null);
        }
    }
}

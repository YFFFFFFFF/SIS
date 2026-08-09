package com.sis.iids.engine.montecarlo;

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
import java.util.Arrays;
import java.util.List;

/**
 * 蒙特卡洛概率分析引擎（FR-02-03）。
 * 复用无状态 {@link FinancialEngine}（红线 R1）：对配置变量的比例扰动做 N 次独立抽样，
 * 逐次克隆基准输入重算目标指标，汇总期望值 / 标准差 / P(>0) / VaR(95%) / 分位数 / 直方图 / 累计概率曲线。
 *
 * <p>红线 R11：随机种子由调用方传入并随运行记录入库，同种子+同配置结果完全可复现。</p>
 * <p>数值规范（红线 R2）：内部 double 高速抽样（万次级），汇总输出 BigDecimal scale=4。</p>
 */
public class MonteCarloEngine {

    private static final int SCALE = 4;
    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);
    private static final int MIN_ITERATIONS = 1000;
    private static final int MAX_ITERATIONS = 100000;
    private static final int HISTOGRAM_BUCKETS = 20;
    private static final int CUMULATIVE_POINTS = 21;

    private final FinancialEngine financialEngine = new FinancialEngine();

    /**
     * @param baseInput    基准方案输入（服务层装配）
     * @param targetMetric 目标指标编码（默认 NPV）
     * @param specs        抽样变量分布配置（≥1）
     * @param iterations   抽样次数（1000 ~ 100000，推荐 ≥10000）
     * @param seed         随机种子（可复现）
     */
    public MonteCarloResult run(FinancialInput baseInput, String targetMetric,
                                List<DistributionSpec> specs, int iterations, long seed) {
        if (specs == null || specs.isEmpty()) {
            throw new IllegalArgumentException("至少配置一个抽样变量");
        }
        if (iterations < MIN_ITERATIONS || iterations > MAX_ITERATIONS) {
            throw new IllegalArgumentException("抽样次数需在 " + MIN_ITERATIONS + " ~ " + MAX_ITERATIONS + " 之间");
        }
        String metric = (targetMetric == null || targetMetric.isBlank()) ? MetricCodes.NPV : targetMetric;

        DistributionSampler sampler = new DistributionSampler(seed);
        double[] values = new double[iterations];
        for (int i = 0; i < iterations; i++) {
            double[] factors = sampler.sample(specs);
            FinancialInput varied = vary(baseInput, specs, factors);
            BigDecimal v = metricValue(financialEngine.calculate(varied), metric);
            values[i] = v == null ? Double.NaN : v.doubleValue();
        }
        return summarize(metric, iterations, seed, values);
    }

    // ============================================================
    // 统计汇总
    // ============================================================
    private MonteCarloResult summarize(String metric, int iterations, long seed, double[] raw) {
        double[] valid = Arrays.stream(raw).filter(v -> !Double.isNaN(v)).toArray();
        if (valid.length == 0) {
            throw new IllegalArgumentException("全部抽样样本目标指标均无解（如现金流无变号），无法汇总");
        }
        Arrays.sort(valid);
        int n = valid.length;

        double mean = Arrays.stream(valid).average().orElse(0);
        double variance = Arrays.stream(valid).map(v -> (v - mean) * (v - mean)).average().orElse(0);
        double stdDev = Math.sqrt(variance);
        long positives = Arrays.stream(valid).filter(v -> v > 0).count();
        double probPositive = (double) positives / n;

        double p5 = percentile(valid, 0.05);
        double p50 = percentile(valid, 0.50);
        double p95 = percentile(valid, 0.95);

        return new MonteCarloResult(metric, iterations, seed,
                bd(mean), bd(stdDev), bd(probPositive),
                bd(p5), bd(p5), bd(p50), bd(p95),
                bd(valid[0]), bd(valid[n - 1]),
                histogram(valid), cumulative(valid));
    }

    /** 最近秩分位数（排序数组）。 */
    private double percentile(double[] sorted, double p) {
        int n = sorted.length;
        int rank = (int) Math.ceil(p * n) - 1;
        rank = Math.max(0, Math.min(n - 1, rank));
        return sorted[rank];
    }

    /** 等宽直方图（HISTOGRAM_BUCKETS 桶）。 */
    private List<MonteCarloResult.HistogramBucket> histogram(double[] sorted) {
        int n = sorted.length;
        double lo = sorted[0];
        double hi = sorted[n - 1];
        double width = (hi - lo) / HISTOGRAM_BUCKETS;
        List<MonteCarloResult.HistogramBucket> buckets = new ArrayList<>();
        if (width <= 0) {
            buckets.add(new MonteCarloResult.HistogramBucket(bd(lo), bd(hi), n, BigDecimal.ONE.setScale(SCALE, RoundingMode.HALF_UP)));
            return buckets;
        }
        int[] counts = new int[HISTOGRAM_BUCKETS];
        for (double v : sorted) {
            int idx = (int) ((v - lo) / width);
            if (idx >= HISTOGRAM_BUCKETS) {
                idx = HISTOGRAM_BUCKETS - 1;
            }
            counts[idx]++;
        }
        for (int i = 0; i < HISTOGRAM_BUCKETS; i++) {
            double from = lo + i * width;
            double to = from + width;
            buckets.add(new MonteCarloResult.HistogramBucket(bd(from), bd(to), counts[i],
                    bd((double) counts[i] / n)));
        }
        return buckets;
    }

    /** 累计概率曲线（CUMULATIVE_POINTS 个点：P(X ≤ x)）。 */
    private List<MonteCarloResult.CumulativePoint> cumulative(double[] sorted) {
        int n = sorted.length;
        List<MonteCarloResult.CumulativePoint> points = new ArrayList<>();
        for (int i = 0; i < CUMULATIVE_POINTS; i++) {
            double p = (double) i / (CUMULATIVE_POINTS - 1);
            int idx = (int) Math.round(p * (n - 1));
            points.add(new MonteCarloResult.CumulativePoint(bd(sorted[idx]), bd(p)));
        }
        return points;
    }

    // ============================================================
    // 输入变异（与 SensitivityEngine 同语义，变量集合不同故独立实现）
    // ============================================================
    private FinancialInput vary(FinancialInput base, List<DistributionSpec> specs, double[] factors) {
        FinancialInput copy = copyOf(base);
        for (int i = 0; i < specs.size(); i++) {
            apply(copy, specs.get(i).variable(), factors[i]);
        }
        return copy;
    }

    private void apply(FinancialInput input, MonteCarloVariable variable, double factor) {
        BigDecimal multiplier = BigDecimal.ONE.add(BigDecimal.valueOf(factor), MC);
        switch (variable) {
            case PRICE -> input.setPricePerUnit(nz(input.getPricePerUnit()).multiply(multiplier, MC));
            case UNIT_COST -> {
                input.setUnitVariableCost(nz(input.getUnitVariableCost()).multiply(multiplier, MC));
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
            case ANNUAL_OUTPUT -> input.setAnnualOutput(nz(input.getAnnualOutput()).multiply(multiplier, MC));
        }
    }

    private BigDecimal metricValue(FinancialResult result, String metric) {
        return result.getMetrics().get(metric);
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private BigDecimal bd(double v) {
        return BigDecimal.valueOf(v).setScale(SCALE, RoundingMode.HALF_UP);
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
}

package com.sis.iids.engine.montecarlo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 蒙特卡洛概率分析结果（FR-02-03）。
 *
 * @param targetMetric  目标指标编码
 * @param iterations    抽样次数
 * @param seed          随机种子（红线 R11 入库，可复现）
 * @param mean          期望值
 * @param stdDev        标准差
 * @param probPositive  指标 > 0 的概率
 * @param var95         VaR(95%)：5% 分位数（95% 置信下指标不会低于该值）
 * @param p5 / p50 / p95 分位数
 * @param min / max     极值
 * @param histogram     直方图分桶（等宽 20 桶）
 * @param cumulative    累计概率曲线（按排序样本取 21 个点）
 */
public record MonteCarloResult(String targetMetric,
                               int iterations,
                               long seed,
                               BigDecimal mean,
                               BigDecimal stdDev,
                               BigDecimal probPositive,
                               BigDecimal var95,
                               BigDecimal p5,
                               BigDecimal p50,
                               BigDecimal p95,
                               BigDecimal min,
                               BigDecimal max,
                               List<HistogramBucket> histogram,
                               List<CumulativePoint> cumulative) {

    /** 直方图分桶：[from, to) 区间内的样本计数与占比 */
    public record HistogramBucket(BigDecimal from, BigDecimal to, int count, BigDecimal ratio) {
    }

    /** 累计概率曲线点：P(指标 ≤ value) = probability */
    public record CumulativePoint(BigDecimal value, BigDecimal probability) {
    }
}

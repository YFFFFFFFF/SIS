package com.sis.iids.montecarlo;

import java.math.BigDecimal;
import java.util.List;

/**
 * R-11 蒙特卡洛响应（FR-02-03）。
 */
public record MonteCarloResponse(Long runId,
                                 Long scenarioId,
                                 String targetMetric,
                                 int iterations,
                                 long seed,
                                 List<VariableSpecView> variables,
                                 BigDecimal mean,
                                 BigDecimal stdDev,
                                 BigDecimal probPositive,
                                 BigDecimal var95,
                                 BigDecimal p5,
                                 BigDecimal p50,
                                 BigDecimal p95,
                                 BigDecimal min,
                                 BigDecimal max,
                                 List<HistogramBucketView> histogram,
                                 List<CumulativePointView> cumulative) {

    public record VariableSpecView(String variable, String type, BigDecimal min, BigDecimal mode,
                                   BigDecimal max, BigDecimal mean, BigDecimal stdDev) {
    }

    public record HistogramBucketView(BigDecimal from, BigDecimal to, int count, BigDecimal ratio) {
    }

    public record CumulativePointView(BigDecimal value, BigDecimal probability) {
    }
}

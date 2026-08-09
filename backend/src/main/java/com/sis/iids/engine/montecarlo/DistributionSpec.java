package com.sis.iids.engine.montecarlo;

import java.math.BigDecimal;

/**
 * 抽样分布配置（FR-02-03）。
 * 支持三角分布（TRIANGULAR：min/mode/max）与正态分布（NORMAL：mean/stdDev，按 3σ 截断），
 * 数值为比例扰动（作用于基准值的比例，如 -0.20 = 下浮 20%）。
 *
 * @param variable 抽样变量
 * @param type     分布类型：TRIANGULAR / NORMAL
 * @param min      三角分布下限（TRIANGULAR 必填）
 * @param mode     三角分布最可能值（TRIANGULAR 必填，常用 0 = 基准）
 * @param max      三角分布上限（TRIANGULAR 必填）
 * @param mean     正态分布均值（NORMAL 必填）
 * @param stdDev   正态分布标准差（NORMAL 必填，> 0）
 */
public record DistributionSpec(MonteCarloVariable variable,
                               String type,
                               BigDecimal min,
                               BigDecimal mode,
                               BigDecimal max,
                               BigDecimal mean,
                               BigDecimal stdDev) {

    public static final String TRIANGULAR = "TRIANGULAR";
    public static final String NORMAL = "NORMAL";

    public DistributionSpec {
        if (variable == null) {
            throw new IllegalArgumentException("抽样变量不能为空");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("分布类型不能为空");
        }
        String t = type.trim().toUpperCase(java.util.Locale.ROOT);
        if (TRIANGULAR.equals(t)) {
            if (min == null || mode == null || max == null) {
                throw new IllegalArgumentException("三角分布需提供 min/mode/max");
            }
            if (!(min.compareTo(mode) <= 0 && mode.compareTo(max) <= 0 && min.compareTo(max) < 0)) {
                throw new IllegalArgumentException("三角分布参数需满足 min ≤ mode ≤ max 且 min < max");
            }
        } else if (NORMAL.equals(t)) {
            if (mean == null || stdDev == null) {
                throw new IllegalArgumentException("正态分布需提供 mean/stdDev");
            }
            if (stdDev.signum() <= 0) {
                throw new IllegalArgumentException("正态分布标准差必须为正数");
            }
        } else {
            throw new IllegalArgumentException("不支持的分布类型: " + type + "（支持 TRIANGULAR / NORMAL）");
        }
        type = t;
    }

    public boolean isTriangular() {
        return TRIANGULAR.equals(type);
    }
}

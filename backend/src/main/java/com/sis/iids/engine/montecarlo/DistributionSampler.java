package com.sis.iids.engine.montecarlo;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Random;

/**
 * 蒙特卡洛抽样器（红线 R11：种子确定性可复现）。
 * 三角分布用逆变换采样；正态分布用 Box-Muller，按 3σ 截断（避免极端比例扰动破坏财务模型）。
 */
public class DistributionSampler {

    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);
    private static final double THREE_SIGMA = 3.0;

    private final Random random;

    public DistributionSampler(long seed) {
        this.random = new Random(seed);
    }

    /** 依序对每个分布抽一个比例扰动样本。 */
    public double[] sample(List<DistributionSpec> specs) {
        double[] out = new double[specs.size()];
        for (int i = 0; i < specs.size(); i++) {
            out[i] = sampleOne(specs.get(i));
        }
        return out;
    }

    private double sampleOne(DistributionSpec spec) {
        if (spec.isTriangular()) {
            return triangular(spec.min().doubleValue(), spec.mode().doubleValue(), spec.max().doubleValue());
        }
        return normal(spec.mean().doubleValue(), spec.stdDev().doubleValue());
    }

    /** 三角分布逆变换采样。 */
    private double triangular(double a, double c, double b) {
        double u = random.nextDouble();
        double fc = (c - a) / (b - a);
        if (u < fc) {
            return a + Math.sqrt(u * (b - a) * (c - a));
        }
        return b - Math.sqrt((1 - u) * (b - a) * (b - c));
    }

    /** 正态分布 Box-Muller，3σ 截断。 */
    private double normal(double mean, double stdDev) {
        double u1 = Math.max(random.nextDouble(), 1e-12);
        double u2 = random.nextDouble();
        double z = Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2);
        double v = mean + z * stdDev;
        double lo = mean - THREE_SIGMA * stdDev;
        double hi = mean + THREE_SIGMA * stdDev;
        return Math.max(lo, Math.min(hi, v));
    }
}

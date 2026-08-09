package com.sis.iids.engine.sensitivity;

import java.math.BigDecimal;

/**
 * 单个敏感性分析因素配置：变量 + 波动区间（±比例）+ 步数（奇数，含基准点）。
 */
public record FactorSpec(SensitivityVariable variable, BigDecimal range, int steps) {

    public FactorSpec {
        if (variable == null) {
            throw new IllegalArgumentException("敏感性变量不能为空");
        }
        if (range == null || range.signum() <= 0) {
            throw new IllegalArgumentException("波动区间必须为正数");
        }
        if (steps < 3 || steps % 2 == 0) {
            throw new IllegalArgumentException("步数必须为不小于 3 的奇数（含基准点）");
        }
    }

    /** 生成从 -range 到 +range 共 steps 个波动比例（含 0 基准），升序。 */
    public BigDecimal[] factors() {
        BigDecimal[] result = new BigDecimal[steps];
        int half = steps / 2;
        BigDecimal step = range.divide(BigDecimal.valueOf(half), 12, java.math.RoundingMode.HALF_UP);
        for (int i = 0; i < steps; i++) {
            result[i] = step.multiply(BigDecimal.valueOf(i - half)).stripTrailingZeros();
        }
        return result;
    }
}

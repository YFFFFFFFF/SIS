package com.sis.iids.montecarlo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * R-11 蒙特卡洛请求（FR-02-03）。
 *
 * @param targetMetric 目标指标（默认 NPV）
 * @param iterations   抽样次数（1000~100000，默认 10000）
 * @param seed         随机种子（可空，空则随机生成并入库——红线 R11 入库可复现）
 * @param variables    抽样变量分布配置（≥1）
 * @param taskId       关联测算任务（可选）
 */
public record MonteCarloRequest(String targetMetric,
                                @NotNull @Min(1000) @Max(100000) Integer iterations,
                                Long seed,
                                @NotEmpty @Valid List<VariableSpec> variables,
                                Long taskId) {

    /**
     * @param variable 抽样变量：PRICE / UNIT_COST / INVESTMENT / ANNUAL_OUTPUT
     * @param type     分布类型：TRIANGULAR / NORMAL
     * @param min      三角分布下限（比例扰动）
     * @param mode     三角分布最可能值
     * @param max      三角分布上限
     * @param mean     正态分布均值
     * @param stdDev   正态分布标准差
     */
    public record VariableSpec(@NotNull String variable,
                               @NotNull String type,
                               BigDecimal min,
                               BigDecimal mode,
                               BigDecimal max,
                               BigDecimal mean,
                               BigDecimal stdDev) {
    }
}

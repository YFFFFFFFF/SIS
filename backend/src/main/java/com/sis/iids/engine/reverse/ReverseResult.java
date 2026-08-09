package com.sis.iids.engine.reverse;

import java.math.BigDecimal;

/**
 * 目标反算结果（FR-01-05）。
 *
 * @param targetMetric   目标指标编码
 * @param targetValue    目标值
 * @param variable       被反算变量
 * @param factor         求解得到的比例因子（实际值 = 基准 × factor）
 * @param solvedValue    反算得到的变量实际取值
 * @param baseValue      基准方案下目标指标的值
 * @param achievedValue  反算后目标指标的实际达成值（应≈targetValue）
 * @param feasible       是否在搜索区间内收敛出解
 * @param iterations     二分迭代次数
 * @param sensitivityNote 敏感性说明（指标对该变量的敏感程度与方向）
 * @param boundaryNote   适用边界与假设声明（约束：需标注适用边界与假设）
 */
public record ReverseResult(String targetMetric,
                            BigDecimal targetValue,
                            String variable,
                            BigDecimal factor,
                            BigDecimal solvedValue,
                            BigDecimal baseValue,
                            BigDecimal achievedValue,
                            boolean feasible,
                            int iterations,
                            String sensitivityNote,
                            String boundaryNote) {
}

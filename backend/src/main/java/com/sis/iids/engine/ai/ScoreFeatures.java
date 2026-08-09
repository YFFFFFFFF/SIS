package com.sis.iids.engine.ai;

import java.math.BigDecimal;

/**
 * R-17 打分输入特征（FR-05-03）：由服务层从测算/敏感性/盈亏平衡/复盘聚合。
 *
 * @param npv               NPV
 * @param irr               IRR（可空 = 无解）
 * @param wacc              基准折现率
 * @param staticPayback     静态回收期（年，可空）
 * @param horizonYears      测算期（年）
 * @param highestSensitivity 最高敏感等级（HIGH/MEDIUM/LOW，可空 = 未做敏感性）
 * @param bepUtilization    盈亏平衡产能利用率（可空）
 * @param reviewDeviation   历史复盘偏差率中位数（可空 = 无复盘数据）
 */
public record ScoreFeatures(BigDecimal npv,
                            BigDecimal irr,
                            BigDecimal wacc,
                            BigDecimal staticPayback,
                            int horizonYears,
                            String highestSensitivity,
                            BigDecimal bepUtilization,
                            BigDecimal reviewDeviation) {
}

package com.sis.iids.engine.sensitivity;

import java.math.BigDecimal;

/**
 * 单个因素的单因素敏感性结论。
 * coefficient：敏感系数 = (指标变动率)/(因素变动率)，取 ±range 端点均值；
 * criticalFactor：使目标指标 = 0 的因素波动比例（线性插值），无穿越时为 null；
 * level：HIGH/MEDIUM/LOW 敏感等级。
 */
public record SensitivityFactorResult(SensitivityVariable variable,
                                      BigDecimal coefficient,
                                      BigDecimal criticalFactor,
                                      String level) {
}

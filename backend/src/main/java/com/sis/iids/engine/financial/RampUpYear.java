package com.sis.iids.engine.financial;

import java.math.BigDecimal;

/**
 * 投产期负荷：year 为运营年序号（1 起），loadFactor ∈ (0,1]，未列年份默认 1.0。
 */
public record RampUpYear(int year, BigDecimal loadFactor) {
}

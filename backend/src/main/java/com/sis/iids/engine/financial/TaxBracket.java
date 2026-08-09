package com.sis.iids.engine.financial;

import java.math.BigDecimal;

/**
 * 税率梯度区间（税收优惠期，如"三免三减半"），year 为运营年序号（1 起，含端点）。
 */
public record TaxBracket(int fromYear, int toYear, BigDecimal rate) {
}

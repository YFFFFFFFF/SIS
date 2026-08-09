package com.sis.iids.portfolio;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * R-13 组合优化请求（FR-03-02）。
 *
 * @param budget   资金池上限（> 0）
 * @param maxCount 数量上限（可空 = 不限）
 */
public record PortfolioRequest(@NotNull @Positive BigDecimal budget,
                               Integer maxCount) {
}

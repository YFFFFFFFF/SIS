package com.sis.iids.library;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * R-16 项目复盘保存请求（FR-03-03）。
 */
public record ProjectReviewRequest(@NotNull Long scenarioId,
                                   BigDecimal actualNpv,
                                   BigDecimal actualIrr,
                                   BigDecimal actualInvestment,
                                   BigDecimal actualPaybackYears,
                                   LocalDate operationStartDate,
                                   String lessons) {
}

package com.sis.iids.library;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * R-16 项目复盘响应（FR-03-03）：实际指标 + 计划指标 + 偏差对照。
 */
public record ProjectReviewResponse(Long id,
                                    Long projectId,
                                    Long scenarioId,
                                    String scenarioName,
                                    BigDecimal actualNpv,
                                    BigDecimal actualIrr,
                                    BigDecimal actualInvestment,
                                    BigDecimal actualPaybackYears,
                                    BigDecimal plannedNpv,
                                    BigDecimal plannedIrr,
                                    BigDecimal plannedInvestment,
                                    BigDecimal plannedPaybackYears,
                                    BigDecimal npvDeviation,
                                    BigDecimal irrDeviation,
                                    LocalDate operationStartDate,
                                    String lessons,
                                    String createdBy,
                                    LocalDateTime createdAt,
                                    LocalDateTime updatedAt) {
}

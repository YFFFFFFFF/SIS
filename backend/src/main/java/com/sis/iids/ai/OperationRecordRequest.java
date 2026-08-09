package com.sis.iids.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * R-17 历史运营数据录入请求（FR-05）。
 */
public record OperationRecordRequest(@NotBlank String period,
                                     BigDecimal actualRevenue,
                                     BigDecimal actualCost,
                                     BigDecimal actualOutput,
                                     BigDecimal actualNpv,
                                     BigDecimal actualIrr,
                                     BigDecimal deviationRatio,
                                     @NotNull Boolean verified,
                                     String note) {
}

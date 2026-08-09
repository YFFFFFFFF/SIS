package com.sis.iids.ai;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * R-17 历史运营数据响应（FR-05）。
 */
public record OperationRecordResponse(Long id,
                                      Long projectId,
                                      String period,
                                      BigDecimal actualRevenue,
                                      BigDecimal actualCost,
                                      BigDecimal actualOutput,
                                      BigDecimal actualNpv,
                                      BigDecimal actualIrr,
                                      BigDecimal deviationRatio,
                                      boolean verified,
                                      String note,
                                      String createdBy,
                                      LocalDateTime createdAt) {
}

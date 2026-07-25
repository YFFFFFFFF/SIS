package com.sis.iids.calculation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record CalculationRunResponse(CalculationTaskResponse task, Map<String, BigDecimal> metrics,
                                     List<CashFlowRowResponse> cashFlowRows) {
}
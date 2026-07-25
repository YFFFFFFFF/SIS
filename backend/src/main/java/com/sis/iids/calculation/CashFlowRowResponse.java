package com.sis.iids.calculation;

import java.math.BigDecimal;

public record CashFlowRowResponse(Long id, Long scenarioId, Long taskId, String statementType, Integer periodNo,
                                  BigDecimal inflow, BigDecimal outflow, BigDecimal netCashFlow,
                                  BigDecimal discountedCashFlow, BigDecimal cumulativeCashFlow) {
    static CashFlowRowResponse from(CashFlowRow row) {
        return new CashFlowRowResponse(row.getId(), row.getScenarioId(), row.getTaskId(), row.getStatementType(),
                row.getPeriodNo(), row.getInflow(), row.getOutflow(), row.getNetCashFlow(),
                row.getDiscountedCashFlow(), row.getCumulativeCashFlow());
    }
}
package com.sis.iids.calculation;

import java.math.BigDecimal;

public record CashFlowRowResponse(Long id, Long scenarioId, Long taskId, String statementType, Integer periodNo,
                                  BigDecimal inflow, BigDecimal outflow, BigDecimal netCashFlow,
                                  BigDecimal discountedCashFlow, BigDecimal cumulativeCashFlow,
                                  BigDecimal revenue, BigDecimal operatingCost, BigDecimal depreciation,
                                  BigDecimal amortization, BigDecimal interest, BigDecimal tax,
                                  BigDecimal netProfit) {
    static CashFlowRowResponse from(CashFlowRow row) {
        return new CashFlowRowResponse(row.getId(), row.getScenarioId(), row.getTaskId(), row.getStatementType(),
                row.getPeriodNo(), row.getInflow(), row.getOutflow(), row.getNetCashFlow(),
                row.getDiscountedCashFlow(), row.getCumulativeCashFlow(),
                row.getRevenue(), row.getOperatingCost(), row.getDepreciation(), row.getAmortization(),
                row.getInterest(), row.getTax(), row.getNetProfit());
    }
}

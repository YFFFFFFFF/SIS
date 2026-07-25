package com.sis.iids.calculation;

import java.math.BigDecimal;

public record InvestmentItemResponse(Long id, Long scenarioId, String category, String name, BigDecimal amount, Integer yearNo) {
    static InvestmentItemResponse from(InvestmentItem item) {
        return new InvestmentItemResponse(item.getId(), item.getScenarioId(), item.getCategory(), item.getName(), item.getAmount(), item.getYearNo());
    }
}
package com.sis.iids.calculation;

import java.math.BigDecimal;

public record FinancingPlanResponse(Long id, Long scenarioId, String sourceType, BigDecimal ratio, BigDecimal amount,
                                    BigDecimal interestRate, Integer termYears) {
    static FinancingPlanResponse from(FinancingPlan plan) {
        return new FinancingPlanResponse(plan.getId(), plan.getScenarioId(), plan.getSourceType(), plan.getRatio(),
                plan.getAmount(), plan.getInterestRate(), plan.getTermYears());
    }
}
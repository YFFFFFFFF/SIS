package com.sis.iids.calculation;

import java.math.BigDecimal;

public record FinancingPlanResponse(Long id, Long scenarioId, String sourceType, BigDecimal ratio, BigDecimal amount,
                                    BigDecimal interestRate, Integer termYears, String repaymentMethod,
                                    Integer graceYears) {
    static FinancingPlanResponse from(FinancingPlan plan) {
        return new FinancingPlanResponse(plan.getId(), plan.getScenarioId(), plan.getSourceType(), plan.getRatio(),
                plan.getAmount(), plan.getInterestRate(), plan.getTermYears(), plan.getRepaymentMethod(),
                plan.getGraceYears());
    }
}

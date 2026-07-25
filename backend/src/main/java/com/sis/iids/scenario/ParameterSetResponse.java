package com.sis.iids.scenario;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ParameterSetResponse(
        Long id,
        Long scenarioId,
        BigDecimal wacc,
        String waccSource,
        BigDecimal taxRate,
        Integer depreciationYears,
        BigDecimal residualRate,
        BigDecimal loanRatioLimit,
        BigDecimal pricePerUnit,
        BigDecimal unitCost,
        BigDecimal annualOutput,
        BigDecimal fixedOperatingCost,
        String formulaVersion,
        LocalDateTime createdAt
) {
    static ParameterSetResponse from(ParameterSet parameterSet) {
        return new ParameterSetResponse(
                parameterSet.getId(),
                parameterSet.getScenarioId(),
                parameterSet.getWacc(),
                parameterSet.getWaccSource(),
                parameterSet.getTaxRate(),
                parameterSet.getDepreciationYears(),
                parameterSet.getResidualRate(),
                parameterSet.getLoanRatioLimit(),
                parameterSet.getPricePerUnit(),
                parameterSet.getUnitCost(),
                parameterSet.getAnnualOutput(),
                parameterSet.getFixedOperatingCost(),
                parameterSet.getFormulaVersion(),
                parameterSet.getCreatedAt()
        );
    }
}
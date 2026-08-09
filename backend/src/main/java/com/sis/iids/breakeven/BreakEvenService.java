package com.sis.iids.breakeven;

import com.sis.iids.calculation.CalculationService;
import com.sis.iids.common.error.BusinessException;
import com.sis.iids.common.error.ErrorCode;
import com.sis.iids.engine.breakeven.BreakEvenEngine;
import com.sis.iids.engine.breakeven.BreakEvenResult;
import com.sis.iids.engine.financial.FinancialInput;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * R-10 盈亏平衡分析服务（FR-02-02）。
 * 只读计算接口：实时组装基准输入 → 调用无状态 BreakEvenEngine，不落库（规模 S，结论可复算）。
 */
@Service
public class BreakEvenService {

    private final CalculationService calculationService;
    private final BreakEvenEngine breakEvenEngine = new BreakEvenEngine();

    public BreakEvenService(CalculationService calculationService) {
        this.calculationService = calculationService;
    }

    @Transactional(readOnly = true)
    public BreakEvenResponse analyze(Long scenarioId) {
        FinancialInput input;
        try {
            input = calculationService.buildBaseInput(scenarioId);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, ex.getMessage());
        }
        BreakEvenResult result;
        try {
            result = breakEvenEngine.analyze(input);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, ex.getMessage());
        }
        return new BreakEvenResponse(scenarioId,
                result.pricePerUnit(), result.annualOutput(), result.unitVariableCost(), result.annualFixedCost(),
                result.bepOutput(), result.bepUtilization(), result.bepPrice(), result.contributionMargin(),
                result.solvable(), result.unsolvableReason(),
                result.curve().stream().map(p -> new BreakEvenResponse.CurvePointView(p.output(), p.revenue(), p.totalCost())).toList(),
                result.assumptionNote());
    }
}

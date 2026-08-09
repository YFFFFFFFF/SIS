package com.sis.iids.calculation;

import java.math.BigDecimal;
import java.util.List;

/**
 * 投资估算汇总（FR-01-01）：三级结构分项合计 + 与总投资口径的校验结果。
 * balanced = constructionTotal + interestDuringConstruction + workingCapital 是否等于声明总投资（若声明）。
 */
public record InvestmentSummary(Long scenarioId,
                                BigDecimal constructionTotal,
                                BigDecimal interestDuringConstruction,
                                BigDecimal workingCapital,
                                BigDecimal totalInvestment,
                                BigDecimal declaredTotalInvestment,
                                boolean balanced,
                                List<InvestmentItemResponse> items) {
}

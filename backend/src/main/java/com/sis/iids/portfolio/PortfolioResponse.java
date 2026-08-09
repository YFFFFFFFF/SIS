package com.sis.iids.portfolio;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * R-13 组合优化响应（FR-03-02）。
 */
public record PortfolioResponse(Long runId,
                                BigDecimal budget,
                                Integer maxCount,
                                int candidateCount,
                                BigDecimal totalNpv,
                                BigDecimal totalInvestment,
                                String explanation,
                                List<MemberView> members,
                                List<FrontierPointView> frontier,
                                String engineVersion,
                                String createdBy,
                                LocalDateTime createdAt) {

    public record MemberView(Long scenarioId, String scenarioName, String projectName,
                             BigDecimal npv, BigDecimal investment, BigDecimal irr,
                             boolean selected, Integer rankNo) {
    }

    public record FrontierPointView(BigDecimal budget, BigDecimal npv, BigDecimal investment, int count) {
    }
}

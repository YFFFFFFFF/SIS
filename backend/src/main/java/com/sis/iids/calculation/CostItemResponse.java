package com.sis.iids.calculation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CostItemResponse(Long id, Long scenarioId, String category, String name, Integer yearNo,
                               BigDecimal amount, LocalDateTime createdAt) {
    static CostItemResponse from(CostItem item) {
        return new CostItemResponse(item.getId(), item.getScenarioId(), item.getCategory(), item.getName(),
                item.getYearNo(), item.getAmount(), item.getCreatedAt());
    }
}

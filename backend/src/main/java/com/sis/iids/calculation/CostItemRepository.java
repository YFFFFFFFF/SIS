package com.sis.iids.calculation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CostItemRepository extends JpaRepository<CostItem, Long> {
    List<CostItem> findByScenarioId(Long scenarioId);
    List<CostItem> findByScenarioIdOrderByCategoryAscYearNoAsc(Long scenarioId);
}

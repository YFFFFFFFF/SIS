package com.sis.iids.calculation;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InvestmentItemRepository extends JpaRepository<InvestmentItem, Long> {
    List<InvestmentItem> findByScenarioId(Long scenarioId);
    List<InvestmentItem> findByScenarioIdOrderBySortOrderAscIdAsc(Long scenarioId);
    List<InvestmentItem> findByParentId(Long parentId);
}

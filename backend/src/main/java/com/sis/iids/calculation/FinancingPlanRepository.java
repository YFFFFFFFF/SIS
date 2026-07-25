package com.sis.iids.calculation;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FinancingPlanRepository extends JpaRepository<FinancingPlan, Long> {
    List<FinancingPlan> findByScenarioId(Long scenarioId);
}
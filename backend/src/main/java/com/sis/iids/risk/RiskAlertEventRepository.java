package com.sis.iids.risk;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiskAlertEventRepository extends JpaRepository<RiskAlertEvent, Long> {

    List<RiskAlertEvent> findByStatusOrderByCreatedAtDesc(String status);

    List<RiskAlertEvent> findByScenarioIdOrderByCreatedAtDesc(Long scenarioId);

    List<RiskAlertEvent> findByRuleIdAndScenarioIdAndStatus(Long ruleId, Long scenarioId, String status);

    long countByStatus(String status);
}

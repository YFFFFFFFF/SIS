package com.sis.iids.calculation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CalculationTaskRepository extends JpaRepository<CalculationTask, Long> {
    Optional<CalculationTask> findByScenarioIdAndRequestKey(Long scenarioId, String requestKey);

    Optional<CalculationTask> findFirstByStatusOrderByCreatedAtAsc(CalculationStatus status);
}

package com.sis.iids.montecarlo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonteCarloRunRepository extends JpaRepository<MonteCarloRun, Long> {

    List<MonteCarloRun> findByScenarioIdOrderByCreatedAtDesc(Long scenarioId);
}

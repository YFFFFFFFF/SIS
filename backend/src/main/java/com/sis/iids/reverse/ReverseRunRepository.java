package com.sis.iids.reverse;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReverseRunRepository extends JpaRepository<ReverseRun, Long> {
    List<ReverseRun> findByScenarioIdOrderByCreatedAtDesc(Long scenarioId);
}

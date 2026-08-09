package com.sis.iids.sensitivity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SensitivityRunRepository extends JpaRepository<SensitivityRun, Long> {
    List<SensitivityRun> findByScenarioIdOrderByCreatedAtDesc(Long scenarioId);
}

package com.sis.iids.collab;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ScenarioFieldLockRepository extends JpaRepository<ScenarioFieldLock, Long> {

    List<ScenarioFieldLock> findByScenarioId(Long scenarioId);

    Optional<ScenarioFieldLock> findByScenarioIdAndFieldKey(Long scenarioId, String fieldKey);

    void deleteByScenarioIdAndFieldKey(Long scenarioId, String fieldKey);

    void deleteByExpireAtBefore(LocalDateTime time);
}

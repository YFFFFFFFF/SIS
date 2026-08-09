package com.sis.iids.collab;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ScenarioPresenceRepository extends JpaRepository<ScenarioPresence, Long> {

    Optional<ScenarioPresence> findByScenarioIdAndUserId(Long scenarioId, Long userId);

    List<ScenarioPresence> findByScenarioIdAndLastSeenAtAfter(Long scenarioId, LocalDateTime since);
}

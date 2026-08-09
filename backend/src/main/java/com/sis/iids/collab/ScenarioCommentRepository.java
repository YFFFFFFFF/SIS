package com.sis.iids.collab;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScenarioCommentRepository extends JpaRepository<ScenarioComment, Long> {

    List<ScenarioComment> findByScenarioIdOrderByCreatedAtAsc(Long scenarioId);
}

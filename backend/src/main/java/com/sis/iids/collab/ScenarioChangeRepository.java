package com.sis.iids.collab;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScenarioChangeRepository extends JpaRepository<ScenarioChange, Long> {

    List<ScenarioChange> findByScenarioIdOrderByVersionNoDesc(Long scenarioId);

    @Query("SELECT MAX(c.versionNo) FROM ScenarioChange c WHERE c.scenarioId = :scenarioId")
    Optional<Integer> findMaxVersionNo(@Param("scenarioId") Long scenarioId);
}

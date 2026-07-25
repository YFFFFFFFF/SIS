package com.sis.iids.scenario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParameterSetRepository extends JpaRepository<ParameterSet, Long> {

    Optional<ParameterSet> findByScenarioId(Long scenarioId);
}
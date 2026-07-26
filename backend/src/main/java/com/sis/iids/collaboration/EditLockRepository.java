package com.sis.iids.collaboration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EditLockRepository extends JpaRepository<EditLock, Long> {
    Optional<EditLock> findByScenarioId(Long scenarioId);
}
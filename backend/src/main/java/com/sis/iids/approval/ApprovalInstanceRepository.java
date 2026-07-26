package com.sis.iids.approval;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApprovalInstanceRepository extends JpaRepository<ApprovalInstance, Long> {
    Optional<ApprovalInstance> findFirstByScenarioIdOrderByCreatedAtDesc(Long scenarioId);
}
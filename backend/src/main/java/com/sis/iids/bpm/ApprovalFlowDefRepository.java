package com.sis.iids.bpm;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApprovalFlowDefRepository extends JpaRepository<ApprovalFlowDef, Long> {

    Optional<ApprovalFlowDef> findByCode(String code);

    Optional<ApprovalFlowDef> findFirstByIsDefaultTrueAndEnabledTrue();
}

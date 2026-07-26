package com.sis.iids.approval;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalRecordRepository extends JpaRepository<ApprovalRecord, Long> {
    List<ApprovalRecord> findByInstanceIdOrderByOperatedAtAsc(Long instanceId);
}
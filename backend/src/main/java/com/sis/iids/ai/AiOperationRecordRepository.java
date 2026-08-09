package com.sis.iids.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiOperationRecordRepository extends JpaRepository<AiOperationRecord, Long> {

    List<AiOperationRecord> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    List<AiOperationRecord> findByVerifiedTrue();
}

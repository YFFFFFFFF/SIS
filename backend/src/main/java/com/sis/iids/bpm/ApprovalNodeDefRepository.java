package com.sis.iids.bpm;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalNodeDefRepository extends JpaRepository<ApprovalNodeDef, Long> {

    List<ApprovalNodeDef> findByFlowDefIdOrderBySeqAsc(Long flowDefId);

    void deleteByFlowDefId(Long flowDefId);
}

package com.sis.iids.sensitivity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SensitivityCellRepository extends JpaRepository<SensitivityCell, Long> {
    List<SensitivityCell> findByRunIdOrderByFactor1AscFactor2Asc(Long runId);
}

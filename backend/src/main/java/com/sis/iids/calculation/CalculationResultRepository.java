package com.sis.iids.calculation;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CalculationResultRepository extends JpaRepository<CalculationResultEntity, Long> {
    List<CalculationResultEntity> findByTaskIdOrderByMetricCodeAsc(Long taskId);
}
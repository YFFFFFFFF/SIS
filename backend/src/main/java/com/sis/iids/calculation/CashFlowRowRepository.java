package com.sis.iids.calculation;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CashFlowRowRepository extends JpaRepository<CashFlowRow, Long> {
    List<CashFlowRow> findByTaskIdOrderByPeriodNoAsc(Long taskId);
}
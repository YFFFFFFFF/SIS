package com.sis.iids.risk;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiskRuleRepository extends JpaRepository<RiskRule, Long> {

    List<RiskRule> findByEnabledTrue();

    List<RiskRule> findAllByOrderByIdAsc();
}

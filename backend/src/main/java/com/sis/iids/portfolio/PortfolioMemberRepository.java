package com.sis.iids.portfolio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortfolioMemberRepository extends JpaRepository<PortfolioMember, Long> {

    List<PortfolioMember> findByRunIdOrderByRankNoAsc(Long runId);
}

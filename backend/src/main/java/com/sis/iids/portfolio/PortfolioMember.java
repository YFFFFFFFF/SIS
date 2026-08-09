package com.sis.iids.portfolio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * R-13 组合优化成员（FR-03-02）：候选方案在当次运行中的选中状态与排序。
 */
@Entity
@Table(name = "portfolio_member")
public class PortfolioMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "run_id", nullable = false)
    private Long runId;
    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;
    @Column(name = "scenario_name", nullable = false)
    private String scenarioName;
    @Column(name = "project_name")
    private String projectName;
    @Column(precision = 24, scale = 8)
    private BigDecimal npv;
    @Column(precision = 24, scale = 8)
    private BigDecimal investment;
    @Column(precision = 24, scale = 8)
    private BigDecimal irr;
    @Column(nullable = false)
    private Boolean selected = false;
    @Column(name = "rank_no")
    private Integer rankNo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRunId() { return runId; }
    public void setRunId(Long runId) { this.runId = runId; }
    public Long getScenarioId() { return scenarioId; }
    public void setScenarioId(Long scenarioId) { this.scenarioId = scenarioId; }
    public String getScenarioName() { return scenarioName; }
    public void setScenarioName(String scenarioName) { this.scenarioName = scenarioName; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public BigDecimal getNpv() { return npv; }
    public void setNpv(BigDecimal npv) { this.npv = npv; }
    public BigDecimal getInvestment() { return investment; }
    public void setInvestment(BigDecimal investment) { this.investment = investment; }
    public BigDecimal getIrr() { return irr; }
    public void setIrr(BigDecimal irr) { this.irr = irr; }
    public Boolean getSelected() { return selected; }
    public void setSelected(Boolean selected) { this.selected = selected; }
    public Integer getRankNo() { return rankNo; }
    public void setRankNo(Integer rankNo) { this.rankNo = rankNo; }
}

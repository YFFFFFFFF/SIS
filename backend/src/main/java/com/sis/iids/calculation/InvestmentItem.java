package com.sis.iids.calculation;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "investment_item")
public class InvestmentItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;
    @Column(nullable = false, length = 64)
    private String category;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal amount;
    @Column(name = "year_no", nullable = false)
    private Integer yearNo = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getScenarioId() { return scenarioId; }
    public void setScenarioId(Long scenarioId) { this.scenarioId = scenarioId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Integer getYearNo() { return yearNo; }
    public void setYearNo(Integer yearNo) { this.yearNo = yearNo; }
}
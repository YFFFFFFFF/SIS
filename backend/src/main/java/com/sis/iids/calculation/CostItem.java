package com.sis.iids.calculation;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cost_item")
public class CostItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;
    @Column(nullable = false, length = 64)
    private String category;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(name = "year_no", nullable = false)
    private Integer yearNo = 0;
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal amount;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getScenarioId() { return scenarioId; }
    public void setScenarioId(Long scenarioId) { this.scenarioId = scenarioId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getYearNo() { return yearNo; }
    public void setYearNo(Integer yearNo) { this.yearNo = yearNo; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

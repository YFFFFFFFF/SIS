package com.sis.iids.collab;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * R-15 收尾：字段级编辑锁（FR-04-02）。
 * 锁粒度从"整个方案"细化到"单个字段"（如 param.wacc、investment.amount:3），
 * 支持多人同时编辑同一方案的不同字段，同字段冲突时先抢先得、到期自动释放。
 */
@Entity
@Table(name = "scenario_field_lock")
public class ScenarioFieldLock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;

    @Column(name = "field_key", nullable = false, length = 128)
    private String fieldKey;

    @Column(name = "holder_id")
    private Long holderId;

    @Column(name = "holder_name", nullable = false, length = 64)
    private String holderName;

    @Column(name = "acquired_at", nullable = false)
    private LocalDateTime acquiredAt;

    @Column(name = "expire_at", nullable = false)
    private LocalDateTime expireAt;

    @PrePersist
    void onCreate() {
        if (acquiredAt == null) {
            acquiredAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getScenarioId() { return scenarioId; }
    public void setScenarioId(Long scenarioId) { this.scenarioId = scenarioId; }
    public String getFieldKey() { return fieldKey; }
    public void setFieldKey(String fieldKey) { this.fieldKey = fieldKey; }
    public Long getHolderId() { return holderId; }
    public void setHolderId(Long holderId) { this.holderId = holderId; }
    public String getHolderName() { return holderName; }
    public void setHolderName(String holderName) { this.holderName = holderName; }
    public LocalDateTime getAcquiredAt() { return acquiredAt; }
    public void setAcquiredAt(LocalDateTime acquiredAt) { this.acquiredAt = acquiredAt; }
    public LocalDateTime getExpireAt() { return expireAt; }
    public void setExpireAt(LocalDateTime expireAt) { this.expireAt = expireAt; }
}

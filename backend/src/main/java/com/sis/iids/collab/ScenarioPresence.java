package com.sis.iids.collab;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * R-15 在线状态（FR-04-02）：心跳登记，判定在线窗口由服务层控制（默认 2 分钟）。
 */
@Entity
@Table(name = "scenario_presence", uniqueConstraints = @UniqueConstraint(columnNames = {"scenario_id", "user_id"}))
public class ScenarioPresence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "user_name", nullable = false, length = 64)
    private String userName;
    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    @PrePersist
    @PreUpdate
    void touch() {
        lastSeenAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getScenarioId() { return scenarioId; }
    public void setScenarioId(Long scenarioId) { this.scenarioId = scenarioId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public LocalDateTime getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(LocalDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }
}

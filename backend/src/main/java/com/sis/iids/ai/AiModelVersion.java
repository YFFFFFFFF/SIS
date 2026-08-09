package com.sis.iids.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * R-17 模型版本（FR-05）：打分模型配置与权重留痕，支持多版本并存切换。
 */
@Entity
@Table(name = "ai_model_version")
public class AiModelVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "model_code", nullable = false, unique = true, length = 64)
    private String modelCode;
    @Column(name = "model_name", nullable = false)
    private String modelName;
    @Column(nullable = false, length = 32)
    private String version;
    @Column(nullable = false)
    private String algorithm;
    @Column(name = "weights_json", length = 2000)
    private String weightsJson;
    @Column(nullable = false)
    private Boolean active = true;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getModelCode() { return modelCode; }
    public void setModelCode(String modelCode) { this.modelCode = modelCode; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
    public String getWeightsJson() { return weightsJson; }
    public void setWeightsJson(String weightsJson) { this.weightsJson = weightsJson; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

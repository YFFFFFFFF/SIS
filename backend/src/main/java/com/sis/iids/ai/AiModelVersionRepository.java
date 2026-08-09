package com.sis.iids.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiModelVersionRepository extends JpaRepository<AiModelVersion, Long> {

    Optional<AiModelVersion> findByModelCode(String modelCode);

    Optional<AiModelVersion> findFirstByActiveTrue();
}

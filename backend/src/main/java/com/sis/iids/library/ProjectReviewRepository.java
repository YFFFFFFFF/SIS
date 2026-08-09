package com.sis.iids.library;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectReviewRepository extends JpaRepository<ProjectReview, Long> {

    Optional<ProjectReview> findByProjectId(Long projectId);
}

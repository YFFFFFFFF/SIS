package com.sis.iids.library;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectTagRepository extends JpaRepository<ProjectTag, Long> {

    List<ProjectTag> findByProjectId(Long projectId);

    List<ProjectTag> findByTagIn(List<String> tags);

    void deleteByProjectId(Long projectId);
}

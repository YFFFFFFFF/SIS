package com.sis.iids.project;

import com.sis.iids.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ANALYST','INVESTMENT_ANALYST','PROJECT_MANAGER','ADMIN','SYSTEM_ADMINISTRATOR')")
    public ApiResponse<ProjectResponse> create(@Valid @RequestBody ProjectCreateRequest request) {
        return ApiResponse.ok(projectService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ANALYST','INVESTMENT_ANALYST','PROJECT_MANAGER','ADMIN','SYSTEM_ADMINISTRATOR')")
    public ApiResponse<ProjectResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody ProjectUpdateRequest request) {
        return ApiResponse.ok(projectService.update(id, request));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProjectResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(projectService.get(id));
    }

    @GetMapping
    public ApiResponse<List<ProjectResponse>> list() {
        return ApiResponse.ok(projectService.list());
    }
}
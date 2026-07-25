package com.sis.iids.project;

import com.sis.iids.common.error.BusinessException;
import com.sis.iids.common.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Transactional
    public ProjectResponse create(ProjectCreateRequest request) {
        String code = request.code().trim();
        if (projectRepository.existsByCode(code)) {
            throw new BusinessException(ErrorCode.CONFLICT, "Project code already exists");
        }

        Project project = new Project();
        project.setCode(code);
        project.setName(request.name().trim());
        project.setProjectType(blankToNull(request.projectType()));
        project.setDepartment(blankToNull(request.department()));
        project.setOwnerId(request.ownerId());
        project.setTags(blankToNull(request.tags()));
        project.setDescription(blankToNull(request.description()));
        return ProjectResponse.from(projectRepository.save(project));
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(Long id) {
        return ProjectResponse.from(projectRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Project not found")));
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> list() {
        return projectRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(ProjectResponse::from)
                .toList();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
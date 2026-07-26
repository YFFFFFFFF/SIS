package com.sis.iids.project;

import com.sis.iids.audit.AuditService;
import com.sis.iids.common.error.BusinessException;
import com.sis.iids.common.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final AuditService auditService;

    public ProjectService(ProjectRepository projectRepository, AuditService auditService) {
        this.projectRepository = projectRepository;
        this.auditService = auditService;
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
        Project saved = projectRepository.save(project);
        auditService.record("PROJECT_CREATED", "PROJECT", saved.getId().toString(), null, projectSnapshot(saved));
        return ProjectResponse.from(saved);
    }

    @Transactional
    public ProjectResponse update(Long id, ProjectUpdateRequest request) {
        Project project = findProject(id);
        String before = projectSnapshot(project);

        project.setName(request.name().trim());
        project.setProjectType(blankToNull(request.projectType()));
        project.setStatus(request.status());
        project.setDepartment(blankToNull(request.department()));
        project.setOwnerId(request.ownerId());
        project.setTags(blankToNull(request.tags()));
        project.setDescription(blankToNull(request.description()));

        Project saved = projectRepository.save(project);
        auditService.record("PROJECT_UPDATED", "PROJECT", saved.getId().toString(), before, projectSnapshot(saved));
        return ProjectResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(Long id) {
        return ProjectResponse.from(findProject(id));
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> list() {
        return projectRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(ProjectResponse::from)
                .toList();
    }

    private Project findProject(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Project not found"));
    }

    private String projectSnapshot(Project project) {
        return "code=%s;name=%s;status=%s;department=%s;tags=%s".formatted(
                project.getCode(),
                project.getName(),
                project.getStatus(),
                project.getDepartment(),
                project.getTags());
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

package com.sis.iids.library;

import com.sis.iids.audit.AuditService;
import com.sis.iids.calculation.CalculationResultEntity;
import com.sis.iids.calculation.CalculationResultRepository;
import com.sis.iids.calculation.CalculationStatus;
import com.sis.iids.calculation.CalculationTask;
import com.sis.iids.calculation.CalculationTaskRepository;
import com.sis.iids.common.error.BusinessException;
import com.sis.iids.common.error.ErrorCode;
import com.sis.iids.engine.financial.MetricCodes;
import com.sis.iids.project.Project;
import com.sis.iids.project.ProjectRepository;
import com.sis.iids.scenario.Scenario;
import com.sis.iids.scenario.ScenarioRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * R-16 项目库与知识沉淀服务（FR-03-03）。
 * 多维检索（状态/类型/标签/关键字）+ 结构化标签管理 + 已投运项目复盘（计划 vs 实际偏差）。
 */
@Service
public class LibraryService {

    private final ProjectRepository projectRepository;
    private final ScenarioRepository scenarioRepository;
    private final ProjectTagRepository tagRepository;
    private final ProjectReviewRepository reviewRepository;
    private final CalculationTaskRepository taskRepository;
    private final CalculationResultRepository resultRepository;
    private final AuditService auditService;

    public LibraryService(ProjectRepository projectRepository,
                          ScenarioRepository scenarioRepository,
                          ProjectTagRepository tagRepository,
                          ProjectReviewRepository reviewRepository,
                          CalculationTaskRepository taskRepository,
                          CalculationResultRepository resultRepository,
                          AuditService auditService) {
        this.projectRepository = projectRepository;
        this.scenarioRepository = scenarioRepository;
        this.tagRepository = tagRepository;
        this.reviewRepository = reviewRepository;
        this.taskRepository = taskRepository;
        this.resultRepository = resultRepository;
        this.auditService = auditService;
    }

    // ============================================================
    // 多维检索
    // ============================================================
    @Transactional(readOnly = true)
    public List<ProjectLibraryItem> search(String status, String projectType, String tag, String keyword) {
        // 标签筛选先解析出项目 ID 集
        List<Long> tagMatched = null;
        if (tag != null && !tag.isBlank()) {
            tagMatched = tagRepository.findByTagIn(List.of(tag.trim())).stream()
                    .map(ProjectTag::getProjectId).distinct().toList();
            if (tagMatched.isEmpty()) {
                return List.of();
            }
        }
        String kw = keyword == null || keyword.isBlank() ? null : keyword.trim().toLowerCase(java.util.Locale.ROOT);

        Map<Long, List<String>> tagsByProject = tagsByProject();
        List<ProjectLibraryItem> items = new ArrayList<>();
        for (Project p : projectRepository.findAllByOrderByCreatedAtDesc()) {
            if (status != null && !status.isBlank() && !p.getStatus().name().equalsIgnoreCase(status.trim())) {
                continue;
            }
            if (projectType != null && !projectType.isBlank()
                    && (p.getProjectType() == null || !p.getProjectType().equalsIgnoreCase(projectType.trim()))) {
                continue;
            }
            if (tagMatched != null && !tagMatched.contains(p.getId())) {
                continue;
            }
            if (kw != null && !(p.getName().toLowerCase(java.util.Locale.ROOT).contains(kw)
                    || p.getCode().toLowerCase(java.util.Locale.ROOT).contains(kw))) {
                continue;
            }
            BigDecimal[] latest = latestMetrics(p.getId());
            items.add(new ProjectLibraryItem(p.getId(), p.getCode(), p.getName(), p.getProjectType(),
                    p.getStatus().name(), p.getDepartment(),
                    tagsByProject.getOrDefault(p.getId(), fallbackTags(p.getTags())),
                    p.getDescription(), latest[0], latest[1],
                    reviewRepository.findByProjectId(p.getId()).isPresent()));
        }
        return items;
    }

    // ============================================================
    // 标签管理
    // ============================================================
    @Transactional
    public List<String> setTags(Long projectId, List<String> tags) {
        findProject(projectId);
        tagRepository.deleteByProjectId(projectId);
        List<String> cleaned = tags == null ? List.of() : tags.stream()
                .filter(t -> t != null && !t.isBlank())
                .map(t -> t.trim().toLowerCase(java.util.Locale.ROOT))
                .distinct().toList();
        for (String tag : cleaned) {
            ProjectTag entity = new ProjectTag();
            entity.setProjectId(projectId);
            entity.setTag(tag);
            tagRepository.save(entity);
        }
        auditService.record("PROJECT_TAGS_UPDATED", "PROJECT", projectId.toString(), null, String.join(",", cleaned));
        return cleaned;
    }

    @Transactional(readOnly = true)
    public List<String> listTags(Long projectId) {
        return tagRepository.findByProjectId(projectId).stream().map(ProjectTag::getTag).toList();
    }

    // ============================================================
    // 复盘
    // ============================================================
    @Transactional
    public ProjectReviewResponse saveReview(Long projectId, ProjectReviewRequest request) {
        Project project = findProject(projectId);
        Scenario scenario = scenarioRepository.findById(request.scenarioId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "对照测算方案不存在"));
        if (!scenario.getProjectId().equals(projectId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "对照方案不属于该项目");
        }
        ProjectReview review = reviewRepository.findByProjectId(projectId).orElseGet(ProjectReview::new);
        review.setProjectId(projectId);
        review.setScenarioId(request.scenarioId());
        review.setActualNpv(request.actualNpv());
        review.setActualIrr(request.actualIrr());
        review.setActualInvestment(request.actualInvestment());
        review.setActualPaybackYears(request.actualPaybackYears());
        review.setOperationStartDate(request.operationStartDate());
        review.setLessons(request.lessons());
        if (review.getCreatedBy() == null) {
            review.setCreatedBy(currentUsername());
        }
        review = reviewRepository.save(review);
        auditService.record("PROJECT_REVIEW_SAVED", "PROJECT_REVIEW", review.getId().toString(), null,
                "project=%s;scenario=%s".formatted(project.getCode(), scenario.getName()));
        return toReviewResponse(review, scenario);
    }

    @Transactional(readOnly = true)
    public ProjectReviewResponse getReview(Long projectId) {
        ProjectReview review = reviewRepository.findByProjectId(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "该项目暂无复盘记录"));
        Scenario scenario = review.getScenarioId() == null ? null
                : scenarioRepository.findById(review.getScenarioId()).orElse(null);
        return toReviewResponse(review, scenario);
    }

    // ============================================================
    // 内部
    // ============================================================
    private Project findProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "项目不存在"));
    }

    private Map<Long, List<String>> tagsByProject() {
        Map<Long, List<String>> map = new HashMap<>();
        for (ProjectTag t : tagRepository.findAll()) {
            map.computeIfAbsent(t.getProjectId(), k -> new ArrayList<>()).add(t.getTag());
        }
        return map;
    }

    /** 兼容：无结构化标签时回退解析逗号字符串。 */
    private List<String> fallbackTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return List.of(tags.split(",")).stream().map(String::trim).filter(t -> !t.isBlank()).toList();
    }

    /** 项目下所有方案的最新 SUCCESS 指标（取最新一次任务的 NPV/IRR）。 */
    private BigDecimal[] latestMetrics(Long projectId) {
        CalculationTask latest = null;
        for (Scenario scenario : scenarioRepository.findByProjectIdOrderByCreatedAtDesc(projectId)) {
            Optional<CalculationTask> task = taskRepository
                    .findFirstByScenarioIdAndStatusOrderByFinishedAtDesc(scenario.getId(), CalculationStatus.SUCCESS);
            if (task.isPresent() && (latest == null
                    || task.get().getFinishedAt().isAfter(latest.getFinishedAt()))) {
                latest = task.get();
            }
        }
        if (latest == null) {
            return new BigDecimal[]{null, null};
        }
        Map<String, BigDecimal> metrics = new HashMap<>();
        for (CalculationResultEntity r : resultRepository.findByTaskIdOrderByMetricCodeAsc(latest.getId())) {
            metrics.put(r.getMetricCode(), r.getMetricValue());
        }
        return new BigDecimal[]{metrics.get(MetricCodes.NPV), metrics.get(MetricCodes.IRR)};
    }

    private ProjectReviewResponse toReviewResponse(ProjectReview review, Scenario scenario) {
        BigDecimal plannedNpv = null;
        BigDecimal plannedIrr = null;
        BigDecimal plannedInvestment = null;
        BigDecimal plannedPayback = null;
        if (scenario != null) {
            Optional<CalculationTask> task = taskRepository
                    .findFirstByScenarioIdAndStatusOrderByFinishedAtDesc(scenario.getId(), CalculationStatus.SUCCESS);
            if (task.isPresent()) {
                Map<String, BigDecimal> metrics = new HashMap<>();
                for (CalculationResultEntity r : resultRepository.findByTaskIdOrderByMetricCodeAsc(task.get().getId())) {
                    metrics.put(r.getMetricCode(), r.getMetricValue());
                }
                plannedNpv = metrics.get(MetricCodes.NPV);
                plannedIrr = metrics.get(MetricCodes.IRR);
                plannedInvestment = metrics.get(MetricCodes.TOTAL_INVESTMENT);
                plannedPayback = metrics.get(MetricCodes.STATIC_PAYBACK_YEARS);
            }
        }
        return new ProjectReviewResponse(review.getId(), review.getProjectId(), review.getScenarioId(),
                scenario == null ? null : scenario.getName(),
                review.getActualNpv(), review.getActualIrr(), review.getActualInvestment(), review.getActualPaybackYears(),
                plannedNpv, plannedIrr, plannedInvestment, plannedPayback,
                deviation(review.getActualNpv(), plannedNpv), deviation(review.getActualIrr(), plannedIrr),
                review.getOperationStartDate(), review.getLessons(), review.getCreatedBy(),
                review.getCreatedAt(), review.getUpdatedAt());
    }

    /** 偏差率 = (实际 − 计划) / |计划|，计划为 0 或缺失时为 null。 */
    private BigDecimal deviation(BigDecimal actual, BigDecimal planned) {
        if (actual == null || planned == null || planned.signum() == 0) {
            return null;
        }
        return actual.subtract(planned).divide(planned.abs(), 6, RoundingMode.HALF_UP);
    }

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "system" : auth.getName();
    }
}

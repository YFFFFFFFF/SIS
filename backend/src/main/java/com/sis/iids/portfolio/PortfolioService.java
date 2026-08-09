package com.sis.iids.portfolio;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sis.iids.audit.AuditService;
import com.sis.iids.calculation.CalculationResultEntity;
import com.sis.iids.calculation.CalculationResultRepository;
import com.sis.iids.calculation.CalculationStatus;
import com.sis.iids.calculation.CalculationTask;
import com.sis.iids.calculation.CalculationTaskRepository;
import com.sis.iids.common.error.BusinessException;
import com.sis.iids.common.error.ErrorCode;
import com.sis.iids.engine.financial.MetricCodes;
import com.sis.iids.engine.portfolio.PortfolioCandidate;
import com.sis.iids.engine.portfolio.PortfolioEngine;
import com.sis.iids.engine.portfolio.PortfolioResult;
import com.sis.iids.project.Project;
import com.sis.iids.project.ProjectRepository;
import com.sis.iids.scenario.Scenario;
import com.sis.iids.scenario.ScenarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * R-13 投资组合优化服务（FR-03-02）。
 * 候选池 = 全部已测算成功的方案（每方案取最新 SUCCESS 任务指标，与 R-05/R-07 同口径），
 * 调用无状态 PortfolioEngine（oj! MIP，D1 选型 A）求解后落库留痕。
 */
@Service
public class PortfolioService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final PortfolioRunRepository runRepository;
    private final PortfolioMemberRepository memberRepository;
    private final ScenarioRepository scenarioRepository;
    private final ProjectRepository projectRepository;
    private final CalculationTaskRepository taskRepository;
    private final CalculationResultRepository resultRepository;
    private final AuditService auditService;
    private final PortfolioEngine portfolioEngine = new PortfolioEngine();
    private final String engineVersion;

    public PortfolioService(PortfolioRunRepository runRepository,
                            PortfolioMemberRepository memberRepository,
                            ScenarioRepository scenarioRepository,
                            ProjectRepository projectRepository,
                            CalculationTaskRepository taskRepository,
                            CalculationResultRepository resultRepository,
                            AuditService auditService,
                            @Value("${iids.engine-version:2.0.0}") String engineVersion) {
        this.runRepository = runRepository;
        this.memberRepository = memberRepository;
        this.scenarioRepository = scenarioRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.resultRepository = resultRepository;
        this.auditService = auditService;
        this.engineVersion = engineVersion;
    }

    @Transactional
    public PortfolioResponse optimize(PortfolioRequest request) {
        List<PortfolioCandidate> candidates = collectCandidates();
        if (candidates.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "没有已测算成功的方案可作为候选池");
        }
        PortfolioResult result;
        try {
            result = portfolioEngine.optimize(candidates, request.budget(), request.maxCount());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, ex.getMessage());
        }

        PortfolioRun run = new PortfolioRun();
        run.setBudget(request.budget());
        run.setMaxCount(request.maxCount());
        run.setCandidateCount(candidates.size());
        run.setTotalNpv(result.totalNpv());
        run.setTotalInvestment(result.totalInvestment());
        run.setExplanation(result.explanation());
        run.setFrontierJson(toJson(result.frontier().stream()
                .map(p -> new PortfolioResponse.FrontierPointView(p.budget(), p.npv(), p.investment(), p.count())).toList()));
        run.setEngineVersion(engineVersion);
        run.setCreatedBy(currentUsername());
        run = runRepository.save(run);

        // 成员：入选者 rankNo 按 NPV 降序 1..N，未入选 rankNo=null
        Map<Long, Integer> rankByScenarioId = new HashMap<>();
        int rank = 1;
        for (Long scenarioId : result.selectedScenarioIds()) {
            rankByScenarioId.put(scenarioId, rank++);
        }
        List<PortfolioMember> members = new ArrayList<>();
        for (PortfolioCandidate c : candidates) {
            PortfolioMember m = new PortfolioMember();
            m.setRunId(run.getId());
            m.setScenarioId(c.scenarioId());
            m.setScenarioName(c.scenarioName());
            m.setProjectName(c.projectName());
            m.setNpv(c.npv());
            m.setInvestment(c.investment());
            m.setIrr(c.irr());
            m.setSelected(rankByScenarioId.containsKey(c.scenarioId()));
            m.setRankNo(rankByScenarioId.get(c.scenarioId()));
            members.add(memberRepository.save(m));
        }
        auditService.record("PORTFOLIO_OPTIMIZED", "PORTFOLIO_RUN", run.getId().toString(), null,
                "budget=%s;candidates=%d;selected=%d;totalNpv=%s".formatted(
                        request.budget(), candidates.size(), result.selectedScenarioIds().size(), result.totalNpv()));
        return toResponse(run, members);
    }

    @Transactional(readOnly = true)
    public PortfolioResponse getRun(Long runId) {
        PortfolioRun run = runRepository.findById(runId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "组合优化运行不存在"));
        return toResponse(run, memberRepository.findByRunIdOrderByRankNoAsc(runId));
    }

    /** 候选池：全部方案的最新 SUCCESS 测算指标（NPV/IRR/总投资齐全者）。 */
    private List<PortfolioCandidate> collectCandidates() {
        Map<Long, String> projectNames = projectRepository.findAll().stream()
                .collect(Collectors.toMap(Project::getId, Project::getName));
        List<PortfolioCandidate> candidates = new ArrayList<>();
        for (Scenario scenario : scenarioRepository.findAll()) {
            Optional<CalculationTask> latest = taskRepository
                    .findFirstByScenarioIdAndStatusOrderByFinishedAtDesc(scenario.getId(), CalculationStatus.SUCCESS);
            if (latest.isEmpty()) {
                continue;
            }
            Map<String, BigDecimal> metrics = new HashMap<>();
            for (CalculationResultEntity r : resultRepository.findByTaskIdOrderByMetricCodeAsc(latest.get().getId())) {
                metrics.put(r.getMetricCode(), r.getMetricValue());
            }
            BigDecimal npv = metrics.get(MetricCodes.NPV);
            BigDecimal investment = metrics.get(MetricCodes.TOTAL_INVESTMENT);
            if (npv == null || investment == null || investment.signum() <= 0) {
                continue;
            }
            candidates.add(new PortfolioCandidate(scenario.getId(), scenario.getName(),
                    projectNames.getOrDefault(scenario.getProjectId(), "未知项目"),
                    npv, investment, metrics.get(MetricCodes.IRR)));
        }
        return candidates;
    }

    private PortfolioResponse toResponse(PortfolioRun run, List<PortfolioMember> members) {
        // 入选者 rank 升序在前，未入选在后按 NPV 降序
        List<PortfolioMember> ordered = new ArrayList<>(members);
        ordered.sort((a, b) -> {
            if (a.getRankNo() != null && b.getRankNo() != null) {
                return a.getRankNo() - b.getRankNo();
            }
            if (a.getRankNo() != null) {
                return -1;
            }
            if (b.getRankNo() != null) {
                return 1;
            }
            return b.getNpv().compareTo(a.getNpv());
        });
        return new PortfolioResponse(run.getId(), run.getBudget(), run.getMaxCount(), run.getCandidateCount(),
                run.getTotalNpv(), run.getTotalInvestment(), run.getExplanation(),
                ordered.stream().map(m -> new PortfolioResponse.MemberView(m.getScenarioId(), m.getScenarioName(),
                        m.getProjectName(), m.getNpv(), m.getInvestment(), m.getIrr(),
                        Boolean.TRUE.equals(m.getSelected()), m.getRankNo())).toList(),
                fromJson(run.getFrontierJson(), new TypeReference<>() {}),
                run.getEngineVersion(), run.getCreatedBy(), run.getCreatedAt());
    }

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "system" : auth.getName();
    }

    private String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("组合优化结果序列化失败", ex);
        }
    }

    private <T> T fromJson(String json, TypeReference<T> type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("组合优化结果反序列化失败", ex);
        }
    }
}

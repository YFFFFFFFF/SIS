package com.sis.iids.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sis.iids.audit.AuditService;
import com.sis.iids.calculation.CalculationResultEntity;
import com.sis.iids.calculation.CalculationResultRepository;
import com.sis.iids.calculation.CalculationService;
import com.sis.iids.calculation.CalculationStatus;
import com.sis.iids.calculation.CalculationTask;
import com.sis.iids.calculation.CalculationTaskRepository;
import com.sis.iids.common.error.BusinessException;
import com.sis.iids.common.error.ErrorCode;
import com.sis.iids.engine.ai.ScoreFeatures;
import com.sis.iids.engine.ai.ScoreOutcome;
import com.sis.iids.engine.ai.ScoringEngine;
import com.sis.iids.engine.breakeven.BreakEvenEngine;
import com.sis.iids.engine.breakeven.BreakEvenResult;
import com.sis.iids.engine.financial.FinancialInput;
import com.sis.iids.engine.financial.MetricCodes;
import com.sis.iids.library.ProjectReview;
import com.sis.iids.library.ProjectReviewRepository;
import com.sis.iids.project.ProjectRepository;
import com.sis.iids.scenario.ParameterSet;
import com.sis.iids.scenario.ParameterSetRepository;
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
 * R-17 AI 决策引擎服务（FR-05，D4 选型 A：同仓 ai 模块，接口预留可拆微服务）。
 * 历史运营数据库 + 智能参数推荐（依据来源可解释）+ 智能打分（六因子加权，不替代人工决策）。
 */
@Service
public class AiService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String DISCLAIMER =
            "本评分为规则化加权模型的参考输出，因子得分与权重全量公开，供决策者参考，不构成投资建议，不替代人工决策。";

    private final AiOperationRecordRepository operationRepository;
    private final AiModelVersionRepository modelRepository;
    private final ScenarioRepository scenarioRepository;
    private final ParameterSetRepository parameterSetRepository;
    private final ProjectRepository projectRepository;
    private final ProjectReviewRepository reviewRepository;
    private final CalculationTaskRepository taskRepository;
    private final CalculationResultRepository resultRepository;
    private final CalculationService calculationService;
    private final AuditService auditService;
    private final ScoringEngine scoringEngine = new ScoringEngine();
    private final BreakEvenEngine breakEvenEngine = new BreakEvenEngine();

    public AiService(AiOperationRecordRepository operationRepository,
                     AiModelVersionRepository modelRepository,
                     ScenarioRepository scenarioRepository,
                     ParameterSetRepository parameterSetRepository,
                     ProjectRepository projectRepository,
                     ProjectReviewRepository reviewRepository,
                     CalculationTaskRepository taskRepository,
                     CalculationResultRepository resultRepository,
                     CalculationService calculationService,
                     AuditService auditService) {
        this.operationRepository = operationRepository;
        this.modelRepository = modelRepository;
        this.scenarioRepository = scenarioRepository;
        this.parameterSetRepository = parameterSetRepository;
        this.projectRepository = projectRepository;
        this.reviewRepository = reviewRepository;
        this.taskRepository = taskRepository;
        this.resultRepository = resultRepository;
        this.calculationService = calculationService;
        this.auditService = auditService;
    }

    // ============================================================
    // 历史运营数据
    // ============================================================
    @Transactional(readOnly = true)
    public List<OperationRecordResponse> listOperationRecords(Long projectId) {
        return operationRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(this::toOperationResponse).toList();
    }

    @Transactional
    public OperationRecordResponse addOperationRecord(Long projectId, OperationRecordRequest request) {
        if (!projectRepository.existsById(projectId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "项目不存在");
        }
        AiOperationRecord record = new AiOperationRecord();
        record.setProjectId(projectId);
        record.setPeriod(request.period().trim());
        record.setActualRevenue(request.actualRevenue());
        record.setActualCost(request.actualCost());
        record.setActualOutput(request.actualOutput());
        record.setActualNpv(request.actualNpv());
        record.setActualIrr(request.actualIrr());
        record.setDeviationRatio(request.deviationRatio());
        record.setVerified(Boolean.TRUE.equals(request.verified()));
        record.setNote(request.note());
        record.setCreatedBy(currentUsername());
        record = operationRepository.save(record);
        auditService.record("AI_OPERATION_RECORDED", "AI_OPERATION_RECORD", record.getId().toString(), null,
                "projectId=%s;period=%s;verified=%s".formatted(projectId, record.getPeriod(), record.getVerified()));
        return toOperationResponse(record);
    }

    // ============================================================
    // 智能参数推荐
    // ============================================================
    @Transactional(readOnly = true)
    public ParamRecommendationResponse recommendParams(Long scenarioId) {
        findScenario(scenarioId);
        ParameterSet params = parameterSetRepository.findByScenarioId(scenarioId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "参数集不存在，请先保存参数"));

        BigDecimal deviationMedian = deviationMedian();
        List<ParamRecommendationResponse.Item> items = new ArrayList<>();

        // WACC：历史偏差大 → 建议区间上浮（风险补偿）
        BigDecimal wacc = params.getWacc();
        BigDecimal waccHigh = wacc;
        String waccBasis = "无历史复盘数据，维持当前基准假设";
        if (deviationMedian != null && deviationMedian.abs().compareTo(new BigDecimal("0.10")) > 0) {
            waccHigh = wacc.add(new BigDecimal("0.02"));
            waccBasis = "历史复盘偏差率中位数 " + pct(deviationMedian) + " 超过 10%，建议 WACC 区间上浮 0~2pct 作为风险补偿";
        }
        items.add(new ParamRecommendationResponse.Item("wacc", wacc, wacc, waccHigh, waccBasis));

        // 售价/成本：常规敏感性区间 + 历史偏差说明
        BigDecimal price = params.getPricePerUnit();
        items.add(new ParamRecommendationResponse.Item("pricePerUnit", price,
                price.multiply(new BigDecimal("0.85")), price.multiply(new BigDecimal("1.10")),
                "依据敏感性常规区间：售价下浮 15%（保守）至上浮 10%（乐观）；历史偏差 "
                        + (deviationMedian == null ? "无" : pct(deviationMedian))));

        BigDecimal unitCost = params.getUnitCost();
        items.add(new ParamRecommendationResponse.Item("unitCost", unitCost,
                unitCost.multiply(new BigDecimal("0.95")), unitCost.multiply(new BigDecimal("1.15")),
                "成本超支为历史项目主要偏差来源，建议以 -5%（降本）~ +15%（超支防御）为敏感性区间"));

        // 敏感性建议区间（反哺）
        BigDecimal suggestedRange = deviationMedian == null ? new BigDecimal("0.20")
                : deviationMedian.abs().max(new BigDecimal("0.20"));
        items.add(new ParamRecommendationResponse.Item("sensitivityRange", new BigDecimal("0.20"),
                new BigDecimal("0.20"), suggestedRange,
                deviationMedian == null ? "无历史复盘数据，建议沿用默认 ±20% 敏感性区间"
                        : "历史复盘偏差率中位数 " + pct(deviationMedian) + "，建议敏感性区间覆盖 ±" + pct(suggestedRange)));

        return new ParamRecommendationResponse(scenarioId, items,
                deviationMedian == null ? "依据：通用基准假设（无历史复盘数据）"
                        : "依据：" + operationRepository.findByVerifiedTrue().size() + " 条已校验运营记录 + 项目库复盘，偏差率中位数 " + pct(deviationMedian));
    }

    // ============================================================
    // 智能打分
    // ============================================================
    @Transactional(readOnly = true)
    public ScoreResponse scoreScenario(Long scenarioId) {
        Scenario scenario = findScenario(scenarioId);
        Map<String, BigDecimal> metrics = latestMetrics(scenarioId);
        if (metrics.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "方案尚无成功测算结果，无法打分");
        }
        AiModelVersion model = modelRepository.findFirstByActiveTrue()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "无可用打分模型"));

        ScoreFeatures features = new ScoreFeatures(
                metrics.get(MetricCodes.NPV), metrics.get(MetricCodes.IRR), waccOf(scenarioId),
                metrics.get(MetricCodes.STATIC_PAYBACK_YEARS),
                scenario.getHorizonYears(), null, bepUtilization(scenarioId), deviationMedian());

        ScoreOutcome outcome = scoringEngine.score(features, parseWeights(model.getWeightsJson()));

        List<ScoreResponse.FactorScore> factors = outcome.factors().stream()
                .map(f -> new ScoreResponse.FactorScore(f.factor(), f.name(), f.rawValue(),
                        f.score(), f.weight(), f.weighted(), f.explain()))
                .toList();
        return new ScoreResponse(scenarioId, model.getModelCode(), model.getVersion(),
                outcome.totalScore(), outcome.label(), DISCLAIMER, factors);
    }

    // ============================================================
    // 内部
    // ============================================================
    private Scenario findScenario(Long scenarioId) {
        return scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "测算方案不存在"));
    }

    private Map<String, BigDecimal> latestMetrics(Long scenarioId) {
        Optional<CalculationTask> latest = taskRepository
                .findFirstByScenarioIdAndStatusOrderByFinishedAtDesc(scenarioId, CalculationStatus.SUCCESS);
        if (latest.isEmpty()) {
            return Map.of();
        }
        Map<String, BigDecimal> metrics = new HashMap<>();
        for (CalculationResultEntity r : resultRepository.findByTaskIdOrderByMetricCodeAsc(latest.get().getId())) {
            metrics.put(r.getMetricCode(), r.getMetricValue());
        }
        return metrics;
    }

    private BigDecimal waccOf(Long scenarioId) {
        return parameterSetRepository.findByScenarioId(scenarioId)
                .map(ParameterSet::getWacc).orElse(null);
    }

    /** 盈亏平衡产能利用率（实时复算，不可解时为 null）。 */
    private BigDecimal bepUtilization(Long scenarioId) {
        try {
            FinancialInput input = calculationService.buildBaseInput(scenarioId);
            BreakEvenResult result = breakEvenEngine.analyze(input);
            return result.solvable() ? result.bepUtilization() : null;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /** 历史偏差率中位数：已校验运营记录 + 项目库复盘偏差（反哺数据源）。 */
    private BigDecimal deviationMedian() {
        List<BigDecimal> deviations = new ArrayList<>();
        for (AiOperationRecord r : operationRepository.findByVerifiedTrue()) {
            if (r.getDeviationRatio() != null) {
                deviations.add(r.getDeviationRatio());
            }
        }
        for (ProjectReview review : reviewRepository.findAll()) {
            if (review.getActualNpv() != null) {
                // 复盘偏差以 NPV 偏差率代理（与 R-16 同口径）
                Optional<Scenario> scenario = review.getScenarioId() == null ? Optional.empty()
                        : scenarioRepository.findById(review.getScenarioId());
                if (scenario.isPresent()) {
                    Map<String, BigDecimal> metrics = latestMetrics(scenario.get().getId());
                    BigDecimal planned = metrics.get(MetricCodes.NPV);
                    if (planned != null && planned.signum() != 0) {
                        deviations.add(review.getActualNpv().subtract(planned)
                                .divide(planned.abs(), 6, RoundingMode.HALF_UP));
                    }
                }
            }
        }
        if (deviations.isEmpty()) {
            return null;
        }
        deviations.sort(null);
        int n = deviations.size();
        if (n % 2 == 1) {
            return deviations.get(n / 2);
        }
        return deviations.get(n / 2 - 1).add(deviations.get(n / 2))
                .divide(BigDecimal.valueOf(2), 6, RoundingMode.HALF_UP);
    }

    private Map<String, BigDecimal> parseWeights(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            return null;
        }
    }

    private String pct(BigDecimal v) {
        return v.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP) + "%";
    }

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "system" : auth.getName();
    }

    private OperationRecordResponse toOperationResponse(AiOperationRecord r) {
        return new OperationRecordResponse(r.getId(), r.getProjectId(), r.getPeriod(),
                r.getActualRevenue(), r.getActualCost(), r.getActualOutput(), r.getActualNpv(), r.getActualIrr(),
                r.getDeviationRatio(), Boolean.TRUE.equals(r.getVerified()), r.getNote(),
                r.getCreatedBy(), r.getCreatedAt());
    }
}

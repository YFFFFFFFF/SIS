package com.sis.iids.montecarlo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sis.iids.audit.AuditService;
import com.sis.iids.calculation.CalculationService;
import com.sis.iids.common.error.BusinessException;
import com.sis.iids.common.error.ErrorCode;
import com.sis.iids.engine.financial.FinancialInput;
import com.sis.iids.engine.montecarlo.DistributionSpec;
import com.sis.iids.engine.montecarlo.MonteCarloEngine;
import com.sis.iids.engine.montecarlo.MonteCarloResult;
import com.sis.iids.engine.montecarlo.MonteCarloVariable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * R-11 蒙特卡洛概率分析服务（FR-02-03）。
 * 组装基准输入 → 调用无状态 MonteCarloEngine → 持久化运行记录（红线 R11：种子入库可复现）。
 */
@Service
public class MonteCarloService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final MonteCarloRunRepository runRepository;
    private final CalculationService calculationService;
    private final AuditService auditService;
    private final MonteCarloEngine monteCarloEngine = new MonteCarloEngine();
    private final String engineVersion;

    public MonteCarloService(MonteCarloRunRepository runRepository,
                             CalculationService calculationService,
                             AuditService auditService,
                             @Value("${iids.engine-version:2.0.0}") String engineVersion) {
        this.runRepository = runRepository;
        this.calculationService = calculationService;
        this.auditService = auditService;
        this.engineVersion = engineVersion;
    }

    @Transactional
    public MonteCarloResponse run(Long scenarioId, MonteCarloRequest request) {
        FinancialInput baseInput = calculationService.buildBaseInput(scenarioId);
        long seed = request.seed() != null ? request.seed() : ThreadLocalRandom.current().nextLong();

        List<DistributionSpec> specs;
        MonteCarloResult result;
        try {
            specs = request.variables().stream().map(v -> new DistributionSpec(
                    MonteCarloVariable.from(v.variable()), v.type(),
                    v.min(), v.mode(), v.max(), v.mean(), v.stdDev())).toList();
            String metric = request.targetMetric() == null || request.targetMetric().isBlank()
                    ? "NPV" : request.targetMetric();
            result = monteCarloEngine.run(baseInput, metric, specs, request.iterations(), seed);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, ex.getMessage());
        }

        MonteCarloRun run = new MonteCarloRun();
        run.setScenarioId(scenarioId);
        run.setTaskId(request.taskId());
        run.setTargetMetric(result.targetMetric());
        run.setIterations(result.iterations());
        run.setSeed(seed);
        run.setVariablesJson(toJson(specs.stream().map(this::toSpecView).toList()));
        run.setMeanValue(result.mean());
        run.setStdDev(result.stdDev());
        run.setProbPositive(result.probPositive());
        run.setVar95(result.var95());
        run.setP5(result.p5());
        run.setP50(result.p50());
        run.setP95(result.p95());
        run.setMinValue(result.min());
        run.setMaxValue(result.max());
        run.setHistogramJson(toJson(result.histogram().stream()
                .map(b -> new MonteCarloResponse.HistogramBucketView(b.from(), b.to(), b.count(), b.ratio())).toList()));
        run.setCumulativeJson(toJson(result.cumulative().stream()
                .map(p -> new MonteCarloResponse.CumulativePointView(p.value(), p.probability())).toList()));
        run.setEngineVersion(engineVersion);
        run = runRepository.save(run);
        auditService.record("MONTE_CARLO_RUN", "MONTE_CARLO_RUN", run.getId().toString(), null,
                "scenarioId=%s;metric=%s;iterations=%s;seed=%s;probPositive=%s".formatted(
                        scenarioId, result.targetMetric(), result.iterations(), seed, result.probPositive()));
        return toResponse(run);
    }

    @Transactional(readOnly = true)
    public MonteCarloResponse getRun(Long runId) {
        return toResponse(runRepository.findById(runId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "蒙特卡洛运行不存在")));
    }

    @Transactional(readOnly = true)
    public List<MonteCarloResponse> listRuns(Long scenarioId) {
        return runRepository.findByScenarioIdOrderByCreatedAtDesc(scenarioId).stream()
                .map(this::toResponse).toList();
    }

    private MonteCarloResponse toResponse(MonteCarloRun run) {
        return new MonteCarloResponse(run.getId(), run.getScenarioId(), run.getTargetMetric(),
                run.getIterations(), run.getSeed(),
                fromJson(run.getVariablesJson(), new TypeReference<>() {}),
                run.getMeanValue(), run.getStdDev(), run.getProbPositive(), run.getVar95(),
                run.getP5(), run.getP50(), run.getP95(), run.getMinValue(), run.getMaxValue(),
                fromJson(run.getHistogramJson(), new TypeReference<>() {}),
                fromJson(run.getCumulativeJson(), new TypeReference<>() {}));
    }

    private MonteCarloResponse.VariableSpecView toSpecView(DistributionSpec spec) {
        return new MonteCarloResponse.VariableSpecView(spec.variable().name(), spec.type(),
                spec.min(), spec.mode(), spec.max(), spec.mean(), spec.stdDev());
    }

    private String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("蒙特卡洛结果序列化失败", ex);
        }
    }

    private <T> T fromJson(String json, TypeReference<T> type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("蒙特卡洛结果反序列化失败", ex);
        }
    }
}

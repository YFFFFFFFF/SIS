package com.sis.iids.sensitivity;

import com.sis.iids.calculation.CalculationService;
import com.sis.iids.common.error.BusinessException;
import com.sis.iids.common.error.ErrorCode;
import com.sis.iids.engine.financial.FinancialInput;
import com.sis.iids.engine.sensitivity.FactorSpec;
import com.sis.iids.engine.sensitivity.SensitivityEngine;
import com.sis.iids.engine.sensitivity.SensitivityResult;
import com.sis.iids.engine.sensitivity.SensitivityVariable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SensitivityService {

    private final SensitivityRunRepository runRepository;
    private final SensitivityCellRepository cellRepository;
    private final CalculationService calculationService;
    private final SensitivityEngine sensitivityEngine = new SensitivityEngine();
    private final String engineVersion;

    public SensitivityService(SensitivityRunRepository runRepository,
                              SensitivityCellRepository cellRepository,
                              CalculationService calculationService,
                              @Value("${iids.engine-version:2.0.0}") String engineVersion) {
        this.runRepository = runRepository;
        this.cellRepository = cellRepository;
        this.calculationService = calculationService;
        this.engineVersion = engineVersion;
    }

    @Transactional
    public SensitivityResponse analyze(Long scenarioId, SensitivityRequest request) {
        FinancialInput baseInput = calculationService.buildBaseInput(scenarioId);
        String targetMetric = request.targetMetric() == null || request.targetMetric().isBlank()
                ? "NPV" : request.targetMetric().trim().toUpperCase(java.util.Locale.ROOT);

        FactorSpec factor1;
        FactorSpec factor2;
        SensitivityResult result;
        try {
            factor1 = new FactorSpec(SensitivityVariable.from(request.variable1()),
                    request.range1(), request.steps1());
            boolean twoFactor = request.variable2() != null && !request.variable2().isBlank();
            factor2 = twoFactor
                    ? new FactorSpec(SensitivityVariable.from(request.variable2()), request.range2(),
                    request.steps2() == null ? 5 : request.steps2())
                    : null;
            result = sensitivityEngine.analyze(baseInput, targetMetric, factor1, factor2);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, ex.getMessage());
        }

        SensitivityRun run = new SensitivityRun();
        run.setScenarioId(scenarioId);
        run.setTaskId(request.taskId());
        run.setTargetMetric(targetMetric);
        run.setVariable1(factor1.variable().name());
        run.setRange1(request.range1());
        run.setSteps1(request.steps1());
        run.setVariable2(factor2 == null ? null : factor2.variable().name());
        run.setRange2(request.range2());
        run.setSteps2(request.steps2());
        run.setBaseValue(result.baseValue());
        run.setStatus("SUCCESS");
        run.setEngineVersion(engineVersion);
        run = runRepository.save(run);

        for (SensitivityResult.SensitivityCell cell : result.matrix()) {
            SensitivityCell entity = new SensitivityCell();
            entity.setRunId(run.getId());
            entity.setFactor1(cell.factor1());
            entity.setFactor2(cell.factor2());
            entity.setMetricValue(cell.metricValue());
            cellRepository.save(entity);
        }

        return toResponse(run.getId(), scenarioId, result);
    }

    @Transactional(readOnly = true)
    public SensitivityResponse getRun(Long runId) {
        SensitivityRun run = runRepository.findById(runId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "敏感性分析运行不存在"));
        List<SensitivityCell> cells = cellRepository.findByRunIdOrderByFactor1AscFactor2Asc(runId);
        List<SensitivityResponse.Cell> matrix = cells.stream()
                .map(c -> new SensitivityResponse.Cell(c.getFactor1(), c.getFactor2(), c.getMetricValue()))
                .toList();
        return new SensitivityResponse(run.getId(), run.getScenarioId(), run.getTargetMetric(),
                run.getVariable1(), run.getVariable2(), run.getBaseValue(),
                null, null, null, null, null, null, matrix);
    }

    @Transactional(readOnly = true)
    public List<SensitivityResponse> listRuns(Long scenarioId) {
        return runRepository.findByScenarioIdOrderByCreatedAtDesc(scenarioId).stream()
                .map(r -> new SensitivityResponse(r.getId(), r.getScenarioId(), r.getTargetMetric(),
                        r.getVariable1(), r.getVariable2(), r.getBaseValue(),
                        null, null, null, null, null, null, List.of()))
                .toList();
    }

    private SensitivityResponse toResponse(Long runId, Long scenarioId, SensitivityResult r) {
        List<SensitivityResponse.Cell> matrix = r.matrix().stream()
                .map(c -> new SensitivityResponse.Cell(c.factor1(), c.factor2(), c.metricValue()))
                .toList();
        return new SensitivityResponse(runId, scenarioId, r.targetMetric(),
                r.factor1().variable().name(), r.factor2() == null ? null : r.factor2().variable().name(),
                r.baseValue(), r.coefficient1(), r.coefficient2(),
                r.criticalFactor1(), r.criticalFactor2(), r.level1(), r.level2(), matrix);
    }
}

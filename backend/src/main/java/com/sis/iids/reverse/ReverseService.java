package com.sis.iids.reverse;

import com.sis.iids.audit.AuditService;
import com.sis.iids.calculation.CalculationService;
import com.sis.iids.common.error.BusinessException;
import com.sis.iids.common.error.ErrorCode;
import com.sis.iids.engine.financial.FinancialInput;
import com.sis.iids.engine.reverse.ReverseEngine;
import com.sis.iids.engine.reverse.ReverseResult;
import com.sis.iids.engine.reverse.ReverseTarget;
import com.sis.iids.engine.reverse.ReverseVariable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * R-09 目标反算服务（FR-01-05）：组装基准输入 → 调用无状态 ReverseEngine → 持久化运行记录。
 */
@Service
public class ReverseService {

    private final ReverseRunRepository runRepository;
    private final CalculationService calculationService;
    private final AuditService auditService;
    private final ReverseEngine reverseEngine = new ReverseEngine();
    private final String engineVersion;

    public ReverseService(ReverseRunRepository runRepository,
                          CalculationService calculationService,
                          AuditService auditService,
                          @Value("${iids.engine-version:2.0.0}") String engineVersion) {
        this.runRepository = runRepository;
        this.calculationService = calculationService;
        this.auditService = auditService;
        this.engineVersion = engineVersion;
    }

    @Transactional
    public ReverseResponse solve(Long scenarioId, ReverseRequest request) {
        FinancialInput baseInput = calculationService.buildBaseInput(scenarioId);
        ReverseResult result;
        try {
            ReverseTarget target = ReverseTarget.from(request.targetMetric());
            ReverseVariable variable = ReverseVariable.from(request.variable());
            result = reverseEngine.solve(baseInput, target, request.targetValue(), variable);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, ex.getMessage());
        }

        ReverseRun run = new ReverseRun();
        run.setScenarioId(scenarioId);
        run.setTaskId(request.taskId());
        run.setTargetMetric(result.targetMetric());
        run.setTargetValue(result.targetValue());
        run.setVariable(result.variable());
        run.setFactor(result.factor());
        run.setSolvedValue(result.solvedValue());
        run.setBaseValue(result.baseValue());
        run.setAchievedValue(result.achievedValue());
        run.setFeasible(result.feasible());
        run.setIterations(result.iterations());
        run.setSensitivityNote(result.sensitivityNote());
        run.setBoundaryNote(result.boundaryNote());
        run.setEngineVersion(engineVersion);
        run = runRepository.save(run);
        auditService.record("REVERSE_RUN_SOLVED", "REVERSE_RUN", run.getId().toString(), null,
                "scenarioId=%s;target=%s %s;variable=%s;feasible=%s".formatted(scenarioId,
                        result.targetMetric(), result.targetValue(), result.variable(), result.feasible()));
        return toResponse(run);
    }

    @Transactional(readOnly = true)
    public ReverseResponse getRun(Long runId) {
        return toResponse(runRepository.findById(runId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "目标反算运行不存在")));
    }

    @Transactional(readOnly = true)
    public List<ReverseResponse> listRuns(Long scenarioId) {
        return runRepository.findByScenarioIdOrderByCreatedAtDesc(scenarioId).stream()
                .map(this::toResponse).toList();
    }

    private ReverseResponse toResponse(ReverseRun run) {
        return new ReverseResponse(run.getId(), run.getScenarioId(), run.getTargetMetric(), run.getTargetValue(),
                run.getVariable(), run.getFactor(), run.getSolvedValue(), run.getBaseValue(), run.getAchievedValue(),
                run.getFeasible(), run.getIterations(), run.getSensitivityNote(), run.getBoundaryNote());
    }
}

package com.sis.iids.scenario;

import com.sis.iids.audit.AuditService;
import com.sis.iids.common.error.BusinessException;
import com.sis.iids.common.error.ErrorCode;
import com.sis.iids.project.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ScenarioService {

    private final ProjectRepository projectRepository;
    private final ScenarioRepository scenarioRepository;
    private final ParameterSetRepository parameterSetRepository;
    private final AuditService auditService;

    public ScenarioService(ProjectRepository projectRepository,
                           ScenarioRepository scenarioRepository,
                           ParameterSetRepository parameterSetRepository,
                           AuditService auditService) {
        this.projectRepository = projectRepository;
        this.scenarioRepository = scenarioRepository;
        this.parameterSetRepository = parameterSetRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ScenarioResponse create(Long projectId, ScenarioCreateRequest request) {
        if (!projectRepository.existsById(projectId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Project not found");
        }
        Scenario scenario = new Scenario();
        scenario.setProjectId(projectId);
        scenario.setName(request.name().trim());
        scenario.setHorizonYears(request.horizonYears());
        scenario.setConstructionYears(request.constructionYears());
        scenario.setRemarks(blankToNull(request.remarks()));
        Scenario saved = scenarioRepository.save(scenario);
        auditService.record("SCENARIO_CREATED", "SCENARIO", saved.getId().toString(), null, scenarioSnapshot(saved));
        return ScenarioResponse.from(saved);
    }

    @Transactional
    public ScenarioResponse update(Long id, ScenarioUpdateRequest request) {
        Scenario scenario = findScenario(id);
        String before = scenarioSnapshot(scenario);

        scenario.setName(request.name().trim());
        scenario.setStatus(request.status());
        scenario.setHorizonYears(request.horizonYears());
        scenario.setConstructionYears(request.constructionYears());
        scenario.setRemarks(blankToNull(request.remarks()));

        Scenario saved = scenarioRepository.save(scenario);
        auditService.record("SCENARIO_UPDATED", "SCENARIO", saved.getId().toString(), before, scenarioSnapshot(saved));
        return ScenarioResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ScenarioResponse get(Long id) {
        return ScenarioResponse.from(findScenario(id));
    }

    @Transactional(readOnly = true)
    public List<ScenarioResponse> listByProject(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Project not found");
        }
        return scenarioRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(ScenarioResponse::from)
                .toList();
    }

    @Transactional
    public ParameterSetResponse upsertParameters(Long scenarioId, ParameterSetRequest request) {
        findScenario(scenarioId);
        ParameterSet parameterSet = parameterSetRepository.findByScenarioId(scenarioId)
                .orElseGet(ParameterSet::new);
        parameterSet.setScenarioId(scenarioId);
        parameterSet.setWacc(request.wacc());
        parameterSet.setWaccSource(blankToNull(request.waccSource()));
        parameterSet.setTaxRate(request.taxRate());
        parameterSet.setDepreciationYears(request.depreciationYears());
        parameterSet.setResidualRate(request.residualRate());
        parameterSet.setLoanRatioLimit(request.loanRatioLimit());
        parameterSet.setPricePerUnit(request.pricePerUnit());
        parameterSet.setUnitCost(request.unitCost());
        parameterSet.setAnnualOutput(request.annualOutput());
        parameterSet.setFixedOperatingCost(request.fixedOperatingCost());
        parameterSet.setFormulaVersion(blankToNull(request.formulaVersion()));
        return ParameterSetResponse.from(parameterSetRepository.save(parameterSet));
    }

    @Transactional(readOnly = true)
    public ParameterSetResponse getParameters(Long scenarioId) {
        findScenario(scenarioId);
        return ParameterSetResponse.from(parameterSetRepository.findByScenarioId(scenarioId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Parameter set not found")));
    }

    private Scenario findScenario(Long id) {
        return scenarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Scenario not found"));
    }

    private String scenarioSnapshot(Scenario scenario) {
        return "projectId=%s;name=%s;status=%s;horizonYears=%s;constructionYears=%s".formatted(
                scenario.getProjectId(),
                scenario.getName(),
                scenario.getStatus(),
                scenario.getHorizonYears(),
                scenario.getConstructionYears());
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

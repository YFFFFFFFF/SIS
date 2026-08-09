package com.sis.iids.scenario;

import com.sis.iids.audit.AuditService;
import com.sis.iids.collab.CollabService;
import com.sis.iids.collab.FieldLockService;
import com.sis.iids.common.error.BusinessException;
import com.sis.iids.common.error.ErrorCode;
import com.sis.iids.project.ProjectRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ScenarioService {

    private final ProjectRepository projectRepository;
    private final ScenarioRepository scenarioRepository;
    private final ParameterSetRepository parameterSetRepository;
    private final AuditService auditService;
    private final FieldLockService fieldLockService;
    private final CollabService collabService;

    public ScenarioService(ProjectRepository projectRepository,
                           ScenarioRepository scenarioRepository,
                           ParameterSetRepository parameterSetRepository,
                           AuditService auditService,
                           FieldLockService fieldLockService,
                           CollabService collabService) {
        this.projectRepository = projectRepository;
        this.scenarioRepository = scenarioRepository;
        this.parameterSetRepository = parameterSetRepository;
        this.auditService = auditService;
        this.fieldLockService = fieldLockService;
        this.collabService = collabService;
    }

    @Transactional
    public ScenarioResponse create(Long projectId, ScenarioCreateRequest request) {
        if (!projectRepository.existsById(projectId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "项目不存在");
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
            throw new BusinessException(ErrorCode.NOT_FOUND, "项目不存在");
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

        // R-15c 字段锁强制拦截 + 变更留痕：先构造“将要生效”的值快照，与库内旧值逐字段 diff
        Map<String, String> newValues = new LinkedHashMap<>();
        newValues.put("wacc", num(request.wacc()));
        newValues.put("taxRate", num(request.taxRate()));
        newValues.put("depreciationYears", str(request.depreciationYears()));
        newValues.put("residualRate", num(request.residualRate()));
        newValues.put("loanRatioLimit", num(request.loanRatioLimit()));
        newValues.put("pricePerUnit", num(request.pricePerUnit()));
        newValues.put("unitCost", num(request.unitCost()));
        newValues.put("annualOutput", num(request.annualOutput()));
        newValues.put("fixedOperatingCost", num(request.fixedOperatingCost()));
        newValues.put("depreciationPolicy", blankToNullOrDefault(request.depreciationPolicy(), "STRAIGHT_LINE"));
        newValues.put("amortizationYears", str(request.amortizationYears() == null ? 0 : request.amortizationYears()));
        newValues.put("amortizableAmount", num(request.amortizableAmount() == null ? java.math.BigDecimal.ZERO : request.amortizableAmount()));
        newValues.put("repaymentMethod", blankToNullOrDefault(request.repaymentMethod(), "EQUAL_PRINCIPAL"));
        newValues.put("taxSchedule", blankToNull(request.taxSchedule()));
        newValues.put("rampUp", blankToNull(request.rampUp()));

        Map<String, String> oldValues = currentParamValues(parameterSet);
        Map<String, String[]> changed = new LinkedHashMap<>(); // paramKey → [old, new]
        newValues.forEach((k, nv) -> {
            String ov = oldValues.get(k);
            if (!Objects.equals(ov, nv)) {
                changed.put(k, new String[]{ov, nv});
            }
        });

        // 仅对“值真的变了”的字段做锁校验（未变更字段不受他人锁影响）
        if (!changed.isEmpty()) {
            fieldLockService.assertFieldsEditable(scenarioId,
                    changed.keySet().stream().map(k -> "param." + k).toList(), currentUsername());
        }

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
        parameterSet.setDepreciationPolicy(blankToNullOrDefault(request.depreciationPolicy(), "STRAIGHT_LINE"));
        parameterSet.setAmortizationYears(request.amortizationYears() == null ? 0 : request.amortizationYears());
        parameterSet.setAmortizableAmount(request.amortizableAmount() == null ? java.math.BigDecimal.ZERO : request.amortizableAmount());
        parameterSet.setRepaymentMethod(blankToNullOrDefault(request.repaymentMethod(), "EQUAL_PRINCIPAL"));
        parameterSet.setTaxSchedule(blankToNull(request.taxSchedule()));
        parameterSet.setRampUp(blankToNull(request.rampUp()));
        ParameterSet saved = parameterSetRepository.save(parameterSet);

        // 逐字段留痕 FIELD_UPDATED（old → new），驱动协同表“最后编辑”与变更时间线
        String operator = currentUsername();
        changed.forEach((k, pair) -> collabService.recordChange(scenarioId, "FIELD_UPDATED",
                "param." + k, pair[0], pair[1], null, operator));
        return ParameterSetResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ParameterSetResponse getParameters(Long scenarioId) {
        findScenario(scenarioId);
        return ParameterSetResponse.from(parameterSetRepository.findByScenarioId(scenarioId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "参数集不存在")));
    }

    private Scenario findScenario(Long id) {
        return scenarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "测算方案不存在"));
    }

    /** 读取参数集当前值快照（未落库的新对象视为全 null）。 */
    private Map<String, String> currentParamValues(ParameterSet ps) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("wacc", num(ps.getWacc()));
        values.put("taxRate", num(ps.getTaxRate()));
        values.put("depreciationYears", str(ps.getDepreciationYears()));
        values.put("residualRate", num(ps.getResidualRate()));
        values.put("loanRatioLimit", num(ps.getLoanRatioLimit()));
        values.put("pricePerUnit", num(ps.getPricePerUnit()));
        values.put("unitCost", num(ps.getUnitCost()));
        values.put("annualOutput", num(ps.getAnnualOutput()));
        values.put("fixedOperatingCost", num(ps.getFixedOperatingCost()));
        values.put("depreciationPolicy", ps.getDepreciationPolicy());
        values.put("amortizationYears", str(ps.getAmortizationYears()));
        values.put("amortizableAmount", num(ps.getAmortizableAmount()));
        values.put("repaymentMethod", ps.getRepaymentMethod());
        values.put("taxSchedule", ps.getTaxSchedule());
        values.put("rampUp", ps.getRampUp());
        return values;
    }

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "anonymous" : auth.getName();
    }

    private String num(java.math.BigDecimal v) { return v == null ? null : v.stripTrailingZeros().toPlainString(); }
    private String str(Object v) { return v == null ? null : String.valueOf(v); }

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

    private String blankToNullOrDefault(String value, String defaultValue) {
        String v = blankToNull(value);
        return v == null ? defaultValue : v.toUpperCase(java.util.Locale.ROOT);
    }
}

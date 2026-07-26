package com.sis.iids.calculation;

import com.sis.iids.audit.AuditService;
import com.sis.iids.common.error.BusinessException;
import com.sis.iids.common.error.ErrorCode;
import com.sis.iids.engine.financial.CashFlowPeriod;
import com.sis.iids.engine.financial.FinancialEngine;
import com.sis.iids.engine.financial.FinancialInput;
import com.sis.iids.engine.financial.FinancialResult;
import com.sis.iids.scenario.ParameterSet;
import com.sis.iids.scenario.ParameterSetRepository;
import com.sis.iids.scenario.Scenario;
import com.sis.iids.scenario.ScenarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class CalculationService {

    private final ScenarioRepository scenarioRepository;
    private final ParameterSetRepository parameterSetRepository;
    private final InvestmentItemRepository investmentItemRepository;
    private final FinancingPlanRepository financingPlanRepository;
    private final CalculationTaskRepository taskRepository;
    private final CashFlowRowRepository cashFlowRowRepository;
    private final CalculationResultRepository resultRepository;
    private final AuditService auditService;
    private final String defaultFormulaVersion;
    private final String engineVersion;

    public CalculationService(ScenarioRepository scenarioRepository,
                              ParameterSetRepository parameterSetRepository,
                              InvestmentItemRepository investmentItemRepository,
                              FinancingPlanRepository financingPlanRepository,
                              CalculationTaskRepository taskRepository,
                              CashFlowRowRepository cashFlowRowRepository,
                              CalculationResultRepository resultRepository,
                              AuditService auditService,
                              @Value("${iids.formula-version:fin-m1-1.0.0}") String defaultFormulaVersion,
                              @Value("${iids.engine-version:0.1.0}") String engineVersion) {
        this.scenarioRepository = scenarioRepository;
        this.parameterSetRepository = parameterSetRepository;
        this.investmentItemRepository = investmentItemRepository;
        this.financingPlanRepository = financingPlanRepository;
        this.taskRepository = taskRepository;
        this.cashFlowRowRepository = cashFlowRowRepository;
        this.resultRepository = resultRepository;
        this.auditService = auditService;
        this.defaultFormulaVersion = defaultFormulaVersion;
        this.engineVersion = engineVersion;
    }

    @Transactional
    public InvestmentItemResponse createInvestmentItem(Long scenarioId, InvestmentItemRequest request) {
        findScenario(scenarioId);
        InvestmentItem item = new InvestmentItem();
        item.setScenarioId(scenarioId);
        item.setCategory(request.category().trim().toUpperCase(Locale.ROOT));
        item.setName(request.name().trim());
        item.setAmount(request.amount());
        item.setYearNo(request.yearNo());
        return InvestmentItemResponse.from(investmentItemRepository.save(item));
    }

    @Transactional
    public FinancingPlanResponse createFinancingPlan(Long scenarioId, FinancingPlanRequest request) {
        findScenario(scenarioId);
        FinancingPlan plan = new FinancingPlan();
        plan.setScenarioId(scenarioId);
        plan.setSourceType(request.sourceType().trim().toUpperCase(Locale.ROOT));
        plan.setRatio(request.ratio());
        plan.setAmount(request.amount());
        plan.setInterestRate(request.interestRate());
        plan.setTermYears(request.termYears());
        return FinancingPlanResponse.from(financingPlanRepository.save(plan));
    }

    @Transactional
    public CalculationRunResponse runFinancialCalculation(Long scenarioId, CalculationTaskRequest request) {
        Scenario scenario = findScenario(scenarioId);
        ParameterSet parameterSet = parameterSetRepository.findByScenarioId(scenarioId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "Parameter set is required"));

        CalculationTask task = new CalculationTask();
        task.setScenarioId(scenarioId);
        task.setTaskType(request.taskType().trim().toUpperCase(Locale.ROOT));
        task.setStatus(CalculationStatus.RUNNING);
        task.setProgress(10);
        task.setStartedAt(LocalDateTime.now());
        task = taskRepository.save(task);

        try {
            FinancialInput input = buildInput(scenario, parameterSet);
            FinancialResult result = new FinancialEngine().calculate(input);
            persistRows(task, result);
            Map<String, BigDecimal> metrics = persistMetrics(task, parameterSet, result);
            task.setStatus(CalculationStatus.COMPLETED);
            task.setProgress(100);
            task.setFinishedAt(LocalDateTime.now());
            task = taskRepository.save(task);
            auditService.record("CALCULATION_COMPLETED", "CALCULATION_TASK", task.getId().toString(), null, metrics.toString());
            return new CalculationRunResponse(CalculationTaskResponse.from(task), metrics, rowsForTask(task.getId()));
        } catch (RuntimeException ex) {
            task.setStatus(CalculationStatus.FAILED);
            task.setProgress(100);
            task.setErrorMessage(ex.getMessage());
            task.setFinishedAt(LocalDateTime.now());
            taskRepository.save(task);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public CalculationTaskResponse getTask(Long taskId) {
        return CalculationTaskResponse.from(findTask(taskId));
    }

    @Transactional(readOnly = true)
    public CalculationRunResponse getResults(Long taskId) {
        CalculationTask task = findTask(taskId);
        return new CalculationRunResponse(CalculationTaskResponse.from(task), metricsForTask(taskId), rowsForTask(taskId));
    }

    private FinancialInput buildInput(Scenario scenario, ParameterSet parameterSet) {
        FinancialInput input = new FinancialInput();
        input.setConstructionYears(scenario.getConstructionYears());
        input.setHorizonYears(scenario.getHorizonYears());
        input.setWacc(parameterSet.getWacc());
        input.setTaxRate(parameterSet.getTaxRate());
        input.setDepreciationYears(parameterSet.getDepreciationYears());
        input.setResidualRate(parameterSet.getResidualRate());
        input.setPricePerUnit(parameterSet.getPricePerUnit());
        input.setUnitCost(parameterSet.getUnitCost());
        input.setAnnualOutput(parameterSet.getAnnualOutput());
        input.setFixedOperatingCost(parameterSet.getFixedOperatingCost());

        BigDecimal constructionInvestment = BigDecimal.ZERO;
        BigDecimal workingCapital = BigDecimal.ZERO;
        BigDecimal interestDuringConstruction = BigDecimal.ZERO;
        for (InvestmentItem item : investmentItemRepository.findByScenarioId(scenario.getId())) {
            String category = item.getCategory();
            if ("WORKING_CAPITAL".equals(category)) {
                workingCapital = workingCapital.add(item.getAmount());
            } else if ("INTEREST_DURING_CONSTRUCTION".equals(category)) {
                interestDuringConstruction = interestDuringConstruction.add(item.getAmount());
            } else {
                constructionInvestment = constructionInvestment.add(item.getAmount());
            }
        }
        input.setConstructionInvestment(constructionInvestment);
        input.setWorkingCapital(workingCapital);
        input.setInterestDuringConstruction(interestDuringConstruction);

        BigDecimal loanRatio = BigDecimal.ZERO;
        BigDecimal equityRatio = BigDecimal.ONE;
        BigDecimal loanInterestRate = BigDecimal.ZERO;
        int loanTermYears = 0;
        for (FinancingPlan plan : financingPlanRepository.findByScenarioId(scenario.getId())) {
            if ("LOAN".equals(plan.getSourceType())) {
                loanRatio = plan.getRatio();
                loanInterestRate = plan.getInterestRate();
                loanTermYears = plan.getTermYears();
            } else if ("EQUITY".equals(plan.getSourceType())) {
                equityRatio = plan.getRatio();
            }
        }
        input.setLoanRatio(loanRatio);
        input.setEquityRatio(equityRatio);
        input.setLoanInterestRate(loanInterestRate);
        input.setLoanTermYears(loanTermYears);
        input.setConstructionSchedule(defaultSchedule(scenario.getConstructionYears()));
        return input;
    }

    private List<BigDecimal> defaultSchedule(Integer constructionYears) {
        List<BigDecimal> schedule = new ArrayList<>();
        BigDecimal share = BigDecimal.ONE.divide(BigDecimal.valueOf(constructionYears), 8, RoundingMode.HALF_UP);
        for (int i = 0; i < constructionYears; i++) {
            schedule.add(share);
        }
        return schedule;
    }

    private void persistRows(CalculationTask task, FinancialResult result) {
        for (CashFlowPeriod period : result.getRows()) {
            CashFlowRow row = new CashFlowRow();
            row.setScenarioId(task.getScenarioId());
            row.setTaskId(task.getId());
            row.setStatementType("PROJECT_CASH_FLOW");
            row.setPeriodNo(period.getPeriodNo());
            row.setInflow(period.getInflow());
            row.setOutflow(period.getOutflow());
            row.setNetCashFlow(period.getNetCashFlow());
            row.setDiscountedCashFlow(period.getDiscountedCashFlow());
            row.setCumulativeCashFlow(period.getCumulativeCashFlow());
            cashFlowRowRepository.save(row);
        }
    }

    private Map<String, BigDecimal> persistMetrics(CalculationTask task, ParameterSet parameterSet, FinancialResult result) {
        Map<String, BigDecimal> metrics = new LinkedHashMap<>();
        metrics.put("TOTAL_INVESTMENT", result.getTotalInvestment());
        metrics.put("NPV", result.getNpv());
        metrics.put("ROI", result.getRoi());
        metrics.put("STATIC_PAYBACK_YEARS", result.getStaticPaybackYears());
        metrics.put("DYNAMIC_PAYBACK_YEARS", result.getDynamicPaybackYears());
        String formulaVersion = parameterSet.getFormulaVersion() == null ? defaultFormulaVersion : parameterSet.getFormulaVersion();
        for (Map.Entry<String, BigDecimal> entry : metrics.entrySet()) {
            CalculationResultEntity entity = new CalculationResultEntity();
            entity.setScenarioId(task.getScenarioId());
            entity.setTaskId(task.getId());
            entity.setMetricCode(entry.getKey());
            entity.setMetricValue(entry.getValue());
            entity.setFormulaVersion(formulaVersion);
            entity.setEngineVersion(engineVersion);
            entity.setParameterSetId(parameterSet.getId());
            resultRepository.save(entity);
        }
        return metrics;
    }

    private Map<String, BigDecimal> metricsForTask(Long taskId) {
        Map<String, BigDecimal> metrics = new LinkedHashMap<>();
        for (CalculationResultEntity result : resultRepository.findByTaskIdOrderByMetricCodeAsc(taskId)) {
            metrics.put(result.getMetricCode(), result.getMetricValue());
        }
        return metrics;
    }

    private List<CashFlowRowResponse> rowsForTask(Long taskId) {
        return cashFlowRowRepository.findByTaskIdOrderByPeriodNoAsc(taskId).stream()
                .map(CashFlowRowResponse::from)
                .toList();
    }

    private Scenario findScenario(Long scenarioId) {
        return scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Scenario not found"));
    }

    private CalculationTask findTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Calculation task not found"));
    }
}
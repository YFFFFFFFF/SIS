package com.sis.iids.calculation;

import com.sis.iids.audit.AuditService;
import com.sis.iids.collab.CollabService;
import com.sis.iids.collab.FieldLockService;
import com.sis.iids.common.error.BusinessException;
import com.sis.iids.common.error.ErrorCode;
import com.sis.iids.engine.financial.CostEntry;
import com.sis.iids.engine.financial.DepreciationPolicy;
import com.sis.iids.engine.financial.FinancialEngine;
import com.sis.iids.engine.financial.FinancialInput;
import com.sis.iids.engine.financial.FinancialResult;
import com.sis.iids.engine.financial.InvestmentEntry;
import com.sis.iids.engine.financial.LoanTerms;
import com.sis.iids.engine.financial.MetricCodes;
import com.sis.iids.engine.financial.RampUpYear;
import com.sis.iids.engine.financial.RepaymentMethod;
import com.sis.iids.engine.financial.StatementRow;
import com.sis.iids.engine.financial.TaxBracket;
import com.sis.iids.scenario.ParameterSet;
import com.sis.iids.scenario.ParameterSetRepository;
import com.sis.iids.scenario.Scenario;
import com.sis.iids.scenario.ScenarioRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
    private final CostItemRepository costItemRepository;
    private final CalculationResultRepository resultRepository;
    private final AuditService auditService;
    private final FieldLockService fieldLockService;
    private final CollabService collabService;
    private final ObjectMapper objectMapper;
    private final String defaultFormulaVersion;
    private final String engineVersion;

    public CalculationService(ScenarioRepository scenarioRepository,
                              ParameterSetRepository parameterSetRepository,
                              InvestmentItemRepository investmentItemRepository,
                              FinancingPlanRepository financingPlanRepository,
                              CalculationTaskRepository taskRepository,
                              CashFlowRowRepository cashFlowRowRepository,
                              CostItemRepository costItemRepository,
                              CalculationResultRepository resultRepository,
                              AuditService auditService,
                              FieldLockService fieldLockService,
                              CollabService collabService,
                              ObjectMapper objectMapper,
                              @Value("${iids.formula-version:fin-m1-1.0.0}") String defaultFormulaVersion,
                              @Value("${iids.engine-version:0.1.0}") String engineVersion) {
        this.scenarioRepository = scenarioRepository;
        this.parameterSetRepository = parameterSetRepository;
        this.investmentItemRepository = investmentItemRepository;
        this.financingPlanRepository = financingPlanRepository;
        this.taskRepository = taskRepository;
        this.cashFlowRowRepository = cashFlowRowRepository;
        this.costItemRepository = costItemRepository;
        this.resultRepository = resultRepository;
        this.auditService = auditService;
        this.fieldLockService = fieldLockService;
        this.collabService = collabService;
        this.objectMapper = objectMapper;
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
        item.setItemCode(blankToNull(request.itemCode()));
        item.setParentId(request.parentId());
        item.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        InvestmentItem saved = investmentItemRepository.save(item);
        collabService.recordChange(scenarioId, "FIELD_UPDATED", null, null,
                "新增投资项目「%s」金额 %s".formatted(saved.getName(), saved.getAmount()), null, currentUsername());
        return InvestmentItemResponse.from(saved);
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
        plan.setRepaymentMethod(request.repaymentMethod() == null ? "EQUAL_PRINCIPAL"
                : request.repaymentMethod().trim().toUpperCase(Locale.ROOT));
        plan.setGraceYears(request.graceYears() == null ? 0 : request.graceYears());
        FinancingPlan saved = financingPlanRepository.save(plan);
        collabService.recordChange(scenarioId, "FIELD_UPDATED", null, null,
                "新增融资方案「%s」金额 %s".formatted(saved.getSourceType(), saved.getAmount()), null, currentUsername());
        return FinancingPlanResponse.from(saved);
    }

    // ============================================================
    // 投资分项 CRUD（FR-01-01，设计 §8.2）
    // ============================================================

    @Transactional(readOnly = true)
    public List<InvestmentItemResponse> listInvestmentItems(Long scenarioId) {
        findScenario(scenarioId);
        return investmentItemRepository.findByScenarioIdOrderBySortOrderAscIdAsc(scenarioId).stream()
                .map(InvestmentItemResponse::from)
                .toList();
    }

    @Transactional
    public InvestmentItemResponse updateInvestmentItem(Long scenarioId, Long itemId, InvestmentItemRequest request) {
        findScenario(scenarioId);
        InvestmentItem item = investmentItemRepository.findById(itemId)
                .filter(i -> i.getScenarioId().equals(scenarioId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "投资分项不存在"));
        // 先取旧值（set 前），再做锁校验与变更留痕
        BigDecimal oldAmount = item.getAmount();
        Integer oldYearNo = item.getYearNo();
        String operator = currentUsername();
        item.setCategory(request.category().trim().toUpperCase(Locale.ROOT));
        item.setName(request.name().trim());
        item.setAmount(request.amount());
        item.setYearNo(request.yearNo());
        item.setItemCode(blankToNull(request.itemCode()));
        item.setParentId(request.parentId());
        item.setSortOrder(request.sortOrder() == null ? item.getSortOrder() : request.sortOrder());
        // R-15c 字段锁强制拦截：金额/发生年份被他人锁定时禁止更新
        assertItemFieldsEditable(scenarioId, "investment", itemId,
                changedKeys(oldAmount, request.amount(), "amount", oldYearNo, request.yearNo(), "yearNo"), operator);
        InvestmentItem saved = investmentItemRepository.save(item);
        recordItemFieldChanges(scenarioId, "investment", itemId, saved.getName(),
                oldAmount, request.amount(), "amount", oldYearNo, request.yearNo(), "yearNo", operator);
        return InvestmentItemResponse.from(saved);
    }

    @Transactional
    public void deleteInvestmentItem(Long scenarioId, Long itemId) {
        findScenario(scenarioId);
        InvestmentItem item = investmentItemRepository.findById(itemId)
                .filter(i -> i.getScenarioId().equals(scenarioId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "投资分项不存在"));
        deleteInvestmentItemCascade(item);
    }

    private void deleteInvestmentItemCascade(InvestmentItem item) {
        for (InvestmentItem child : investmentItemRepository.findByParentId(item.getId())) {
            deleteInvestmentItemCascade(child);
        }
        investmentItemRepository.delete(item);
    }

    // ============================================================
    // 成本分项 CRUD（FR-01-02，设计 §8.2）
    // ============================================================

    @Transactional(readOnly = true)
    public List<CostItemResponse> listCostItems(Long scenarioId) {
        findScenario(scenarioId);
        return costItemRepository.findByScenarioIdOrderByCategoryAscYearNoAsc(scenarioId).stream()
                .map(CostItemResponse::from)
                .toList();
    }

    @Transactional
    public CostItemResponse createCostItem(Long scenarioId, CostItemRequest request) {
        findScenario(scenarioId);
        CostItem item = new CostItem();
        item.setScenarioId(scenarioId);
        item.setCategory(request.category().trim().toUpperCase(Locale.ROOT));
        item.setName(request.name().trim());
        item.setYearNo(request.yearNo());
        item.setAmount(request.amount());
        CostItem saved = costItemRepository.save(item);
        collabService.recordChange(scenarioId, "FIELD_UPDATED", null, null,
                "新增成本分项「%s」金额 %s".formatted(saved.getName(), saved.getAmount()), null, currentUsername());
        return CostItemResponse.from(saved);
    }

    @Transactional
    public CostItemResponse updateCostItem(Long scenarioId, Long itemId, CostItemRequest request) {
        findScenario(scenarioId);
        CostItem item = costItemRepository.findById(itemId)
                .filter(i -> i.getScenarioId().equals(scenarioId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "成本分项不存在"));
        BigDecimal oldAmount = item.getAmount();
        String operator = currentUsername();
        item.setCategory(request.category().trim().toUpperCase(Locale.ROOT));
        item.setName(request.name().trim());
        item.setYearNo(request.yearNo());
        item.setAmount(request.amount());
        // R-15c 字段锁强制拦截：金额被他人锁定时禁止更新
        assertItemFieldsEditable(scenarioId, "cost", itemId,
                changedKeys(oldAmount, request.amount(), "amount", null, null, null), operator);
        CostItem saved = costItemRepository.save(item);
        recordItemFieldChanges(scenarioId, "cost", itemId, saved.getName(),
                oldAmount, request.amount(), "amount", null, null, null, operator);
        return CostItemResponse.from(saved);
    }

    @Transactional
    public void deleteCostItem(Long scenarioId, Long itemId) {
        findScenario(scenarioId);
        CostItem item = costItemRepository.findById(itemId)
                .filter(i -> i.getScenarioId().equals(scenarioId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "成本分项不存在"));
        costItemRepository.delete(item);
    }

    // ============================================================
    // 投资估算汇总（FR-01-01，分项合计 = 总投资校验）
    // ============================================================

    @Transactional(readOnly = true)
    public InvestmentSummary getInvestmentSummary(Long scenarioId) {
        findScenario(scenarioId);
        List<InvestmentItem> items = investmentItemRepository.findByScenarioIdOrderBySortOrderAscIdAsc(scenarioId);
        BigDecimal construction = BigDecimal.ZERO;
        BigDecimal idc = BigDecimal.ZERO;
        BigDecimal workingCapital = BigDecimal.ZERO;
        for (InvestmentItem item : items) {
            String category = item.getCategory();
            if ("WORKING_CAPITAL".equals(category)) {
                workingCapital = workingCapital.add(item.getAmount());
            } else if ("INTEREST_DURING_CONSTRUCTION".equals(category)) {
                idc = idc.add(item.getAmount());
            } else if (category != null && category.startsWith("CONSTRUCTION")) {
                construction = construction.add(item.getAmount());
            }
        }
        BigDecimal total = construction.add(idc).add(workingCapital);
        List<InvestmentItemResponse> responses = items.stream().map(InvestmentItemResponse::from).toList();
        // 声明总投资口径：当前数据模型无独立总投资字段，balanced 以分项构成是否非空为准
        boolean balanced = total.signum() > 0;
        return new InvestmentSummary(scenarioId, construction, idc, workingCapital, total, null, balanced, responses);
    }

    // ============================================================
    // 三类报表 / 利润流向 / 还本付息（FR-01-03/04，设计 §8.2）
    // ============================================================

    @Transactional(readOnly = true)
    public List<CashFlowRowResponse> getStatements(Long taskId, String statementType) {
        findTask(taskId);
        if (statementType == null || statementType.isBlank()) {
            return rowsForTask(taskId);
        }
        return cashFlowRowRepository.findByTaskIdAndStatementTypeOrderByPeriodNoAsc(taskId,
                        statementType.trim().toUpperCase(Locale.ROOT)).stream()
                .map(CashFlowRowResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProfitFlowResponse> getProfitFlow(Long taskId) {
        findTask(taskId);
        return recompute(taskId).getProfitFlow().stream()
                .map(p -> new ProfitFlowResponse(p.seq(), p.key(), p.label(), p.value()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LoanScheduleResponse> getLoanSchedule(Long taskId) {
        findTask(taskId);
        return recompute(taskId).getLoanSchedule().stream()
                .map(r -> new LoanScheduleResponse(r.getYearNo(), r.getOpeningBalance(), r.getPrincipalPaid(),
                        r.getInterestPaid(), r.getClosingBalance()))
                .toList();
    }

    /** 重算：利润流向/还本付息不落库，按持久化输入确定性重算（引擎为无状态纯函数） */
    private FinancialResult recompute(Long taskId) {
        CalculationTask task = findTask(taskId);
        Scenario scenario = findScenario(task.getScenarioId());
        ParameterSet parameterSet = parameterSetRepository.findByScenarioId(task.getScenarioId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "请先维护测算参数集"));
        return new FinancialEngine().calculate(buildInput(scenario, parameterSet));
    }

    @Transactional
    public CalculationRunResponse createCalculationTask(Long scenarioId, CalculationTaskRequest request) {
        findScenario(scenarioId);
        String requestKey = blankToNull(request.requestKey());
        if (requestKey != null) {
            var existing = taskRepository.findByScenarioIdAndRequestKey(scenarioId, requestKey);
            if (existing.isPresent()) {
                CalculationTask task = existing.get();
                return new CalculationRunResponse(CalculationTaskResponse.from(task), metricsForTask(task.getId()), rowsForTask(task.getId()));
            }
        }

        CalculationTask task = new CalculationTask();
        task.setScenarioId(scenarioId);
        task.setTaskType(request.taskType().trim().toUpperCase(Locale.ROOT));
        task.setStatus(CalculationStatus.PENDING);
        task.setProgress(0);
        task.setRequestKey(requestKey);
        task = taskRepository.save(task);
        return new CalculationRunResponse(CalculationTaskResponse.from(task), Map.of(), List.of());
    }

    @Transactional
    public boolean runNextPendingTask() {
        return taskRepository.findFirstByStatusOrderByCreatedAtAsc(CalculationStatus.PENDING)
                .map(task -> {
                    executeTask(task.getId());
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public CalculationRunResponse executeTask(Long taskId) {
        CalculationTask task = findTask(taskId);
        if (task.getStatus() != CalculationStatus.PENDING) {
            return new CalculationRunResponse(CalculationTaskResponse.from(task), metricsForTask(task.getId()), rowsForTask(task.getId()));
        }

        task.setStatus(CalculationStatus.RUNNING);
        task.setProgress(10);
        task.setErrorMessage(null);
        task.setStartedAt(LocalDateTime.now());
        task = taskRepository.save(task);

        try {
            Scenario scenario = findScenario(task.getScenarioId());
            ParameterSet parameterSet = parameterSetRepository.findByScenarioId(task.getScenarioId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "请先维护测算参数集"));
            FinancialInput input = buildInput(scenario, parameterSet);
            String inputHash = inputHash(input);
            FinancialResult result = new FinancialEngine().calculate(input);
            persistRows(task, result);
            Map<String, BigDecimal> metrics = persistMetrics(task, parameterSet, result, inputHash);
            task.setStatus(CalculationStatus.SUCCESS);
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
            task = taskRepository.save(task);
            auditService.record("CALCULATION_FAILED", "CALCULATION_TASK", task.getId().toString(), null, ex.getMessage());
            return new CalculationRunResponse(CalculationTaskResponse.from(task), Map.of(), List.of());
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

    /**
     * 供敏感性/反算/蒙特卡洛等分析模块复用的基准输入装配（R-04/09/11）。
     * 返回无状态 {@link FinancialInput}，调用方可克隆修改后批量重算。
     */
    public FinancialInput buildBaseInput(Long scenarioId) {
        Scenario scenario = findScenario(scenarioId);
        ParameterSet parameterSet = parameterSetRepository.findByScenarioId(scenarioId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "请先维护测算参数集"));
        return buildInput(scenario, parameterSet);
    }

    private FinancialInput buildInput(Scenario scenario, ParameterSet parameterSet) {
        FinancialInput input = new FinancialInput();
        input.setConstructionYears(scenario.getConstructionYears());
        input.setOperationYears(scenario.getHorizonYears());
        input.setWacc(parameterSet.getWacc());
        input.setWaccSource(parameterSet.getWaccSource());
        input.setTaxRate(parameterSet.getTaxRate());
        input.setDepreciationYears(parameterSet.getDepreciationYears());
        input.setResidualRate(parameterSet.getResidualRate());
        input.setDepreciationPolicy(parseEnum(DepreciationPolicy.class, parameterSet.getDepreciationPolicy(), DepreciationPolicy.STRAIGHT_LINE));
        input.setAmortizationYears(parameterSet.getAmortizationYears() == null ? 0 : parameterSet.getAmortizationYears());
        input.setAmortizableAmount(parameterSet.getAmortizableAmount() == null ? BigDecimal.ZERO : parameterSet.getAmortizableAmount());
        input.setTaxSchedule(parseTaxSchedule(parameterSet.getTaxSchedule()));
        input.setRampUp(parseRampUp(parameterSet.getRampUp()));
        input.setPricePerUnit(parameterSet.getPricePerUnit());
        input.setAnnualOutput(parameterSet.getAnnualOutput());

        // 投资分项：二级子项优先，无子项时取一级 CONSTRUCTION 行
        BigDecimal constructionInvestment = BigDecimal.ZERO;
        BigDecimal workingCapital = BigDecimal.ZERO;
        List<InvestmentEntry> constructionEntries = new ArrayList<>();
        for (InvestmentItem item : investmentItemRepository.findByScenarioId(scenario.getId())) {
            String category = item.getCategory();
            if ("WORKING_CAPITAL".equals(category)) {
                workingCapital = workingCapital.add(item.getAmount());
            } else if ("INTEREST_DURING_CONSTRUCTION".equals(category)) {
                // 利息由引擎根据贷款条款自动资本化，手录建设期利息项不再计入（见设计文档 §5.2）
            } else if ("CONSTRUCTION".equals(category)) {
                constructionInvestment = constructionInvestment.add(item.getAmount());
            } else if (category != null && category.startsWith("CONSTRUCTION_")) {
                constructionEntries.add(new InvestmentEntry(category, item.getName(), item.getAmount()));
            }
        }
        if (!constructionEntries.isEmpty()) {
            input.setConstructionEntries(constructionEntries);
        } else if (constructionInvestment.signum() > 0) {
            input.setConstructionEntries(List.of(new InvestmentEntry("CONSTRUCTION", "建设投资", constructionInvestment)));
        }
        input.setWorkingCapital(workingCapital);

        // 成本分项：cost_item 表；无记录时回退到参数集的 unitCost/fixedOperatingCost（M1 兼容）
        List<CostEntry> costEntries = new ArrayList<>();
        BigDecimal rawMaterialTotal = BigDecimal.ZERO;
        for (CostItem costItem : costItemRepository.findByScenarioId(scenario.getId())) {
            costEntries.add(new CostEntry(costItem.getCategory(), costItem.getName(), costItem.getYearNo(), costItem.getAmount()));
            if ("RAW_MATERIAL".equals(costItem.getCategory()) && costItem.getYearNo() == 0) {
                rawMaterialTotal = rawMaterialTotal.add(costItem.getAmount());
            }
        }
        if (costEntries.isEmpty() && parameterSet.getUnitCost() != null && parameterSet.getAnnualOutput() != null
                && parameterSet.getAnnualOutput().signum() > 0) {
            BigDecimal raw = parameterSet.getUnitCost().multiply(parameterSet.getAnnualOutput());
            costEntries.add(new CostEntry("RAW_MATERIAL", "外购原材料及燃料动力", 0, raw));
            rawMaterialTotal = raw;
            if (parameterSet.getFixedOperatingCost() != null && parameterSet.getFixedOperatingCost().signum() > 0) {
                costEntries.add(new CostEntry("LABOR_MANUFACTURING", "人工及制造费用", 0, parameterSet.getFixedOperatingCost()));
            }
        }
        input.setCostEntries(costEntries);
        input.setUnitVariableCost(safeDivide(rawMaterialTotal, input.getAnnualOutput()));

        // 融资：EQUITY/LOAN
        BigDecimal loanRatio = BigDecimal.ZERO;
        BigDecimal equityRatio = BigDecimal.ONE;
        BigDecimal loanInterestRate = BigDecimal.ZERO;
        int loanTermYears = 0;
        int graceYears = 0;
        RepaymentMethod repaymentMethod = RepaymentMethod.EQUAL_PRINCIPAL;
        for (FinancingPlan plan : financingPlanRepository.findByScenarioId(scenario.getId())) {
            if ("LOAN".equals(plan.getSourceType())) {
                loanRatio = plan.getRatio();
                loanInterestRate = plan.getInterestRate();
                loanTermYears = plan.getTermYears();
                graceYears = plan.getGraceYears() == null ? 0 : plan.getGraceYears();
                repaymentMethod = parseEnum(RepaymentMethod.class, plan.getRepaymentMethod(), RepaymentMethod.EQUAL_PRINCIPAL);
            } else if ("EQUITY".equals(plan.getSourceType())) {
                equityRatio = plan.getRatio();
            }
        }
        // 贷款比例上限校验（设计文档 §4.2 V4）
        if (loanRatio.signum() > 0 && parameterSet.getLoanRatioLimit() != null
                && loanRatio.compareTo(parameterSet.getLoanRatioLimit()) > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "贷款比例 " + loanRatio + " 超过上限 " + parameterSet.getLoanRatioLimit());
        }
        if (loanRatio.signum() > 0) {
            LoanTerms loan = new LoanTerms();
            loan.setPrincipalRatioOfConstruction(loanRatio);
            loan.setInterestRate(loanInterestRate);
            loan.setRepaymentYears(loanTermYears);
            loan.setGraceYears(graceYears);
            loan.setRepaymentMethod(repaymentMethod);
            input.setLoan(loan);
        }
        input.setEquityRatio(equityRatio);
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
        for (Map.Entry<String, List<StatementRow>> entry : result.getStatements().entrySet()) {
            String statementType = entry.getKey();
            for (StatementRow period : entry.getValue()) {
                CashFlowRow row = new CashFlowRow();
                row.setScenarioId(task.getScenarioId());
                row.setTaskId(task.getId());
                row.setStatementType(statementType);
                row.setPeriodNo(period.getPeriodNo());
                row.setInflow(period.getInflow());
                row.setOutflow(period.getOutflow());
                row.setNetCashFlow(period.getNetCashFlow());
                row.setDiscountedCashFlow(period.getDiscountedCashFlow());
                row.setCumulativeCashFlow(period.getCumulativeCashFlow());
                row.setRevenue(period.getRevenue());
                row.setOperatingCost(period.getOperatingCost());
                row.setDepreciation(period.getDepreciation());
                row.setAmortization(period.getAmortization());
                row.setInterest(period.getInterest());
                row.setTax(period.getTax());
                row.setNetProfit(period.getNetProfit());
                cashFlowRowRepository.save(row);
            }
        }
    }

    private Map<String, BigDecimal> persistMetrics(CalculationTask task,
                                                   ParameterSet parameterSet,
                                                   FinancialResult result,
                                                   String inputHash) {
        Map<String, BigDecimal> metrics = new LinkedHashMap<>(result.getMetrics());
        String formulaVersion = parameterSet.getFormulaVersion() == null ? defaultFormulaVersion : parameterSet.getFormulaVersion();
        for (Map.Entry<String, BigDecimal> entry : metrics.entrySet()) {
            if (entry.getValue() == null) {
                continue;   // IRR 等无解指标缺省，不落库（红线：禁止 0 占位）
            }
            CalculationResultEntity entity = new CalculationResultEntity();
            entity.setScenarioId(task.getScenarioId());
            entity.setTaskId(task.getId());
            entity.setMetricCode(entry.getKey());
            entity.setMetricValue(entry.getValue());
            entity.setFormulaVersion(formulaVersion);
            entity.setEngineVersion(engineVersion);
            entity.setParameterSetId(parameterSet.getId());
            entity.setInputHash(inputHash);
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

    private String inputHash(FinancialInput input) {
        String source = String.join("|",
                value(input.getConstructionYears()),
                value(input.getOperationYears()),
                decimal(input.getWacc()),
                decimal(input.getTaxRate()),
                value(input.getDepreciationYears()),
                decimal(input.getResidualRate()),
                value(input.getDepreciationPolicy()),
                value(input.getAmortizationYears()),
                decimal(input.getAmortizableAmount()),
                decimal(input.getPricePerUnit()),
                decimal(input.getAnnualOutput()),
                decimal(input.getWorkingCapital()),
                decimal(input.getUnitVariableCost()),
                decimal(input.getEquityRatio()),
                entriesHash(input.getConstructionEntries()),
                costEntriesHash(input.getCostEntries()),
                taxScheduleHash(input.getTaxSchedule()),
                rampUpHash(input.getRampUp()),
                loanHash(input.getLoan()),
                input.getConstructionSchedule() == null ? "" : input.getConstructionSchedule().stream().map(this::decimal).toList().toString());
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("当前运行环境不支持 SHA-256 摘要算法", ex);
        }
    }

    private String entriesHash(List<InvestmentEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return "";
        }
        return entries.stream()
                .map(e -> e.category() + ":" + decimal(e.amount()))
                .toList().toString();
    }

    private String costEntriesHash(List<CostEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return "";
        }
        return entries.stream()
                .map(e -> e.category() + ":" + e.yearNo() + ":" + decimal(e.amount()))
                .toList().toString();
    }

    private String taxScheduleHash(List<TaxBracket> schedule) {
        if (schedule == null || schedule.isEmpty()) {
            return "";
        }
        return schedule.stream()
                .map(t -> t.fromYear() + "-" + t.toYear() + ":" + decimal(t.rate()))
                .toList().toString();
    }

    private String rampUpHash(List<RampUpYear> rampUp) {
        if (rampUp == null || rampUp.isEmpty()) {
            return "";
        }
        return rampUp.stream()
                .map(r -> r.year() + ":" + decimal(r.loadFactor()))
                .toList().toString();
    }

    private String loanHash(LoanTerms loan) {
        if (loan == null) {
            return "";
        }
        return String.join("/",
                decimal(loan.getPrincipalRatioOfConstruction()),
                decimal(loan.getInterestRate()),
                value(loan.getRepaymentYears()),
                value(loan.getGraceYears()),
                value(loan.getRepaymentMethod()));
    }

    private String decimal(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private String value(Object value) {
        return value == null ? "" : value.toString();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private List<TaxBracket> parseTaxSchedule(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<TaxBracket>>() {});
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "所得税税率表 JSON 解析失败: " + ex.getMessage());
        }
    }

    private List<RampUpYear> parseRampUp(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<RampUpYear>>() {});
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "达产进度 JSON 解析失败: " + ex.getMessage());
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String raw, E fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    private BigDecimal safeDivide(BigDecimal dividend, BigDecimal divisor) {
        if (dividend == null || divisor == null || divisor.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return dividend.divide(divisor, 20, RoundingMode.HALF_UP);
    }

    private Scenario findScenario(Long scenarioId) {
        return scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "测算方案不存在"));
    }

    // ============================================================
    // R-15c 字段锁强制拦截 + 分项变更留痕辅助
    // ============================================================

    /** 收集“值真的变了”的分项字段后缀（amount / yearNo），未变则不纳入锁校验。 */
    private java.util.List<String> changedKeys(Object oldA, Object newA, String keyA,
                                               Object oldB, Object newB, String keyB) {
        java.util.List<String> keys = new java.util.ArrayList<>();
        if (keyA != null && !java.util.Objects.equals(norm(oldA), norm(newA))) {
            keys.add(keyA);
        }
        if (keyB != null && !java.util.Objects.equals(norm(oldB), norm(newB))) {
            keys.add(keyB);
        }
        return keys;
    }

    /** 分项更新锁校验：fieldKey 形如 investment.amount:3 / cost.amount:5。 */
    private void assertItemFieldsEditable(Long scenarioId, String group, Long itemId,
                                          java.util.List<String> fieldNames, String operator) {
        if (fieldNames.isEmpty()) {
            return;
        }
        java.util.List<String> fieldKeys = fieldNames.stream()
                .map(f -> group + "." + f + ":" + itemId).toList();
        fieldLockService.assertFieldsEditable(scenarioId, fieldKeys, operator);
    }

    /** 分项变更逐字段留痕 FIELD_UPDATED（old → new），驱动协同表“最后编辑”。 */
    private void recordItemFieldChanges(Long scenarioId, String group, Long itemId, String itemName,
                                        Object oldA, Object newA, String keyA,
                                        Object oldB, Object newB, String keyB, String operator) {
        if (keyA != null && !java.util.Objects.equals(norm(oldA), norm(newA))) {
            collabService.recordChange(scenarioId, "FIELD_UPDATED", group + "." + keyA + ":" + itemId,
                    norm(oldA), norm(newA), null, operator);
        }
        if (keyB != null && !java.util.Objects.equals(norm(oldB), norm(newB))) {
            collabService.recordChange(scenarioId, "FIELD_UPDATED", group + "." + keyB + ":" + itemId,
                    norm(oldB), norm(newB), null, operator);
        }
    }

    private String norm(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof BigDecimal bd) {
            return bd.stripTrailingZeros().toPlainString();
        }
        return String.valueOf(v);
    }

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "anonymous" : auth.getName();
    }

    private CalculationTask findTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "测算任务不存在"));
    }
}

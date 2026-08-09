package com.sis.iids.collab;

import com.sis.iids.calculation.CostItemRepository;
import com.sis.iids.calculation.FinancingPlanRepository;
import com.sis.iids.calculation.InvestmentItemRepository;
import com.sis.iids.common.error.BusinessException;
import com.sis.iids.common.error.ErrorCode;
import com.sis.iids.scenario.ParameterSet;
import com.sis.iids.scenario.ParameterSetRepository;
import com.sis.iids.scenario.ScenarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * R-15 收尾：协同数据目录（原型 P8"基础数据协同表"的数据源）。
 * 把方案的测算参数 / 投资项目 / 成本分项 / 融资方案聚合成带
 * 责任部门、当前值、锁状态、最后编辑人的协同行。
 */
@Service
public class CollabFieldCatalogService {

    /** 字段分组 → 责任部门（FR-04-02：技术部维护工程量、财务部维护折旧/税率）。 */
    private static final Map<String, String> GROUP_DEPT = Map.of(
            "param", "财务部",
            "investment", "技术部",
            "cost", "财务部",
            "financing", "投资部"
    );

    /** 参数字段 → 中文名。 */
    private static final Map<String, String> PARAM_NAMES = Map.ofEntries(
            Map.entry("wacc", "WACC（折现率）"),
            Map.entry("taxRate", "所得税率"),
            Map.entry("depreciationYears", "折旧年限"),
            Map.entry("residualRate", "残值率"),
            Map.entry("loanRatioLimit", "贷款比例上限"),
            Map.entry("pricePerUnit", "单位售价"),
            Map.entry("unitCost", "单位成本"),
            Map.entry("annualOutput", "年产量"),
            Map.entry("fixedOperatingCost", "固定运营成本"),
            Map.entry("depreciationPolicy", "折旧政策"),
            Map.entry("amortizationYears", "摊销年限"),
            Map.entry("amortizableAmount", "摊销基数"),
            Map.entry("repaymentMethod", "还款方式"),
            Map.entry("taxSchedule", "税率梯度"),
            Map.entry("rampUp", "投产负荷")
    );

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ScenarioRepository scenarioRepository;
    private final ParameterSetRepository parameterSetRepository;
    private final InvestmentItemRepository investmentItemRepository;
    private final CostItemRepository costItemRepository;
    private final FinancingPlanRepository financingPlanRepository;
    private final ScenarioFieldLockRepository fieldLockRepository;
    private final ScenarioChangeRepository changeRepository;

    public CollabFieldCatalogService(ScenarioRepository scenarioRepository,
                                     ParameterSetRepository parameterSetRepository,
                                     InvestmentItemRepository investmentItemRepository,
                                     CostItemRepository costItemRepository,
                                     FinancingPlanRepository financingPlanRepository,
                                     ScenarioFieldLockRepository fieldLockRepository,
                                     ScenarioChangeRepository changeRepository) {
        this.scenarioRepository = scenarioRepository;
        this.parameterSetRepository = parameterSetRepository;
        this.investmentItemRepository = investmentItemRepository;
        this.costItemRepository = costItemRepository;
        this.financingPlanRepository = financingPlanRepository;
        this.fieldLockRepository = fieldLockRepository;
        this.changeRepository = changeRepository;
    }

    @Transactional(readOnly = true)
    public List<CollabFieldItem> catalog(Long scenarioId) {
        if (!scenarioRepository.existsById(scenarioId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "测算方案不存在");
        }
        // 锁索引：fieldKey → 锁
        Map<String, ScenarioFieldLock> lockByField = fieldLockRepository.findByScenarioId(scenarioId).stream()
                .filter(l -> l.getExpireAt().isAfter(java.time.LocalDateTime.now()))
                .collect(Collectors.toMap(ScenarioFieldLock::getFieldKey, Function.identity(), (a, b) -> a));
        // 最后编辑索引：fieldKey → 最近一次 FIELD_UPDATED 变更
        Map<String, ScenarioChange> lastEditByField = changeRepository
                .findByScenarioIdOrderByVersionNoDesc(scenarioId).stream()
                .filter(c -> "FIELD_UPDATED".equals(c.getChangeType()) && c.getFieldName() != null)
                .collect(Collectors.toMap(ScenarioChange::getFieldName, Function.identity(), (a, b) -> a));

        List<CollabFieldItem> rows = new ArrayList<>();

        // ① 测算参数（按数值转字符串）
        parameterSetRepository.findByScenarioId(scenarioId).ifPresent(ps -> {
            addParam(rows, lockByField, lastEditByField, "wacc", num(ps.getWacc()));
            addParam(rows, lockByField, lastEditByField, "taxRate", num(ps.getTaxRate()));
            addParam(rows, lockByField, lastEditByField, "depreciationYears", str(ps.getDepreciationYears()));
            addParam(rows, lockByField, lastEditByField, "residualRate", num(ps.getResidualRate()));
            addParam(rows, lockByField, lastEditByField, "loanRatioLimit", num(ps.getLoanRatioLimit()));
            addParam(rows, lockByField, lastEditByField, "pricePerUnit", num(ps.getPricePerUnit()));
            addParam(rows, lockByField, lastEditByField, "unitCost", num(ps.getUnitCost()));
            addParam(rows, lockByField, lastEditByField, "annualOutput", num(ps.getAnnualOutput()));
            addParam(rows, lockByField, lastEditByField, "fixedOperatingCost", num(ps.getFixedOperatingCost()));
            addParam(rows, lockByField, lastEditByField, "depreciationPolicy", ps.getDepreciationPolicy());
            addParam(rows, lockByField, lastEditByField, "amortizationYears", str(ps.getAmortizationYears()));
            addParam(rows, lockByField, lastEditByField, "amortizableAmount", num(ps.getAmortizableAmount()));
            addParam(rows, lockByField, lastEditByField, "repaymentMethod", ps.getRepaymentMethod());
            addParam(rows, lockByField, lastEditByField, "taxSchedule", ps.getTaxSchedule());
            addParam(rows, lockByField, lastEditByField, "rampUp", ps.getRampUp());
        });

        // ② 投资项目（金额、发生年份可锁）
        investmentItemRepository.findByScenarioIdOrderBySortOrderAscIdAsc(scenarioId).forEach(it -> {
            addEntity(rows, lockByField, lastEditByField, "investment", it.getId(),
                    it.getName() + "（投资）", "amount", num(it.getAmount()));
            addEntity(rows, lockByField, lastEditByField, "investment", it.getId(),
                    it.getName() + "（投资）", "yearNo", str(it.getYearNo()));
        });

        // ③ 成本分项（金额可锁）
        costItemRepository.findByScenarioIdOrderByCategoryAscYearNoAsc(scenarioId).forEach(it -> {
            addEntity(rows, lockByField, lastEditByField, "cost", it.getId(),
                    it.getName() + "（成本）", "amount", num(it.getAmount()));
        });

        // ④ 融资方案（比例、金额、利率可锁）
        financingPlanRepository.findByScenarioId(scenarioId).forEach(it -> {
            String label = ("EQUITY".equals(it.getSourceType()) ? "资本金" : "银行贷款") + "（融资）";
            addEntity(rows, lockByField, lastEditByField, "financing", it.getId(), label, "ratio", num(it.getRatio()));
            addEntity(rows, lockByField, lastEditByField, "financing", it.getId(), label, "amount", num(it.getAmount()));
            addEntity(rows, lockByField, lastEditByField, "financing", it.getId(), label, "interestRate", num(it.getInterestRate()));
        });

        // 排序：分组 → 名称
        rows.sort(Comparator.comparing(CollabFieldItem::group).thenComparing(CollabFieldItem::itemName));
        return rows;
    }

    private void addParam(List<CollabFieldItem> rows, Map<String, ScenarioFieldLock> locks,
                          Map<String, ScenarioChange> edits, String paramKey, String value) {
        String fieldKey = "param." + paramKey;
        rows.add(build(fieldKey, "param", PARAM_NAMES.getOrDefault(paramKey, paramKey), value,
                locks.get(fieldKey), edits.get(fieldKey)));
    }

    private void addEntity(List<CollabFieldItem> rows, Map<String, ScenarioFieldLock> locks,
                           Map<String, ScenarioChange> edits, String group, Long entityId,
                           String itemName, String fieldName, String value) {
        String fieldKey = group + "." + fieldName + ":" + entityId;
        rows.add(build(fieldKey, group, itemName + "·" + fieldCn(fieldName), value,
                locks.get(fieldKey), edits.get(fieldKey)));
    }

    private CollabFieldItem build(String fieldKey, String group, String itemName, String value,
                                  ScenarioFieldLock lock, ScenarioChange edit) {
        return new CollabFieldItem(
                fieldKey, group, itemName, GROUP_DEPT.getOrDefault(group, "投资部"), value,
                lock == null ? null : lock.getHolderName(),
                lock == null ? null : lock.getExpireAt().format(DT),
                edit == null ? null : edit.getOperatorName(),
                edit == null ? null : edit.getCreatedAt().format(DT)
        );
    }

    private String fieldCn(String fieldName) {
        return switch (fieldName) {
            case "amount" -> "金额";
            case "yearNo" -> "发生年份";
            case "ratio" -> "比例";
            case "interestRate" -> "利率";
            default -> fieldName;
        };
    }

    private String num(BigDecimal v) { return v == null ? "—" : v.stripTrailingZeros().toPlainString(); }
    private String str(Object v) { return v == null ? "—" : String.valueOf(v); }
}

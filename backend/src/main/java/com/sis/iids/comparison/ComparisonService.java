package com.sis.iids.comparison;

import com.sis.iids.calculation.CalculationResultEntity;
import com.sis.iids.calculation.CalculationResultRepository;
import com.sis.iids.calculation.CalculationStatus;
import com.sis.iids.calculation.CalculationTask;
import com.sis.iids.calculation.CalculationTaskRepository;
import com.sis.iids.common.error.BusinessException;
import com.sis.iids.common.error.ErrorCode;
import com.sis.iids.engine.financial.MetricCodes;
import com.sis.iids.project.Project;
import com.sis.iids.project.ProjectRepository;
import com.sis.iids.scenario.Scenario;
import com.sis.iids.scenario.ScenarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 多方案横向对比（PRD FR-03-01）：只读聚合，不落库。
 * 取同项目下各方案最新一次 SUCCESS 测算任务的指标，生成对比矩阵与排序建议。
 */
@Service
public class ComparisonService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 指标行定义：编码 / 名称 / 单位 / 方向（HIGHER 越大越好，LOWER 越小越好，NONE 不标记最优） */
    private record RowDef(String code, String name, String unit, String direction) {
    }

    private static final List<RowDef> ROW_DEFS = List.of(
            new RowDef(MetricCodes.TOTAL_INVESTMENT, "总投资额", "万元", "NONE"),
            new RowDef(MetricCodes.NPV, "净现值 NPV", "万元", "HIGHER"),
            new RowDef(MetricCodes.IRR, "内部收益率 IRR", "%", "HIGHER"),
            new RowDef(MetricCodes.STATIC_PAYBACK_YEARS, "静态投资回收期", "年", "LOWER"),
            new RowDef(MetricCodes.DYNAMIC_PAYBACK_YEARS, "动态投资回收期", "年", "LOWER"),
            new RowDef(MetricCodes.ROI, "总投资收益率 ROI", "%", "HIGHER"),
            new RowDef(MetricCodes.CAPITAL_NET_PROFIT_RATE, "资本金净利润率", "%", "HIGHER")
    );

    private final ProjectRepository projectRepository;
    private final ScenarioRepository scenarioRepository;
    private final CalculationTaskRepository taskRepository;
    private final CalculationResultRepository resultRepository;

    public ComparisonService(ProjectRepository projectRepository,
                             ScenarioRepository scenarioRepository,
                             CalculationTaskRepository taskRepository,
                             CalculationResultRepository resultRepository) {
        this.projectRepository = projectRepository;
        this.scenarioRepository = scenarioRepository;
        this.taskRepository = taskRepository;
        this.resultRepository = resultRepository;
    }

    @Transactional(readOnly = true)
    public ComparisonMatrix buildComparison(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "项目不存在"));
        List<Scenario> scenarios = scenarioRepository.findByProjectIdOrderByCreatedAtDesc(projectId);

        List<ComparisonMatrix.ScenarioColumn> columns = new ArrayList<>();
        List<Map<String, BigDecimal>> metricsByScenario = new ArrayList<>();
        for (Scenario scenario : scenarios) {
            Optional<CalculationTask> latest = taskRepository
                    .findFirstByScenarioIdAndStatusOrderByFinishedAtDesc(scenario.getId(), CalculationStatus.SUCCESS);
            if (latest.isPresent()) {
                CalculationTask task = latest.get();
                Map<String, BigDecimal> metrics = new HashMap<>();
                for (CalculationResultEntity r : resultRepository.findByTaskIdOrderByMetricCodeAsc(task.getId())) {
                    metrics.put(r.getMetricCode(), r.getMetricValue());
                }
                metricsByScenario.add(metrics);
                columns.add(new ComparisonMatrix.ScenarioColumn(
                        scenario.getId(), scenario.getName(), task.getId(),
                        task.getFinishedAt() == null ? null : TIME_FMT.format(task.getFinishedAt()),
                        true));
            } else {
                metricsByScenario.add(Map.of());
                columns.add(new ComparisonMatrix.ScenarioColumn(
                        scenario.getId(), scenario.getName(), null, null, false));
            }
        }

        List<ComparisonMatrix.MetricRow> rows = new ArrayList<>();
        for (RowDef def : ROW_DEFS) {
            List<BigDecimal> values = new ArrayList<>();
            for (Map<String, BigDecimal> metrics : metricsByScenario) {
                values.add(metrics.get(def.code()));
            }
            rows.add(new ComparisonMatrix.MetricRow(def.code(), def.name(), def.unit(), def.direction(),
                    values, bestScenarioIds(columns, values, def.direction())));
        }
        // 风险等级占位行（待 R-14 风险评估落地后填充）
        rows.add(new ComparisonMatrix.MetricRow("RISK_LEVEL", "风险等级（待风险评估）", "-", "NONE",
                columns.stream().map(c -> (BigDecimal) null).toList(), List.of()));

        return new ComparisonMatrix(project.getId(), project.getName(), columns, rows,
                buildRanking(columns, metricsByScenario));
    }

    /** 方向感知的最优方案标记（支持并列；NONE 或无有效值时返回空） */
    private List<Long> bestScenarioIds(List<ComparisonMatrix.ScenarioColumn> columns,
                                       List<BigDecimal> values, String direction) {
        if ("NONE".equals(direction)) {
            return List.of();
        }
        List<Long> best = new ArrayList<>();
        BigDecimal bestValue = null;
        for (int i = 0; i < values.size(); i++) {
            BigDecimal v = values.get(i);
            if (v == null) {
                continue;
            }
            if (bestValue == null) {
                bestValue = v;
                best.add(columns.get(i).scenarioId());
                continue;
            }
            int cmp = v.compareTo(bestValue);
            boolean better = "HIGHER".equals(direction) ? cmp > 0 : cmp < 0;
            if (better) {
                bestValue = v;
                best.clear();
                best.add(columns.get(i).scenarioId());
            } else if (cmp == 0) {
                best.add(columns.get(i).scenarioId());
            }
        }
        return best;
    }

    /** 排序建议：按 NPV 降序，NPV 并列时按 IRR 降序；未测算方案不参与 */
    private List<ComparisonMatrix.RankingEntry> buildRanking(List<ComparisonMatrix.ScenarioColumn> columns,
                                                             List<Map<String, BigDecimal>> metricsByScenario) {
        record Candidate(Long scenarioId, String name, BigDecimal npv, BigDecimal irr) {
        }
        List<Candidate> candidates = new ArrayList<>();
        for (int i = 0; i < columns.size(); i++) {
            BigDecimal npv = metricsByScenario.get(i).get(MetricCodes.NPV);
            if (npv == null) {
                continue;
            }
            candidates.add(new Candidate(columns.get(i).scenarioId(), columns.get(i).scenarioName(),
                    npv, metricsByScenario.get(i).get(MetricCodes.IRR)));
        }
        candidates.sort(Comparator.comparing(Candidate::npv).reversed()
                .thenComparing((Candidate c) -> c.irr() == null ? BigDecimal.valueOf(Long.MIN_VALUE) : c.irr(),
                        Comparator.reverseOrder()));
        List<ComparisonMatrix.RankingEntry> ranking = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            Candidate c = candidates.get(i);
            ranking.add(new ComparisonMatrix.RankingEntry(i + 1, c.scenarioId(), c.name(), c.npv(), c.irr()));
        }
        return ranking;
    }
}

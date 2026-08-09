package com.sis.iids.dashboard;

import com.sis.iids.approval.ApprovalInstance;
import com.sis.iids.approval.ApprovalInstanceRepository;
import com.sis.iids.approval.ApprovalStatus;
import com.sis.iids.calculation.CalculationResultEntity;
import com.sis.iids.calculation.CalculationResultRepository;
import com.sis.iids.calculation.CalculationStatus;
import com.sis.iids.calculation.CalculationTask;
import com.sis.iids.calculation.CalculationTaskRepository;
import com.sis.iids.engine.financial.MetricCodes;
import com.sis.iids.project.Project;
import com.sis.iids.project.ProjectRepository;
import com.sis.iids.risk.RiskAlertEvent;
import com.sis.iids.risk.RiskAlertEventRepository;
import com.sis.iids.scenario.Scenario;
import com.sis.iids.scenario.ScenarioRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * R-07 BI 仪表盘聚合（FR-04-01）：只读聚合，一次调用返回全部看板数据（首屏 ≤ 3 秒）。
 * 组合口径：纳入全部"已测算成功"的方案（每方案取最新一次 SUCCESS 任务）。
 * 加权 IRR 按总投资额加权；风险信号灯在 R-12（智能风险预警）落地前为规则化占位。
 */
@Service
public class DashboardService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int TODO_LIMIT = 8;

    private final ProjectRepository projectRepository;
    private final ScenarioRepository scenarioRepository;
    private final CalculationTaskRepository taskRepository;
    private final CalculationResultRepository resultRepository;
    private final ApprovalInstanceRepository approvalRepository;
    private final RiskAlertEventRepository riskAlertRepository;

    public DashboardService(ProjectRepository projectRepository,
                            ScenarioRepository scenarioRepository,
                            CalculationTaskRepository taskRepository,
                            CalculationResultRepository resultRepository,
                            ApprovalInstanceRepository approvalRepository,
                            RiskAlertEventRepository riskAlertRepository) {
        this.projectRepository = projectRepository;
        this.scenarioRepository = scenarioRepository;
        this.taskRepository = taskRepository;
        this.resultRepository = resultRepository;
        this.approvalRepository = approvalRepository;
        this.riskAlertRepository = riskAlertRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummary buildSummary() {
        List<Project> projects = projectRepository.findAllByOrderByCreatedAtDesc();
        List<Scenario> scenarios = scenarioRepository.findAll();

        // 每方案最新 SUCCESS 任务 → 指标
        List<DashboardSummary.BubblePoint> bubbles = new ArrayList<>();
        Map<Long, String> projectNameById = projects.stream()
                .collect(Collectors.toMap(Project::getId, Project::getName));
        BigDecimal totalNpv = BigDecimal.ZERO;
        BigDecimal weightedIrrSum = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (Scenario scenario : scenarios) {
            Optional<CalculationTask> latest = taskRepository
                    .findFirstByScenarioIdAndStatusOrderByFinishedAtDesc(scenario.getId(), CalculationStatus.SUCCESS);
            if (latest.isEmpty()) {
                continue;
            }
            Map<String, BigDecimal> metrics = new HashMap<>();
            for (CalculationResultEntity r : resultRepository.findByTaskIdOrderByMetricCodeAsc(latest.get().getId())) {
                metrics.put(r.getMetricCode(), r.getMetricValue());
            }
            BigDecimal npv = metrics.get(MetricCodes.NPV);
            BigDecimal irr = metrics.get(MetricCodes.IRR);
            BigDecimal investment = metrics.get(MetricCodes.TOTAL_INVESTMENT);
            if (npv == null || irr == null || investment == null) {
                continue;
            }
            bubbles.add(new DashboardSummary.BubblePoint(scenario.getId(), scenario.getName(),
                    projectNameById.getOrDefault(scenario.getProjectId(), "未知项目"), npv, irr, investment));
            totalNpv = totalNpv.add(npv);
            weightedIrrSum = weightedIrrSum.add(irr.multiply(investment));
            totalWeight = totalWeight.add(investment);
        }
        BigDecimal weightedIrr = totalWeight.signum() == 0 ? null
                : weightedIrrSum.divide(totalWeight, 8, RoundingMode.HALF_UP);

        // 阶段分布：项目状态计数
        Map<String, Long> stageMap = projects.stream()
                .collect(Collectors.groupingBy(p -> p.getStatus().name(), Collectors.counting()));
        List<DashboardSummary.NameValue> stageCounts = stageMap.entrySet().stream()
                .map(e -> new DashboardSummary.NameValue(e.getKey(), BigDecimal.valueOf(e.getValue())))
                .toList();

        // 行业分布：按项目类型汇总已测算方案总投资（无测算的项目计 0）
        Map<Long, BigDecimal> investmentByProject = new HashMap<>();
        for (DashboardSummary.BubblePoint b : bubbles) {
            Scenario s = scenarios.stream().filter(x -> x.getId().equals(b.scenarioId())).findFirst().orElse(null);
            if (s != null) {
                investmentByProject.merge(s.getProjectId(), b.investment(), BigDecimal::add);
            }
        }
        Map<String, BigDecimal> industryMap = new HashMap<>();
        for (Project p : projects) {
            String type = p.getProjectType() == null ? "OTHER" : p.getProjectType();
            industryMap.merge(type, investmentByProject.getOrDefault(p.getId(), BigDecimal.ZERO), BigDecimal::add);
        }
        List<DashboardSummary.NameValue> industryAmounts = industryMap.entrySet().stream()
                .map(e -> new DashboardSummary.NameValue(e.getKey(), e.getValue()))
                .toList();

        // 待办：在途审批实例（按更新时间倒序取前 N）
        List<ApprovalInstance> pending = approvalRepository
                .findByStatusInOrderByUpdatedAtDesc(List.of(ApprovalStatus.IN_REVIEW, ApprovalStatus.IN_APPROVAL),
                        PageRequest.of(0, TODO_LIMIT));
        Map<Long, Scenario> scenarioById = scenarios.stream()
                .collect(Collectors.toMap(Scenario::getId, s -> s));
        List<DashboardSummary.TodoItem> todos = pending.stream()
                .map(inst -> {
                    Scenario s = scenarioById.get(inst.getScenarioId());
                    return new DashboardSummary.TodoItem(inst.getId(), inst.getScenarioId(),
                            s == null ? "未知方案" : s.getName(),
                            s == null ? "未知项目" : projectNameById.getOrDefault(s.getProjectId(), "未知项目"),
                            inst.getCurrentNode(), inst.getStatus().name(),
                            inst.getUpdatedAt() == null ? "" : TIME_FMT.format(inst.getUpdatedAt()));
                })
                .toList();

        // 风险信号灯：R-12 落地后取 OPEN 预警事件（按级别聚合）；无 OPEN 事件时回退 IRR 占位规则
        List<RiskAlertEvent> openAlerts = riskAlertRepository.findByStatusOrderByCreatedAtDesc(RiskAlertEvent.STATUS_OPEN);
        List<DashboardSummary.RiskSignal> riskSignals = openAlerts.isEmpty()
                ? buildRiskSignals(bubbles)
                : openAlerts.stream().limit(10)
                        .map(a -> new DashboardSummary.RiskSignal(
                                scenarioNameOf(scenarios, a.getScenarioId()) + " · " + a.getMetricCode(),
                                a.getMetricValue().stripTrailingZeros().toPlainString(),
                                a.getLevel(), a.getMessage()))
                        .toList();

        DashboardSummary.Kpis kpis = new DashboardSummary.Kpis(
                projects.size(), weightedIrr, totalNpv, riskSignals.stream().filter(r -> !"GREEN".equals(r.level())).count());
        return new DashboardSummary(kpis, bubbles, stageCounts, industryAmounts, riskSignals, todos);
    }

    private String scenarioNameOf(List<Scenario> scenarios, Long scenarioId) {
        return scenarios.stream().filter(s -> s.getId().equals(scenarioId)).findFirst()
                .map(Scenario::getName).orElse("方案#" + scenarioId);
    }

    /** 占位风险信号：以已测算方案 IRR 相对 8% 基准的偏离生成，R-12 后替换为真实阈值规则。 */
    private List<DashboardSummary.RiskSignal> buildRiskSignals(List<DashboardSummary.BubblePoint> bubbles) {
        BigDecimal benchmark = new BigDecimal("0.08");
        List<DashboardSummary.RiskSignal> signals = new ArrayList<>();
        for (DashboardSummary.BubblePoint b : bubbles) {
            String level;
            String note;
            if (b.irr().compareTo(benchmark) < 0) {
                level = "RED";
                note = "IRR 低于 8% 基准";
            } else if (b.irr().compareTo(new BigDecimal("0.10")) < 0) {
                level = "YELLOW";
                note = "IRR 接近基准下限";
            } else {
                level = "GREEN";
                note = "正常";
            }
            signals.add(new DashboardSummary.RiskSignal(b.scenarioName(),
                    b.irr().multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP) + "%", level, note));
        }
        if (signals.isEmpty()) {
            signals.add(new DashboardSummary.RiskSignal("暂无可监控方案", "-", "GREEN", "完成一次测算后生成信号"));
        }
        return signals;
    }
}

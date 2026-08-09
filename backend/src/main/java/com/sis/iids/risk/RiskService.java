package com.sis.iids.risk;

import com.sis.iids.audit.AuditService;
import com.sis.iids.calculation.CalculationResultEntity;
import com.sis.iids.calculation.CalculationResultRepository;
import com.sis.iids.calculation.CalculationStatus;
import com.sis.iids.calculation.CalculationTask;
import com.sis.iids.calculation.CalculationTaskRepository;
import com.sis.iids.common.error.BusinessException;
import com.sis.iids.common.error.ErrorCode;
import com.sis.iids.engine.financial.MetricCodes;
import com.sis.iids.scenario.Scenario;
import com.sis.iids.scenario.ScenarioRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * R-12 智能风险预警服务（FR-02-04）。
 * 规则管理（管理员可配置）+ 按最新 SUCCESS 测算指标评估阈值 → 预警事件留痕（触发/恢复/确认）。
 */
@Service
public class RiskService {

    private static final Set<String> SUPPORTED_METRICS = Set.of(
            MetricCodes.NPV, MetricCodes.IRR, MetricCodes.STATIC_PAYBACK_YEARS,
            MetricCodes.DYNAMIC_PAYBACK_YEARS, MetricCodes.ROI, MetricCodes.CAPITAL_NET_PROFIT_RATE,
            MetricCodes.EQUITY_IRR, MetricCodes.EQUITY_NPV, MetricCodes.TOTAL_INVESTMENT);
    private static final Set<String> DIRECTIONS = Set.of("BELOW", "ABOVE");
    private static final Set<String> LEVELS = Set.of("RED", "YELLOW");

    private final RiskRuleRepository ruleRepository;
    private final RiskAlertEventRepository alertRepository;
    private final ScenarioRepository scenarioRepository;
    private final CalculationTaskRepository taskRepository;
    private final CalculationResultRepository resultRepository;
    private final AuditService auditService;

    public RiskService(RiskRuleRepository ruleRepository,
                       RiskAlertEventRepository alertRepository,
                       ScenarioRepository scenarioRepository,
                       CalculationTaskRepository taskRepository,
                       CalculationResultRepository resultRepository,
                       AuditService auditService) {
        this.ruleRepository = ruleRepository;
        this.alertRepository = alertRepository;
        this.scenarioRepository = scenarioRepository;
        this.taskRepository = taskRepository;
        this.resultRepository = resultRepository;
        this.auditService = auditService;
    }

    // ============================================================
    // 规则管理
    // ============================================================
    @Transactional(readOnly = true)
    public List<RiskRuleResponse> listRules() {
        return ruleRepository.findAllByOrderByIdAsc().stream().map(this::toRuleResponse).toList();
    }

    @Transactional
    public RiskRuleResponse createRule(RiskRuleRequest request) {
        validate(request);
        RiskRule rule = new RiskRule();
        apply(rule, request);
        rule.setCreatedBy(currentUsername());
        rule = ruleRepository.save(rule);
        auditService.record("RISK_RULE_CREATED", "RISK_RULE", rule.getId().toString(), null,
                "metric=%s %s %s level=%s".formatted(rule.getMetricCode(), rule.getDirection(),
                        rule.getThresholdValue(), rule.getLevel()));
        return toRuleResponse(rule);
    }

    @Transactional
    public RiskRuleResponse updateRule(Long id, RiskRuleRequest request) {
        validate(request);
        RiskRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "风险规则不存在"));
        String before = "metric=%s %s %s level=%s enabled=%s".formatted(rule.getMetricCode(), rule.getDirection(),
                rule.getThresholdValue(), rule.getLevel(), rule.getEnabled());
        apply(rule, request);
        rule = ruleRepository.save(rule);
        auditService.record("RISK_RULE_UPDATED", "RISK_RULE", rule.getId().toString(), before,
                "metric=%s %s %s level=%s enabled=%s".formatted(rule.getMetricCode(), rule.getDirection(),
                        rule.getThresholdValue(), rule.getLevel(), rule.getEnabled()));
        return toRuleResponse(rule);
    }

    @Transactional
    public void deleteRule(Long id) {
        RiskRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "风险规则不存在"));
        ruleRepository.delete(rule);
        auditService.record("RISK_RULE_DELETED", "RISK_RULE", id.toString(),
                "metric=%s %s %s".formatted(rule.getMetricCode(), rule.getDirection(), rule.getThresholdValue()), null);
    }

    // ============================================================
    // 评估与预警事件
    // ============================================================
    /**
     * 对方案最新 SUCCESS 测算指标评估全部启用规则：
     * 触发 → 新建 OPEN 事件；未触发但存在同规则同方案的 OPEN 事件 → 标记 RECOVERED（恢复留痕）。
     */
    @Transactional
    public RiskEvaluationResult evaluate(Long scenarioId) {
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "测算方案不存在"));
        Optional<CalculationTask> latest = taskRepository
                .findFirstByScenarioIdAndStatusOrderByFinishedAtDesc(scenarioId, CalculationStatus.SUCCESS);
        if (latest.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "方案尚无成功测算结果，无法评估风险规则");
        }
        Map<String, BigDecimal> metrics = new HashMap<>();
        for (CalculationResultEntity r : resultRepository.findByTaskIdOrderByMetricCodeAsc(latest.get().getId())) {
            metrics.put(r.getMetricCode(), r.getMetricValue());
        }

        List<RiskRule> rules = ruleRepository.findByEnabledTrue();
        List<RiskAlertResponse> triggered = new ArrayList<>();
        List<RiskAlertResponse> recovered = new ArrayList<>();
        for (RiskRule rule : rules) {
            BigDecimal value = metrics.get(rule.getMetricCode());
            boolean hit = value != null && isHit(rule, value);
            List<RiskAlertEvent> openEvents = alertRepository
                    .findByRuleIdAndScenarioIdAndStatus(rule.getId(), scenarioId, RiskAlertEvent.STATUS_OPEN);
            if (hit) {
                if (openEvents.isEmpty()) {
                    RiskAlertEvent event = new RiskAlertEvent();
                    event.setRuleId(rule.getId());
                    event.setScenarioId(scenarioId);
                    event.setTaskId(latest.get().getId());
                    event.setMetricCode(rule.getMetricCode());
                    event.setMetricValue(value);
                    event.setThresholdValue(rule.getThresholdValue());
                    event.setLevel(rule.getLevel());
                    event.setMessage(buildMessage(rule, value, scenario.getName()));
                    event = alertRepository.save(event);
                    triggered.add(toAlertResponse(event, scenario.getName()));
                }
            } else {
                for (RiskAlertEvent open : openEvents) {
                    open.setStatus(RiskAlertEvent.STATUS_RECOVERED);
                    alertRepository.save(open);
                    recovered.add(toAlertResponse(open, scenario.getName()));
                }
            }
        }
        if (!triggered.isEmpty() || !recovered.isEmpty()) {
            auditService.record("RISK_EVALUATED", "SCENARIO", scenarioId.toString(), null,
                    "triggered=%d;recovered=%d".formatted(triggered.size(), recovered.size()));
        }
        return new RiskEvaluationResult(scenarioId, rules.size(), triggered, recovered);
    }

    @Transactional(readOnly = true)
    public List<RiskAlertResponse> listAlerts(String status) {
        List<RiskAlertEvent> events = (status == null || status.isBlank())
                ? alertRepository.findAll()
                : alertRepository.findByStatusOrderByCreatedAtDesc(status.trim().toUpperCase(java.util.Locale.ROOT));
        Map<Long, String> scenarioNames = scenarioNames();
        return events.stream()
                .map(e -> toAlertResponse(e, scenarioNames.getOrDefault(e.getScenarioId(), "方案#" + e.getScenarioId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RiskAlertResponse> listScenarioAlerts(Long scenarioId) {
        return alertRepository.findByScenarioIdOrderByCreatedAtDesc(scenarioId).stream()
                .map(e -> toAlertResponse(e, null)).toList();
    }

    @Transactional
    public RiskAlertResponse acknowledge(Long alertId) {
        RiskAlertEvent event = alertRepository.findById(alertId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "预警事件不存在"));
        if (!RiskAlertEvent.STATUS_OPEN.equals(event.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅 OPEN 状态的预警可确认");
        }
        event.setStatus(RiskAlertEvent.STATUS_ACKED);
        event.setAckBy(currentUsername());
        event.setAckAt(java.time.LocalDateTime.now());
        event = alertRepository.save(event);
        auditService.record("RISK_ALERT_ACKED", "RISK_ALERT_EVENT", event.getId().toString(),
                RiskAlertEvent.STATUS_OPEN, RiskAlertEvent.STATUS_ACKED);
        return toAlertResponse(event, null);
    }

    // ============================================================
    // 内部
    // ============================================================
    private boolean isHit(RiskRule rule, BigDecimal value) {
        return switch (rule.getDirection()) {
            case "BELOW" -> value.compareTo(rule.getThresholdValue()) < 0;
            case "ABOVE" -> value.compareTo(rule.getThresholdValue()) > 0;
            default -> false;
        };
    }

    private String buildMessage(RiskRule rule, BigDecimal value, String scenarioName) {
        String directionText = "BELOW".equals(rule.getDirection()) ? "低于" : "高于";
        String base = "方案「%s」指标 %s = %s，%s阈值 %s（%s级预警）".formatted(
                scenarioName, rule.getMetricCode(), value.stripTrailingZeros().toPlainString(),
                directionText, rule.getThresholdValue().stripTrailingZeros().toPlainString(), rule.getLevel());
        return rule.getStrategy() == null || rule.getStrategy().isBlank() ? base : base + "。策略建议：" + rule.getStrategy();
    }

    private void validate(RiskRuleRequest request) {
        String metric = request.metricCode().trim().toUpperCase(java.util.Locale.ROOT);
        if (!SUPPORTED_METRICS.contains(metric)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的监控指标: " + request.metricCode());
        }
        String direction = request.direction().trim().toUpperCase(java.util.Locale.ROOT);
        if (!DIRECTIONS.contains(direction)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "direction 仅支持 BELOW / ABOVE");
        }
        String level = request.level().trim().toUpperCase(java.util.Locale.ROOT);
        if (!LEVELS.contains(level)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "level 仅支持 RED / YELLOW");
        }
    }

    private void apply(RiskRule rule, RiskRuleRequest request) {
        rule.setMetricCode(request.metricCode().trim().toUpperCase(java.util.Locale.ROOT));
        rule.setDirection(request.direction().trim().toUpperCase(java.util.Locale.ROOT));
        rule.setThresholdValue(request.thresholdValue());
        rule.setLevel(request.level().trim().toUpperCase(java.util.Locale.ROOT));
        rule.setStrategy(request.strategy());
        rule.setEnabled(request.enabled() == null || request.enabled());
    }

    private Map<Long, String> scenarioNames() {
        Map<Long, String> names = new HashMap<>();
        for (Scenario s : scenarioRepository.findAll()) {
            names.put(s.getId(), s.getName());
        }
        return names;
    }

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "system" : auth.getName();
    }

    private RiskRuleResponse toRuleResponse(RiskRule rule) {
        return new RiskRuleResponse(rule.getId(), rule.getMetricCode(), rule.getDirection(),
                rule.getThresholdValue(), rule.getLevel(), rule.getStrategy(),
                Boolean.TRUE.equals(rule.getEnabled()), rule.getCreatedBy(), rule.getCreatedAt(), rule.getUpdatedAt());
    }

    private RiskAlertResponse toAlertResponse(RiskAlertEvent event, String scenarioName) {
        return new RiskAlertResponse(event.getId(), event.getRuleId(), event.getScenarioId(), scenarioName,
                event.getTaskId(), event.getMetricCode(), event.getMetricValue(), event.getThresholdValue(),
                event.getLevel(), event.getMessage(), event.getStatus(), event.getAckBy(), event.getAckAt(),
                event.getCreatedAt());
    }
}

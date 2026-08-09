package com.sis.iids.risk;

import com.sis.iids.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * R-12 智能风险预警接口（FR-02-04）。
 */
@RestController
@RequestMapping("/api/v1")
public class RiskController {

    private final RiskService riskService;

    public RiskController(RiskService riskService) {
        this.riskService = riskService;
    }

    // ---- 规则管理（管理员可配置） ----
    @GetMapping("/risk-rules")
    public ApiResponse<List<RiskRuleResponse>> listRules() {
        return ApiResponse.ok(riskService.listRules());
    }

    @PostMapping("/risk-rules")
    @PreAuthorize("hasAnyRole('ADMIN','SYSTEM_ADMINISTRATOR')")
    public ApiResponse<RiskRuleResponse> createRule(@Valid @RequestBody RiskRuleRequest request) {
        return ApiResponse.ok(riskService.createRule(request));
    }

    @PutMapping("/risk-rules/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SYSTEM_ADMINISTRATOR')")
    public ApiResponse<RiskRuleResponse> updateRule(@PathVariable Long id, @Valid @RequestBody RiskRuleRequest request) {
        return ApiResponse.ok(riskService.updateRule(id, request));
    }

    @DeleteMapping("/risk-rules/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SYSTEM_ADMINISTRATOR')")
    public ApiResponse<Void> deleteRule(@PathVariable Long id) {
        riskService.deleteRule(id);
        return ApiResponse.ok(null);
    }

    // ---- 评估与预警事件 ----
    @PostMapping("/scenarios/{scenarioId}/risk-alerts/evaluate")
    @PreAuthorize("hasAnyRole('ANALYST','INVESTMENT_ANALYST','FINANCE_SPECIALIST','PROJECT_MANAGER','ADMIN','SYSTEM_ADMINISTRATOR')")
    public ApiResponse<RiskEvaluationResult> evaluate(@PathVariable Long scenarioId) {
        return ApiResponse.ok(riskService.evaluate(scenarioId));
    }

    @GetMapping("/risk-alerts")
    public ApiResponse<List<RiskAlertResponse>> listAlerts(@RequestParam(required = false) String status) {
        return ApiResponse.ok(riskService.listAlerts(status));
    }

    @GetMapping("/scenarios/{scenarioId}/risk-alerts")
    public ApiResponse<List<RiskAlertResponse>> listScenarioAlerts(@PathVariable Long scenarioId) {
        return ApiResponse.ok(riskService.listScenarioAlerts(scenarioId));
    }

    @PostMapping("/risk-alerts/{id}/ack")
    @PreAuthorize("hasAnyRole('ANALYST','INVESTMENT_ANALYST','FINANCE_SPECIALIST','PROJECT_MANAGER','ADMIN','SYSTEM_ADMINISTRATOR')")
    public ApiResponse<RiskAlertResponse> acknowledge(@PathVariable Long id) {
        return ApiResponse.ok(riskService.acknowledge(id));
    }
}

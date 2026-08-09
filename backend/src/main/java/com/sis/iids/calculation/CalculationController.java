package com.sis.iids.calculation;

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

@RestController
@RequestMapping("/api/v1")
public class CalculationController {

    private final CalculationService calculationService;

    public CalculationController(CalculationService calculationService) {
        this.calculationService = calculationService;
    }

    // ---- 投资分项（FR-01-01） ----

    @GetMapping("/scenarios/{scenarioId}/investment-items")
    public ApiResponse<List<InvestmentItemResponse>> listInvestmentItems(@PathVariable Long scenarioId) {
        return ApiResponse.ok(calculationService.listInvestmentItems(scenarioId));
    }

    @PostMapping("/scenarios/{scenarioId}/investment-items")
    @PreAuthorize("hasAnyRole('ANALYST','INVESTMENT_ANALYST','TECHNICAL_ENGINEER','PROJECT_MANAGER','ADMIN','SYSTEM_ADMINISTRATOR')")
    public ApiResponse<InvestmentItemResponse> createInvestmentItem(@PathVariable Long scenarioId,
                                                                    @Valid @RequestBody InvestmentItemRequest request) {
        return ApiResponse.ok(calculationService.createInvestmentItem(scenarioId, request));
    }

    @PutMapping("/scenarios/{scenarioId}/investment-items/{itemId}")
    @PreAuthorize("hasAnyRole('ANALYST','INVESTMENT_ANALYST','TECHNICAL_ENGINEER','PROJECT_MANAGER','ADMIN','SYSTEM_ADMINISTRATOR')")
    public ApiResponse<InvestmentItemResponse> updateInvestmentItem(@PathVariable Long scenarioId,
                                                                    @PathVariable Long itemId,
                                                                    @Valid @RequestBody InvestmentItemRequest request) {
        return ApiResponse.ok(calculationService.updateInvestmentItem(scenarioId, itemId, request));
    }

    @DeleteMapping("/scenarios/{scenarioId}/investment-items/{itemId}")
    @PreAuthorize("hasAnyRole('ANALYST','INVESTMENT_ANALYST','TECHNICAL_ENGINEER','PROJECT_MANAGER','ADMIN','SYSTEM_ADMINISTRATOR')")
    public ApiResponse<Void> deleteInvestmentItem(@PathVariable Long scenarioId, @PathVariable Long itemId) {
        calculationService.deleteInvestmentItem(scenarioId, itemId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/scenarios/{scenarioId}/investment-summary")
    public ApiResponse<InvestmentSummary> getInvestmentSummary(@PathVariable Long scenarioId) {
        return ApiResponse.ok(calculationService.getInvestmentSummary(scenarioId));
    }

    // ---- 成本分项（FR-01-02） ----

    @GetMapping("/scenarios/{scenarioId}/cost-items")
    public ApiResponse<List<CostItemResponse>> listCostItems(@PathVariable Long scenarioId) {
        return ApiResponse.ok(calculationService.listCostItems(scenarioId));
    }

    @PostMapping("/scenarios/{scenarioId}/cost-items")
    @PreAuthorize("hasAnyRole('ANALYST','INVESTMENT_ANALYST','FINANCE_SPECIALIST','PROJECT_MANAGER','ADMIN','SYSTEM_ADMINISTRATOR')")
    public ApiResponse<CostItemResponse> createCostItem(@PathVariable Long scenarioId,
                                                        @Valid @RequestBody CostItemRequest request) {
        return ApiResponse.ok(calculationService.createCostItem(scenarioId, request));
    }

    @PutMapping("/scenarios/{scenarioId}/cost-items/{itemId}")
    @PreAuthorize("hasAnyRole('ANALYST','INVESTMENT_ANALYST','FINANCE_SPECIALIST','PROJECT_MANAGER','ADMIN','SYSTEM_ADMINISTRATOR')")
    public ApiResponse<CostItemResponse> updateCostItem(@PathVariable Long scenarioId,
                                                        @PathVariable Long itemId,
                                                        @Valid @RequestBody CostItemRequest request) {
        return ApiResponse.ok(calculationService.updateCostItem(scenarioId, itemId, request));
    }

    @DeleteMapping("/scenarios/{scenarioId}/cost-items/{itemId}")
    @PreAuthorize("hasAnyRole('ANALYST','INVESTMENT_ANALYST','FINANCE_SPECIALIST','PROJECT_MANAGER','ADMIN','SYSTEM_ADMINISTRATOR')")
    public ApiResponse<Void> deleteCostItem(@PathVariable Long scenarioId, @PathVariable Long itemId) {
        calculationService.deleteCostItem(scenarioId, itemId);
        return ApiResponse.ok(null);
    }

    // ---- 融资方案 ----

    @PostMapping("/scenarios/{scenarioId}/financing-plans")
    @PreAuthorize("hasAnyRole('ANALYST','INVESTMENT_ANALYST','FINANCE_SPECIALIST','PROJECT_MANAGER','ADMIN','SYSTEM_ADMINISTRATOR')")
    public ApiResponse<FinancingPlanResponse> createFinancingPlan(@PathVariable Long scenarioId,
                                                                  @Valid @RequestBody FinancingPlanRequest request) {
        return ApiResponse.ok(calculationService.createFinancingPlan(scenarioId, request));
    }

    // ---- 测算任务与结果 ----

    @PostMapping("/scenarios/{scenarioId}/calculation-tasks")
    @PreAuthorize("hasAnyRole('ANALYST','INVESTMENT_ANALYST','PROJECT_MANAGER','ADMIN','SYSTEM_ADMINISTRATOR')")
    public ApiResponse<CalculationRunResponse> run(@PathVariable Long scenarioId,
                                                   @Valid @RequestBody CalculationTaskRequest request) {
        return ApiResponse.ok(calculationService.createCalculationTask(scenarioId, request));
    }

    @GetMapping("/calculation-tasks/{taskId}")
    public ApiResponse<CalculationTaskResponse> getTask(@PathVariable Long taskId) {
        return ApiResponse.ok(calculationService.getTask(taskId));
    }

    @GetMapping("/calculation-tasks/{taskId}/results")
    public ApiResponse<CalculationRunResponse> getResults(@PathVariable Long taskId) {
        return ApiResponse.ok(calculationService.getResults(taskId));
    }

    @GetMapping("/calculation-tasks/{taskId}/statements")
    public ApiResponse<List<CashFlowRowResponse>> getStatements(@PathVariable Long taskId,
                                                                @RequestParam(required = false) String type) {
        return ApiResponse.ok(calculationService.getStatements(taskId, type));
    }

    @GetMapping("/calculation-tasks/{taskId}/profit-flow")
    public ApiResponse<List<ProfitFlowResponse>> getProfitFlow(@PathVariable Long taskId) {
        return ApiResponse.ok(calculationService.getProfitFlow(taskId));
    }

    @GetMapping("/calculation-tasks/{taskId}/loan-schedule")
    public ApiResponse<List<LoanScheduleResponse>> getLoanSchedule(@PathVariable Long taskId) {
        return ApiResponse.ok(calculationService.getLoanSchedule(taskId));
    }
}

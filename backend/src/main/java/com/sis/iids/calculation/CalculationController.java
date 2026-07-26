package com.sis.iids.calculation;

import com.sis.iids.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CalculationController {

    private final CalculationService calculationService;

    public CalculationController(CalculationService calculationService) {
        this.calculationService = calculationService;
    }

    @PostMapping("/scenarios/{scenarioId}/investment-items")
    public ApiResponse<InvestmentItemResponse> createInvestmentItem(@PathVariable Long scenarioId,
                                                                    @Valid @RequestBody InvestmentItemRequest request) {
        return ApiResponse.ok(calculationService.createInvestmentItem(scenarioId, request));
    }

    @PostMapping("/scenarios/{scenarioId}/financing-plans")
    public ApiResponse<FinancingPlanResponse> createFinancingPlan(@PathVariable Long scenarioId,
                                                                  @Valid @RequestBody FinancingPlanRequest request) {
        return ApiResponse.ok(calculationService.createFinancingPlan(scenarioId, request));
    }

    @PostMapping("/scenarios/{scenarioId}/calculation-tasks")
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
}

package com.sis.iids.montecarlo;

import com.sis.iids.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * R-11 蒙特卡洛概率分析接口（FR-02-03）。
 */
@RestController
@RequestMapping("/api/v1")
public class MonteCarloController {

    private final MonteCarloService monteCarloService;

    public MonteCarloController(MonteCarloService monteCarloService) {
        this.monteCarloService = monteCarloService;
    }

    @PostMapping("/scenarios/{scenarioId}/monte-carlo-runs")
    @PreAuthorize("hasAnyRole('ANALYST','INVESTMENT_ANALYST','FINANCE_SPECIALIST','PROJECT_MANAGER','ADMIN','SYSTEM_ADMINISTRATOR')")
    public ApiResponse<MonteCarloResponse> run(@PathVariable Long scenarioId,
                                               @Valid @RequestBody MonteCarloRequest request) {
        return ApiResponse.ok(monteCarloService.run(scenarioId, request));
    }

    @GetMapping("/scenarios/{scenarioId}/monte-carlo-runs")
    public ApiResponse<List<MonteCarloResponse>> listRuns(@PathVariable Long scenarioId) {
        return ApiResponse.ok(monteCarloService.listRuns(scenarioId));
    }

    @GetMapping("/monte-carlo-runs/{runId}")
    public ApiResponse<MonteCarloResponse> getRun(@PathVariable Long runId) {
        return ApiResponse.ok(monteCarloService.getRun(runId));
    }
}

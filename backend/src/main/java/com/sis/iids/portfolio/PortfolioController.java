package com.sis.iids.portfolio;

import com.sis.iids.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * R-13 投资组合优化接口（FR-03-02）。
 */
@RestController
@RequestMapping("/api/v1")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @PostMapping("/portfolio-runs")
    @PreAuthorize("hasAnyRole('ANALYST','INVESTMENT_ANALYST','FINANCE_SPECIALIST','PROJECT_MANAGER','ADMIN','SYSTEM_ADMINISTRATOR')")
    public ApiResponse<PortfolioResponse> optimize(@Valid @RequestBody PortfolioRequest request) {
        return ApiResponse.ok(portfolioService.optimize(request));
    }

    @GetMapping("/portfolio-runs/{runId}")
    public ApiResponse<PortfolioResponse> getRun(@PathVariable Long runId) {
        return ApiResponse.ok(portfolioService.getRun(runId));
    }
}

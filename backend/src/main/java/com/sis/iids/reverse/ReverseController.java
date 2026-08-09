package com.sis.iids.reverse;

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

@RestController
@RequestMapping("/api/v1")
public class ReverseController {

    private final ReverseService reverseService;

    public ReverseController(ReverseService reverseService) {
        this.reverseService = reverseService;
    }

    @PostMapping("/scenarios/{scenarioId}/reverse-runs")
    @PreAuthorize("hasAnyRole('ANALYST','INVESTMENT_ANALYST','FINANCE_SPECIALIST','PROJECT_MANAGER','ADMIN','SYSTEM_ADMINISTRATOR')")
    public ApiResponse<ReverseResponse> solve(@PathVariable Long scenarioId,
                                              @Valid @RequestBody ReverseRequest request) {
        return ApiResponse.ok(reverseService.solve(scenarioId, request));
    }

    @GetMapping("/scenarios/{scenarioId}/reverse-runs")
    public ApiResponse<List<ReverseResponse>> listRuns(@PathVariable Long scenarioId) {
        return ApiResponse.ok(reverseService.listRuns(scenarioId));
    }

    @GetMapping("/reverse-runs/{runId}")
    public ApiResponse<ReverseResponse> getRun(@PathVariable Long runId) {
        return ApiResponse.ok(reverseService.getRun(runId));
    }
}

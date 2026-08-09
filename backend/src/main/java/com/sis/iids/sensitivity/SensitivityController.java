package com.sis.iids.sensitivity;

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
public class SensitivityController {

    private final SensitivityService sensitivityService;

    public SensitivityController(SensitivityService sensitivityService) {
        this.sensitivityService = sensitivityService;
    }

    @PostMapping("/scenarios/{scenarioId}/sensitivity")
    @PreAuthorize("hasAnyRole('ANALYST','INVESTMENT_ANALYST','FINANCE_SPECIALIST','PROJECT_MANAGER','ADMIN','SYSTEM_ADMINISTRATOR')")
    public ApiResponse<SensitivityResponse> analyze(@PathVariable Long scenarioId,
                                                    @Valid @RequestBody SensitivityRequest request) {
        return ApiResponse.ok(sensitivityService.analyze(scenarioId, request));
    }

    @GetMapping("/scenarios/{scenarioId}/sensitivity")
    public ApiResponse<List<SensitivityResponse>> listRuns(@PathVariable Long scenarioId) {
        return ApiResponse.ok(sensitivityService.listRuns(scenarioId));
    }

    @GetMapping("/sensitivity-runs/{runId}")
    public ApiResponse<SensitivityResponse> getRun(@PathVariable Long runId) {
        return ApiResponse.ok(sensitivityService.getRun(runId));
    }
}

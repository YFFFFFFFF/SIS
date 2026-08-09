package com.sis.iids.scenario;

import com.sis.iids.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ScenarioController {

    private final ScenarioService scenarioService;

    public ScenarioController(ScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    @PostMapping("/projects/{projectId}/scenarios")
    @PreAuthorize("hasAnyRole('ANALYST','INVESTMENT_ANALYST','PROJECT_MANAGER','ADMIN','SYSTEM_ADMINISTRATOR')")
    public ApiResponse<ScenarioResponse> create(@PathVariable Long projectId,
                                                @Valid @RequestBody ScenarioCreateRequest request) {
        return ApiResponse.ok(scenarioService.create(projectId, request));
    }

    @GetMapping("/projects/{projectId}/scenarios")
    public ApiResponse<List<ScenarioResponse>> listByProject(@PathVariable Long projectId) {
        return ApiResponse.ok(scenarioService.listByProject(projectId));
    }

    @GetMapping("/scenarios/{id}")
    public ApiResponse<ScenarioResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(scenarioService.get(id));
    }

    @PutMapping("/scenarios/{id}")
    @PreAuthorize("hasAnyRole('ANALYST','INVESTMENT_ANALYST','PROJECT_MANAGER','ADMIN','SYSTEM_ADMINISTRATOR')")
    public ApiResponse<ScenarioResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody ScenarioUpdateRequest request) {
        return ApiResponse.ok(scenarioService.update(id, request));
    }

    @PutMapping("/scenarios/{id}/parameters")
    @PreAuthorize("hasAnyRole('ANALYST','INVESTMENT_ANALYST','FINANCE_SPECIALIST','ADMIN','SYSTEM_ADMINISTRATOR')")
    public ApiResponse<ParameterSetResponse> upsertParameters(@PathVariable Long id,
                                                              @Valid @RequestBody ParameterSetRequest request) {
        return ApiResponse.ok(scenarioService.upsertParameters(id, request));
    }

    @GetMapping("/scenarios/{id}/parameters")
    public ApiResponse<ParameterSetResponse> getParameters(@PathVariable Long id) {
        return ApiResponse.ok(scenarioService.getParameters(id));
    }
}
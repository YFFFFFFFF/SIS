package com.sis.iids.ai;

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
 * R-17 AI 决策引擎接口（FR-05，D4 选型 A：同仓 ai 模块）。
 */
@RestController
@RequestMapping("/api/v1")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    // ---- 历史运营数据 ----
    @GetMapping("/projects/{projectId}/ai/operation-records")
    public ApiResponse<List<OperationRecordResponse>> listOperationRecords(@PathVariable Long projectId) {
        return ApiResponse.ok(aiService.listOperationRecords(projectId));
    }

    @PostMapping("/projects/{projectId}/ai/operation-records")
    @PreAuthorize("hasAnyRole('ANALYST','INVESTMENT_ANALYST','FINANCE_SPECIALIST','PROJECT_MANAGER','ADMIN','SYSTEM_ADMINISTRATOR')")
    public ApiResponse<OperationRecordResponse> addOperationRecord(@PathVariable Long projectId,
                                                                   @Valid @RequestBody OperationRecordRequest request) {
        return ApiResponse.ok(aiService.addOperationRecord(projectId, request));
    }

    // ---- 智能参数推荐 / 打分 ----
    @GetMapping("/scenarios/{scenarioId}/ai/param-recommendation")
    public ApiResponse<ParamRecommendationResponse> recommendParams(@PathVariable Long scenarioId) {
        return ApiResponse.ok(aiService.recommendParams(scenarioId));
    }

    @GetMapping("/scenarios/{scenarioId}/ai/score")
    public ApiResponse<ScoreResponse> score(@PathVariable Long scenarioId) {
        return ApiResponse.ok(aiService.scoreScenario(scenarioId));
    }
}

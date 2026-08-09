package com.sis.iids.breakeven;

import com.sis.iids.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * R-10 盈亏平衡分析接口（FR-02-02）。
 */
@RestController
@RequestMapping("/api/v1")
public class BreakEvenController {

    private final BreakEvenService breakEvenService;

    public BreakEvenController(BreakEvenService breakEvenService) {
        this.breakEvenService = breakEvenService;
    }

    @GetMapping("/scenarios/{scenarioId}/break-even")
    public ApiResponse<BreakEvenResponse> analyze(@PathVariable Long scenarioId) {
        return ApiResponse.ok(breakEvenService.analyze(scenarioId));
    }
}

package com.sis.iids.comparison;

import com.sis.iids.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ComparisonController {

    private final ComparisonService comparisonService;

    public ComparisonController(ComparisonService comparisonService) {
        this.comparisonService = comparisonService;
    }

    @GetMapping("/projects/{projectId}/comparison")
    public ApiResponse<ComparisonMatrix> compare(@PathVariable Long projectId) {
        return ApiResponse.ok(comparisonService.buildComparison(projectId));
    }
}

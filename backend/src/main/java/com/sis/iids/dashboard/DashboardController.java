package com.sis.iids.dashboard;

import com.sis.iids.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /** R-07 BI 仪表盘聚合（FR-04-01）：一次调用返回 KPI/气泡/分布/风险信号/待办。 */
    @GetMapping("/dashboard/summary")
    public ApiResponse<DashboardSummary> summary() {
        return ApiResponse.ok(dashboardService.buildSummary());
    }
}

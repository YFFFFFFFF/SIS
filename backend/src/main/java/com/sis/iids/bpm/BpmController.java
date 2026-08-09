package com.sis.iids.bpm;

import com.sis.iids.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * R-14 BPM 可配置审批流接口（FR-04-03）。
 */
@RestController
@RequestMapping("/api/v1")
public class BpmController {

    private final BpmService bpmService;

    public BpmController(BpmService bpmService) {
        this.bpmService = bpmService;
    }

    @GetMapping("/admin/approval-flows")
    public ApiResponse<List<ApprovalFlowResponse>> listFlows() {
        return ApiResponse.ok(bpmService.listFlows());
    }

    @GetMapping("/admin/approval-flows/{id}")
    public ApiResponse<ApprovalFlowResponse> getFlow(@PathVariable Long id) {
        return ApiResponse.ok(bpmService.getFlow(id));
    }

    @PostMapping("/admin/approval-flows")
    @PreAuthorize("hasAnyRole('ADMIN','SYSTEM_ADMINISTRATOR')")
    public ApiResponse<ApprovalFlowResponse> createFlow(@Valid @RequestBody ApprovalFlowRequest request) {
        return ApiResponse.ok(bpmService.createFlow(request));
    }

    @PutMapping("/admin/approval-flows/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SYSTEM_ADMINISTRATOR')")
    public ApiResponse<ApprovalFlowResponse> updateFlow(@PathVariable Long id,
                                                        @Valid @RequestBody ApprovalFlowRequest request) {
        return ApiResponse.ok(bpmService.updateFlow(id, request));
    }

    @DeleteMapping("/admin/approval-flows/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SYSTEM_ADMINISTRATOR')")
    public ApiResponse<Void> deleteFlow(@PathVariable Long id) {
        bpmService.deleteFlow(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/approval-instances/{instanceId}/timeline")
    public ApiResponse<ApprovalTimelineResponse> timeline(@PathVariable Long instanceId) {
        return ApiResponse.ok(bpmService.timeline(instanceId));
    }
}

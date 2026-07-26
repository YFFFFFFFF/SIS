package com.sis.iids.approval;

import com.sis.iids.common.api.ApiResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ApprovalController {

    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @PostMapping("/scenarios/{scenarioId}/approval/submit")
    public ApiResponse<ApprovalInstanceResponse> submit(@PathVariable Long scenarioId,
                                                        @RequestBody(required = false) ApprovalActionRequest request) {
        return ApiResponse.ok(approvalService.submit(scenarioId, request));
    }

    @PostMapping("/approval-instances/{instanceId}/review/approve")
    public ApiResponse<ApprovalInstanceResponse> reviewApprove(@PathVariable Long instanceId,
                                                               @RequestBody(required = false) ApprovalActionRequest request) {
        return ApiResponse.ok(approvalService.reviewApprove(instanceId, request));
    }

    @PostMapping("/approval-instances/{instanceId}/approve")
    public ApiResponse<ApprovalInstanceResponse> approve(@PathVariable Long instanceId,
                                                         @RequestBody(required = false) ApprovalActionRequest request) {
        return ApiResponse.ok(approvalService.approve(instanceId, request));
    }

    @PostMapping("/approval-instances/{instanceId}/reject")
    public ApiResponse<ApprovalInstanceResponse> reject(@PathVariable Long instanceId,
                                                        @RequestBody(required = false) ApprovalActionRequest request) {
        return ApiResponse.ok(approvalService.reject(instanceId, request));
    }
}
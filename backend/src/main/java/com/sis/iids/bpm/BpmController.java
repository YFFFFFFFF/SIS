package com.sis.iids.bpm;

import com.sis.iids.common.api.ApiResponse;
import com.sis.iids.common.error.BusinessException;
import com.sis.iids.common.error.ErrorCode;
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
 * 当前测试阶段采用固定审批流；定义只读保留用于时间线展示。
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
        throw fixedFlowOnly();
    }

    @PutMapping("/admin/approval-flows/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SYSTEM_ADMINISTRATOR')")
    public ApiResponse<ApprovalFlowResponse> updateFlow(@PathVariable Long id,
                                                        @Valid @RequestBody ApprovalFlowRequest request) {
        throw fixedFlowOnly();
    }

    @DeleteMapping("/admin/approval-flows/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SYSTEM_ADMINISTRATOR')")
    public ApiResponse<Void> deleteFlow(@PathVariable Long id) {
        throw fixedFlowOnly();
    }

    @GetMapping("/approval-instances/{instanceId}/timeline")
    public ApiResponse<ApprovalTimelineResponse> timeline(@PathVariable Long instanceId) {
        return ApiResponse.ok(bpmService.timeline(instanceId));
    }

    private BusinessException fixedFlowOnly() {
        return new BusinessException(ErrorCode.BAD_REQUEST,
                "当前测试阶段仅支持固定流程：提交→财务复核→项目经理审批，流程定义暂不可修改");
    }
}

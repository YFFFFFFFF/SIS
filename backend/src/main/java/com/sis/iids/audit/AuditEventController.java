package com.sis.iids.audit;

import com.sis.iids.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-events")
public class AuditEventController {

    private final AuditService auditService;

    public AuditEventController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public ApiResponse<List<AuditEventResponse>> list(@RequestParam String targetType,
                                                      @RequestParam String targetId) {
        return ApiResponse.ok(auditService.list(targetType, targetId));
    }
}
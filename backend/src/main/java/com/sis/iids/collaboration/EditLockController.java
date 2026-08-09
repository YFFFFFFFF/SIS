package com.sis.iids.collaboration;

import com.sis.iids.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class EditLockController {

    private final EditLockService editLockService;

    public EditLockController(EditLockService editLockService) {
        this.editLockService = editLockService;
    }

    @PostMapping("/scenarios/{scenarioId}/lock")
    @PreAuthorize("hasAnyRole('ANALYST','INVESTMENT_ANALYST','FINANCE_SPECIALIST','TECHNICAL_ENGINEER','PROJECT_MANAGER','ADMIN','SYSTEM_ADMINISTRATOR')")
    public ApiResponse<EditLockResponse> acquire(@PathVariable Long scenarioId,
                                                 @Valid @RequestBody AcquireLockRequest request) {
        return ApiResponse.ok(editLockService.acquire(scenarioId, request));
    }

    @DeleteMapping("/scenarios/{scenarioId}/lock")
    @PreAuthorize("hasAnyRole('ANALYST','INVESTMENT_ANALYST','FINANCE_SPECIALIST','TECHNICAL_ENGINEER','PROJECT_MANAGER','ADMIN','SYSTEM_ADMINISTRATOR')")
    public ApiResponse<ReleaseLockResponse> release(@PathVariable Long scenarioId,
                                                    @Valid @RequestBody ReleaseLockRequest request) {
        return ApiResponse.ok(editLockService.release(scenarioId, request));
    }
}
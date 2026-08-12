package com.sis.iids.collab;

import com.sis.iids.common.api.ApiResponse;
import com.sis.iids.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * R-15 协同编辑接口（FR-04-02）。
 */
@RestController
@RequestMapping("/api/v1")
public class CollabController {

    private final CollabService collabService;
    private final FieldLockService fieldLockService;
    private final CollabFieldCatalogService fieldCatalogService;
    private final SseTicketService sseTicketService;

    public CollabController(CollabService collabService,
                            FieldLockService fieldLockService,
                            CollabFieldCatalogService fieldCatalogService,
                            SseTicketService sseTicketService) {
        this.collabService = collabService;
        this.fieldLockService = fieldLockService;
        this.fieldCatalogService = fieldCatalogService;
        this.sseTicketService = sseTicketService;
    }

    @PostMapping("/scenarios/{scenarioId}/collab/tickets")
    public ApiResponse<SseTicketResponse> issueTicket(@PathVariable Long scenarioId) {
        collabService.ensureScenarioExists(scenarioId);
        return ApiResponse.ok(sseTicketService.issue(scenarioId,
                SecurityContextHolder.getContext().getAuthentication().getName()));
    }

    /** SSE 订阅使用一次性短期凭证，避免长期 JWT 出现在 URL。 */
    @GetMapping("/scenarios/{scenarioId}/collab/stream")
    public SseEmitter stream(@PathVariable Long scenarioId, @RequestParam String ticket) {
        sseTicketService.consume(ticket, scenarioId);
        return collabService.subscribe(scenarioId);
    }

    @GetMapping("/scenarios/{scenarioId}/comments")
    public ApiResponse<List<CommentResponse>> listComments(@PathVariable Long scenarioId) {
        return ApiResponse.ok(collabService.listComments(scenarioId));
    }

    @PostMapping("/scenarios/{scenarioId}/comments")
    @PreAuthorize("hasAnyRole('ANALYST','INVESTMENT_ANALYST','FINANCE_SPECIALIST','TECHNICAL_ENGINEER','PROJECT_MANAGER','ADMIN','SYSTEM_ADMINISTRATOR')")
    public ApiResponse<CommentResponse> addComment(@PathVariable Long scenarioId,
                                                   @Valid @RequestBody CommentRequest request) {
        return ApiResponse.ok(collabService.addComment(scenarioId, request, currentUserId(), currentUsername()));
    }

    @DeleteMapping("/scenarios/{scenarioId}/comments/{commentId}")
    @PreAuthorize("hasAnyRole('ANALYST','INVESTMENT_ANALYST','FINANCE_SPECIALIST','TECHNICAL_ENGINEER','PROJECT_MANAGER','ADMIN','SYSTEM_ADMINISTRATOR')")
    public ApiResponse<Map<String, Object>> deleteComment(@PathVariable Long scenarioId,
                                                           @PathVariable Long commentId) {
        collabService.deleteComment(scenarioId, commentId, currentUsername(), canManageAllComments());
        return ApiResponse.ok(Map.of("deleted", true, "commentId", commentId));
    }

    @GetMapping("/scenarios/{scenarioId}/changes")
    public ApiResponse<List<ChangeResponse>> listChanges(@PathVariable Long scenarioId) {
        return ApiResponse.ok(collabService.listChanges(scenarioId));
    }

    @PostMapping("/scenarios/{scenarioId}/presence")
    public ApiResponse<List<PresenceResponse>> heartbeat(@PathVariable Long scenarioId,
                                                         @Valid @RequestBody PresenceRequest request) {
        return ApiResponse.ok(collabService.heartbeat(scenarioId, request));
    }

    @GetMapping("/scenarios/{scenarioId}/presence")
    public ApiResponse<List<PresenceResponse>> listPresence(@PathVariable Long scenarioId) {
        return ApiResponse.ok(collabService.listPresence(scenarioId));
    }

    @DeleteMapping("/scenarios/{scenarioId}/presence/{userId}")
    public ApiResponse<List<PresenceResponse>> leave(@PathVariable Long scenarioId, @PathVariable Long userId) {
        return ApiResponse.ok(collabService.leave(scenarioId, userId));
    }

    // ============================================================
    // R-15 收尾：字段级锁定与协同数据目录（FR-04-02）
    // ============================================================

    /** 协同数据表：参数/投资/成本/融资字段 + 责任部门 + 当前值 + 锁状态 + 最后编辑。 */
    @GetMapping("/scenarios/{scenarioId}/collab/fields")
    public ApiResponse<List<CollabFieldItem>> fieldCatalog(@PathVariable Long scenarioId) {
        return ApiResponse.ok(fieldCatalogService.catalog(scenarioId));
    }

    @GetMapping("/scenarios/{scenarioId}/field-locks")
    public ApiResponse<List<FieldLockResponse>> listFieldLocks(@PathVariable Long scenarioId) {
        return ApiResponse.ok(fieldLockService.list(scenarioId));
    }

    /** 获取/续期字段锁：他人持有未过期 → 409 冲突提示。 */
    @PostMapping("/scenarios/{scenarioId}/field-locks")
    @PreAuthorize("hasAnyRole('ANALYST','INVESTMENT_ANALYST','FINANCE_SPECIALIST','TECHNICAL_ENGINEER','PROJECT_MANAGER','ADMIN','SYSTEM_ADMINISTRATOR')")
    public ApiResponse<FieldLockResponse> acquireFieldLock(@PathVariable Long scenarioId,
                                                           @Valid @RequestBody FieldLockAcquireRequest request) {
        return ApiResponse.ok(fieldLockService.acquire(scenarioId, request));
    }

    /** 释放字段锁：仅持有人本人。 */
    @PostMapping("/scenarios/{scenarioId}/field-locks/release")
    @PreAuthorize("hasAnyRole('ANALYST','INVESTMENT_ANALYST','FINANCE_SPECIALIST','TECHNICAL_ENGINEER','PROJECT_MANAGER','ADMIN','SYSTEM_ADMINISTRATOR')")
    public ApiResponse<java.util.Map<String, Object>> releaseFieldLock(@PathVariable Long scenarioId,
                                                                       @Valid @RequestBody FieldLockReleaseRequest request) {
        fieldLockService.release(scenarioId, request);
        return ApiResponse.ok(java.util.Map.of("released", true, "fieldKey", request.fieldKey()));
    }

    /** 管理员强制释放（冲突合并人工兜底）；fieldKey 含 . / : 故走查询参数。 */
    @PostMapping("/scenarios/{scenarioId}/field-locks/force-release")
    @PreAuthorize("hasAnyRole('ADMIN','SYSTEM_ADMINISTRATOR')")
    public ApiResponse<java.util.Map<String, Object>> forceReleaseFieldLock(@PathVariable Long scenarioId,
                                                                            @RequestParam String fieldKey) {
        fieldLockService.forceRelease(scenarioId, fieldKey, currentUsername());
        return ApiResponse.ok(java.util.Map.of("released", true, "fieldKey", fieldKey));
    }

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "anonymous" : auth.getName();
    }

    private Long currentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal instanceof CurrentUser currentUser ? currentUser.getUserId() : null;
    }

    private boolean canManageAllComments() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().equals("ROLE_SYSTEM_ADMINISTRATOR"));
    }
}

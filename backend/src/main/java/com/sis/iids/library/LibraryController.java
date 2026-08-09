package com.sis.iids.library;

import com.sis.iids.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * R-16 项目库与知识沉淀接口（FR-03-03）。
 */
@RestController
@RequestMapping("/api/v1")
public class LibraryController {

    private final LibraryService libraryService;

    public LibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @GetMapping("/project-library")
    public ApiResponse<List<ProjectLibraryItem>> search(@RequestParam(required = false) String status,
                                                        @RequestParam(required = false) String projectType,
                                                        @RequestParam(required = false) String tag,
                                                        @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(libraryService.search(status, projectType, tag, keyword));
    }

    @GetMapping("/projects/{projectId}/tags")
    public ApiResponse<List<String>> listTags(@PathVariable Long projectId) {
        return ApiResponse.ok(libraryService.listTags(projectId));
    }

    @PutMapping("/projects/{projectId}/tags")
    @PreAuthorize("hasAnyRole('ANALYST','INVESTMENT_ANALYST','PROJECT_MANAGER','ADMIN','SYSTEM_ADMINISTRATOR')")
    public ApiResponse<List<String>> setTags(@PathVariable Long projectId, @RequestBody List<String> tags) {
        return ApiResponse.ok(libraryService.setTags(projectId, tags));
    }

    @PostMapping("/projects/{projectId}/review")
    @PreAuthorize("hasAnyRole('ANALYST','INVESTMENT_ANALYST','PROJECT_MANAGER','ADMIN','SYSTEM_ADMINISTRATOR')")
    public ApiResponse<ProjectReviewResponse> saveReview(@PathVariable Long projectId,
                                                         @Valid @RequestBody ProjectReviewRequest request) {
        return ApiResponse.ok(libraryService.saveReview(projectId, request));
    }

    @GetMapping("/projects/{projectId}/review")
    public ApiResponse<ProjectReviewResponse> getReview(@PathVariable Long projectId) {
        return ApiResponse.ok(libraryService.getReview(projectId));
    }
}

package com.sis.iids.importx;

import com.sis.iids.common.api.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class ExcelImportController {

    private final ExcelImportService excelImportService;

    public ExcelImportController(ExcelImportService excelImportService) {
        this.excelImportService = excelImportService;
    }

    @PostMapping("/scenarios/{scenarioId}/import/excel")
    @PreAuthorize("hasAnyRole('ANALYST','INVESTMENT_ANALYST','FINANCE_SPECIALIST','TECHNICAL_ENGINEER','ADMIN','SYSTEM_ADMINISTRATOR')")
    public ApiResponse<ImportJobResponse> importExcel(@PathVariable Long scenarioId,
                                                       @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(excelImportService.importExcel(scenarioId, file));
    }

    @GetMapping("/import-jobs/{jobId}")
    public ApiResponse<ImportJobResponse> getJob(@PathVariable Long jobId) {
        return ApiResponse.ok(excelImportService.getJob(jobId));
    }
}
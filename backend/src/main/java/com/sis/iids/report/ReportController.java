package com.sis.iids.report;

import com.sis.iids.common.api.ApiResponse;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ReportController {

    private static final MediaType EXCEL_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/calculation-tasks/{taskId}/reports")
    @PreAuthorize("hasAnyRole('ANALYST','INVESTMENT_ANALYST','PROJECT_MANAGER','ADMIN','SYSTEM_ADMINISTRATOR')")
    public ApiResponse<ReportDocumentResponse> generate(@PathVariable Long taskId,
                                                        @RequestParam(required = false) String format) {
        return ApiResponse.ok(reportService.generate(taskId, format));
    }

    @GetMapping("/reports/{reportId}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long reportId) {
        ReportDownload download = reportService.download(reportId);
        MediaType mediaType = "PDF".equalsIgnoreCase(download.fileType())
                ? MediaType.APPLICATION_PDF : EXCEL_MEDIA_TYPE;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(download.fileName()).build().toString())
                .body(download.content());
    }
}